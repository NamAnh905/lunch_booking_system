package vn.vnpost.lunchorder.system.modules.user.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.entity.Department;
import vn.vnpost.lunchorder.common.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.system.modules.department.repository.DepartmentRepository;
import vn.vnpost.lunchorder.system.modules.user.repository.UserRepository;
import vn.vnpost.lunchorder.system.modules.user.service.UserService;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserCreateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserUpdateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.mapstruct.UserMapper;
import vn.vnpost.lunchorder.system.modules.role.repository.RoleRepository;
import vn.vnpost.lunchorder.common.entity.Role;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    private Department resolveDepartment(String departmentValue) {
        if (departmentValue == null) {
            return null;
        }
        try {
            Long id = Long.parseLong(departmentValue);
            java.util.Optional<Department> deptOpt = departmentRepository.findById(id);
            if (deptOpt.isPresent()) {
                return deptOpt.get();
            }
        } catch (NumberFormatException e) {
        }

        return departmentRepository.findByCode(departmentValue)
                .or(() -> departmentRepository.findByName(departmentValue))
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
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
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    @Cacheable(value = "users")
    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toDto(user);
    }

    @Override
    @Cacheable(value = "users")
    public PageResponse<UserResponse> findAll(int page, int size, String keyword, List<Long> departmentIds, List<Boolean> isActives) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "id"));

        org.springframework.data.jpa.domain.Specification<User> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
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
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserResponse> dtoList = userMapper.toDtoList(userPage.getContent());

        return PageResponse.<UserResponse>builder()
                .currentPage(page)
                .totalPages(userPage.getTotalPages())
                .pageSize(size)
                .totalElements(userPage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    @Cacheable(value = "users")
    public List<UserResponse> search(String keyword) {
        List<User> users = userRepository.findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword, keyword);
        return userMapper.toDtoList(users);
    }

    @Override
    @Cacheable(value = "users")
    public List<UserResponse> export(String keyword) {
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

        if (roleCodes == null || roleCodes.isEmpty()) {
            user.setRoles(new HashSet<>());
        } else {
            List<Role> roles = roleRepository.findByCodeIn(roleCodes);
            if (roles.size() < roleCodes.size()) {
                throw new AppException(ErrorCode.ROLE_NOT_FOUND);
            }
            user.setRoles(new HashSet<>(roles));
        }
        userRepository.save(user);
    }
}
