package vn.vnpost.lunchorder.system.modules.user.service;

import java.util.List;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.ChangePasswordRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.ProfileUpdateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserCreateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserImportResultResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserUpdateRequest;

public interface UserService {
    UserResponse create(UserCreateRequest request);

    UserResponse update(Long id, UserUpdateRequest request);

    UserResponse updateProfile(String username, ProfileUpdateRequest request);

    void changePassword(String username, ChangePasswordRequest request);

    void delete(Long id);

    UserResponse findByUsername(String username);

    PageResponse<UserResponse> findAll(int page, int size, String keyword, List<Long> departmentIds,
            List<Boolean> isActives, String sortBy, String sortDir);

    List<UserResponse> getAll();


    byte[] exportExcel(String keyword);

    byte[] buildImportTemplate();

    UserImportResultResponse importExcel(MultipartFile file);

    void assignRoles(Long userId, Set<String> roleCodes);
}
