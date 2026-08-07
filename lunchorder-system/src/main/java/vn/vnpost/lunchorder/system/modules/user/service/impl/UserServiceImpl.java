package vn.vnpost.lunchorder.system.modules.user.service.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.vnpost.lunchorder.common.audit.AuditEvent;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.system.modules.department.entity.Department;
import vn.vnpost.lunchorder.system.modules.user.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.system.modules.department.repository.DepartmentRepository;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.ChangePasswordRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.ProfileUpdateRequest;
import vn.vnpost.lunchorder.system.modules.user.repository.UserRepository;
import vn.vnpost.lunchorder.system.modules.user.service.UserService;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserCreateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserImportErrorResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserImportResultResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserUpdateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.helper.UserImportExcelHelper;
import vn.vnpost.lunchorder.system.modules.user.service.mapstruct.UserMapper;
import vn.vnpost.lunchorder.system.modules.role.repository.RoleRepository;
import vn.vnpost.lunchorder.system.modules.role.entity.Role;
import vn.vnpost.lunchorder.tools.excel.ExcelExportService;
import vn.vnpost.lunchorder.tools.excel.ExcelImportService;
import vn.vnpost.lunchorder.tools.excel.ExcelRow;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private static final int MAX_IMPORT_ROWS = 500;
    private static final String DEFAULT_ROLE_CODE = "USER";
    private static final String XLSX_EXTENSION = ".xlsx";

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "fullName", "fullName",
            "department", "department.name");

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;
    private final UserImportExcelHelper userImportExcelHelper;
    private final Validator validator;

    private Set<String> roleCodesOf(User user) {
        if (user.getRoles() == null) {
            return new TreeSet<>();
        }
        return user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Department resolveDepartment(String departmentValue) {
        if (departmentValue == null) {
            return null;
        }
        Long id = parseIdOrNull(departmentValue);
        if (id != null) {
            Optional<Department> deptOpt = departmentRepository.findById(id);
            if (deptOpt.isPresent()) {
                return deptOpt.get();
            }
        }

        return departmentRepository.findByCode(departmentValue)
                .or(() -> departmentRepository.findByName(departmentValue))
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
    }

    private Long parseIdOrNull(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_USERNAME_EXISTS);
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getDepartment() != null) {
            user.setDepartment(resolveDepartment(request.getDepartment()));
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            List<Role> roles = roleRepository.findByCodeIn(request.getRoles());
            user.setRoles(new HashSet<>(roles));
        } else {
            Role userRole = roleRepository.findByCode("USER")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            user.setRoles(new HashSet<>(Set.of(userRole)));
        }

        User savedUser = userRepository.save(user);
        UserResponse created = userMapper.toDto(savedUser);
        eventPublisher.publishEvent(new AuditEvent("CREATE_USER", "User", savedUser.getId(), null, created));
        return created;
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        UserResponse before = userMapper.toDto(user);

        userMapper.update(request, user);
        if (request.getDepartment() != null) {
            user.setDepartment(resolveDepartment(request.getDepartment()));
        }
        
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null) {
            if (request.getRoles().isEmpty()) {
                user.setRoles(new HashSet<>());
            } else {
                List<Role> roles = roleRepository.findByCodeIn(request.getRoles());
                user.setRoles(new HashSet<>(roles));
            }
        }

        User savedUser = userRepository.save(user);
        UserResponse after = userMapper.toDto(savedUser);
        eventPublisher.publishEvent(new AuditEvent("UPDATE_USER", "User", id, before, after));
        return after;
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setFullName(request.getFullName());

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        UserResponse before = userMapper.toDto(user);

        user.setIsActive(false);
        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(new AuditEvent("DELETE_USER", "User", id, before, userMapper.toDto(savedUser)));
    }

    @Override
    @Cacheable(value = "users", key = "'username:' + #username")
    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toDto(user);
    }

    @Override
    @Cacheable(value = "users", key = "'list:' + #page + '-' + #size + '-' + #keyword + '-' + #departmentIds + '-' + #isActives + '-' + #sortBy + '-' + #sortDir")
    public PageResponse<UserResponse> findAll(int page, int size, String keyword, List<Long> departmentIds,
            List<Boolean> isActives, String sortBy, String sortDir) {
        Sort sort = PaginationConstants.toSort(sortBy, sortDir, SORTABLE_FIELDS, DEFAULT_SORT);
        Pageable pageable = PaginationConstants.toPageable(page, size, sort);

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("fullName")), likeKeyword),
                    cb.like(cb.lower(root.get("username")), likeKeyword)
                ));
            }
            if (departmentIds != null && !departmentIds.isEmpty()) {
                predicates.add(root.get("department").get("id").in(departmentIds));
            }
            if (isActives != null && !isActives.isEmpty()) {
                predicates.add(root.get("isActive").in(isActives));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);

        return PageResponse.of(userPage, userMapper.toDtoList(userPage.getContent()), page, size);
    }

    @Override
    @Cacheable(value = "users", key = "'all'")
    public List<UserResponse> getAll() {
        Pageable pageable = PageRequest.of(0, PaginationConstants.MAX_LOOKUP_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        List<User> users = userRepository.findAll(pageable).getContent();
        return userMapper.toDtoList(users);
    }

    @Override
    public byte[] exportExcel(String keyword) {
        try (ByteArrayInputStream in = excelExportService.exportToExcel(export(keyword), "Danh sách người dùng")) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.EXPORT_FAILED);
        }
    }

    private List<UserResponse> export(String keyword) {
        List<User> users;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userRepository.findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword.trim(), keyword.trim());
        } else {
            users = userRepository.findAll(sort);
        }
        return userMapper.toDtoList(users);
    }

    @Override
    public byte[] buildImportTemplate() {
        return userImportExcelHelper.buildTemplate();
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserImportResultResponse importExcel(MultipartFile file) {
        List<ExcelRow> rows = readImportRows(file);

        Map<String, Department> departmentIndex = buildDepartmentIndex();
        Map<String, Role> roleIndex = roleRepository.findAll().stream()
                .collect(Collectors.toMap(role -> normalizeCode(role.getCode()), Function.identity(), (a, b) -> a));
        Set<String> takenUsernames = new HashSet<>(userRepository.findExistingUsernames(rows.stream()
                .map(row -> row.cell(UserImportExcelHelper.COLUMN_USERNAME))
                .collect(Collectors.toSet())));

        List<UserImportErrorResponse> errors = new ArrayList<>();
        List<User> newUsers = new ArrayList<>();

        for (ExcelRow row : rows) {
            List<String> messages = new ArrayList<>();
            User user = buildImportedUser(row, departmentIndex, roleIndex, takenUsernames, messages);

            if (messages.isEmpty()) {
                newUsers.add(user);
                takenUsernames.add(user.getUsername());
            } else {
                errors.add(new UserImportErrorResponse(row.rowNumber(),
                        row.cell(UserImportExcelHelper.COLUMN_USERNAME), String.join(" ", messages)));
            }
        }

        if (!newUsers.isEmpty()) {
            userRepository.saveAll(newUsers);
        }

        UserImportResultResponse result =
                new UserImportResultResponse(rows.size(), newUsers.size(), errors.size(), errors);
        eventPublisher.publishEvent(new AuditEvent("IMPORT_USERS", "User", null, null, result));
        return result;
    }

    private List<ExcelRow> readImportRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.IMPORT_FILE_REQUIRED);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(XLSX_EXTENSION)) {
            throw new AppException(ErrorCode.IMPORT_FILE_TYPE_NOT_ALLOWED);
        }

        List<ExcelRow> rows;
        try (InputStream in = file.getInputStream()) {
            rows = excelImportService.readRows(in, UserImportExcelHelper.COLUMN_COUNT,
                    UserImportExcelHelper.HEADER_ROW_COUNT);
        } catch (IOException | RuntimeException e) {
            log.error("Đọc file Excel import người dùng thất bại: {}", filename, e);
            throw new AppException(ErrorCode.IMPORT_FILE_UNREADABLE);
        }

        if (rows.isEmpty()) {
            throw new AppException(ErrorCode.IMPORT_FILE_EMPTY);
        }
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new AppException(ErrorCode.IMPORT_FILE_TOO_MANY_ROWS);
        }
        return rows;
    }

    private User buildImportedUser(ExcelRow row, Map<String, Department> departmentIndex, Map<String, Role> roleIndex,
            Set<String> takenUsernames, List<String> messages) {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(row.cell(UserImportExcelHelper.COLUMN_USERNAME));
        request.setPassword(row.cell(UserImportExcelHelper.COLUMN_PASSWORD));
        request.setFullName(row.cell(UserImportExcelHelper.COLUMN_FULL_NAME));
        request.setDepartment(row.cell(UserImportExcelHelper.COLUMN_DEPARTMENT));

        validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .forEach(messages::add);

        if (takenUsernames.contains(request.getUsername())) {
            messages.add("Tài khoản đã tồn tại trong hệ thống hoặc bị trùng trong file.");
        }

        Department department = departmentIndex.get(normalizeName(request.getDepartment()));
        if (department == null && !request.getDepartment().isEmpty()) {
            messages.add("Không tìm thấy phòng ban \"" + request.getDepartment() + "\".");
        }

        Set<Role> roles = resolveImportRoles(row.cell(UserImportExcelHelper.COLUMN_ROLES), roleIndex, messages);

        if (!messages.isEmpty()) {
            return null;
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDepartment(department);
        user.setRoles(roles);
        user.setIsActive(true);
        return user;
    }

    private Set<Role> resolveImportRoles(String rawRoles, Map<String, Role> roleIndex, List<String> messages) {
        Set<String> codes = new LinkedHashSet<>();
        for (String part : rawRoles.split(",")) {
            String code = normalizeCode(part);
            if (!code.isEmpty()) {
                codes.add(code);
            }
        }
        if (codes.isEmpty()) {
            codes.add(DEFAULT_ROLE_CODE);
        }

        Set<Role> roles = new HashSet<>();
        for (String code : codes) {
            Role role = roleIndex.get(code);
            if (role == null) {
                messages.add("Không tìm thấy vai trò \"" + code + "\".");
            } else {
                roles.add(role);
            }
        }
        return roles;
    }

    private Map<String, Department> buildDepartmentIndex() {
        Map<String, Department> index = new HashMap<>();
        for (Department department : departmentRepository.findAll()) {
            index.putIfAbsent(normalizeName(department.getName()), department);
            index.putIfAbsent(normalizeName(department.getCode()), department);
        }
        index.remove("");
        return index;
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void assignRoles(Long userId, Set<String> roleCodes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Set<String> before = roleCodesOf(user);

        if (roleCodes == null || roleCodes.isEmpty()) {
            user.setRoles(new HashSet<>());
        } else {
            List<Role> roles = roleRepository.findByCodeIn(roleCodes);
            if (roles.size() < roleCodes.size()) {
                throw new AppException(ErrorCode.ROLE_NOT_FOUND);
            }
            user.setRoles(new HashSet<>(roles));
        }
        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(new AuditEvent(
                "ASSIGN_USER_ROLES", "User", userId, before, roleCodesOf(savedUser)));
    }
}
