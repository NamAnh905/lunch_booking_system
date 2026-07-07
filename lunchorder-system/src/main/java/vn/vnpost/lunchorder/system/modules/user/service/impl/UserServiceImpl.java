package vn.vnpost.lunchorder.system.modules.user.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toDto(user);
    }

    @Override
    public PageResponse<UserResponse> findAll(int page, String keyword) {
        int pageSize = 10;
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<User> userPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            userPage = userRepository.findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<UserResponse> dtoList = userMapper.toDtoList(userPage.getContent());

        return PageResponse.<UserResponse>builder()
                .currentPage(page)
                .totalPages(userPage.getTotalPages())
                .pageSize(pageSize)
                .totalElements(userPage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    public List<UserResponse> search(String keyword) {
        List<User> users = userRepository.findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword, keyword);
        return userMapper.toDtoList(users);
    }

    @Override
    @Transactional
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
