package vn.vnpost.lunchorder.system.modules.permission.service;

import java.util.List;

import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionCreateRequest;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionResponse;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionUpdateRequest;

public interface PermissionService {
    PermissionResponse create(PermissionCreateRequest request);

    PermissionResponse update(Long id, PermissionUpdateRequest request);

    void delete(Long id);

    PermissionResponse findByAction(String action);

    List<PermissionResponse> getAll();

    PageResponse<PermissionResponse> findAll(String keyword, int page, int size);
}
