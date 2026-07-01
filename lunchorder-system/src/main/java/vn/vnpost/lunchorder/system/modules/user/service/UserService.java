package vn.vnpost.lunchorder.system.modules.user.service;

import java.util.List;
import java.util.Set;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserCreateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserUpdateRequest;

public interface UserService {
    UserResponse create(UserCreateRequest request);

    UserResponse update(Long id, UserUpdateRequest request);

    void delete(Long id);

    UserResponse findByUsername(String username);

    UserResponse findByEmployeeCode(String employeeCode);

    PageResponse<UserResponse> findAll(int page);

    List<UserResponse> search(String keyword);

    void assignRoles(Long userId, Set<String> roleCodes);
}
