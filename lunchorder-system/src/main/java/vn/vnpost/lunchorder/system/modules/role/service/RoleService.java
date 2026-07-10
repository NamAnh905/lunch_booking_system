package vn.vnpost.lunchorder.system.modules.role.service;

import java.util.List;
import java.util.Set;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleCreateRequest;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleResponse;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleUpdateRequest;

public interface RoleService {
    RoleResponse create(RoleCreateRequest request);

    RoleResponse update(Long id, RoleUpdateRequest request);

    void delete(Long id);

    RoleResponse findByCode(String code);

    List<RoleResponse> findAll(String keyword);

    void assignPermissions(Long roleId, Set<String> permissionCodes);
}
