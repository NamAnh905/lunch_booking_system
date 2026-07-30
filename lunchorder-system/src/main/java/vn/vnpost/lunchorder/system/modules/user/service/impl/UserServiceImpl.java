package vn.vnpost.lunchorder.system.modules.user.service.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import jakarta.persistence.criteria.Predicate;
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

import lombok.RequiredArgsConstructor;
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
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserUpdateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.mapstruct.UserMapper;
import vn.vnpost.lunchorder.system.modules.role.repository.RoleRepository;
import vn.vnpost.lunchorder.system.modules.role.entity.Role;
import vn.vnpost.lunchorder.tools.excel.ExcelExportService;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ExcelExportService excelExportService;

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
    @Cacheable(value = "users", key = "'list:' + #page + '-' + #size + '-' + #keyword + '-' + #departmentIds + '-' + #isActives")
    public PageResponse<UserResponse> findAll(int page, int size, String keyword, List<Long> departmentIds, List<Boolean> isActives) {
        Pageable pageable = PaginationConstants.toPageable(page, size, Sort.by(Sort.Direction.DESC, "id"));

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
