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

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

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
    public UserResponse create(UserCreateRequest request) {
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getDepartment() != null) {
            user.setDepartment(resolveDepartment(request.getDepartment()));
        }
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        userMapper.update(request, user);
        if (request.getDepartment() != null) {
            user.setDepartment(resolveDepartment(request.getDepartment()));
        }

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
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
    public UserResponse findByEmployeeCode(String employeeCode) {
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toDto(user);
    }

    @Override
    public PageResponse<UserResponse> findAll(int page) {
        int pageSize = 10;
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<User> userPage = userRepository.findAll(pageable);

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
        List<User> users = userRepository.findByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCase(keyword, keyword);
        return userMapper.toDtoList(users);
    }
}
