package vn.vnpost.lunchorder.system.modules.role.service.impl;

import java.util.HashSet;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.entity.Permission;
import vn.vnpost.lunchorder.common.entity.Role;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.system.modules.permission.repository.PermissionRepository;
import vn.vnpost.lunchorder.system.modules.role.repository.RoleRepository;
import vn.vnpost.lunchorder.system.modules.role.service.RoleService;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleCreateRequest;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleResponse;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleUpdateRequest;
import vn.vnpost.lunchorder.system.modules.role.service.mapstruct.RoleMapper;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        Role role = roleMapper.toEntity(request);

        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllByActionIn(request.getPermissions());
            role.setPermissions(new HashSet<>(permissions));
        }

        Role savedRole = roleRepository.save(role);
        return roleMapper.toDto(savedRole);
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        roleMapper.update(request, role);

        if (request.getPermissions() != null) {
            List<Permission> permissions = permissionRepository.findAllByActionIn(request.getPermissions());
            role.setPermissions(new HashSet<>(permissions));
        } else {
            role.setPermissions(null);
        }

        Role savedRole = roleRepository.save(role);
        return roleMapper.toDto(savedRole);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        roleRepository.delete(role);
    }

    @Override
    public RoleResponse findByCode(String code) {
        Role role = roleRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toDto(role);
    }

    @Override
    public PageResponse<RoleResponse> findAll(int page) {
        int pageSize = 10;
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Role> rolePage = roleRepository.findAll(pageable);
        List<RoleResponse> dtoList = roleMapper.toDtoList(rolePage.getContent());

        return PageResponse.<RoleResponse>builder()
                .currentPage(page)
                .totalPages(rolePage.getTotalPages())
                .pageSize(pageSize)
                .totalElements(rolePage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, Set<String> permissionCodes) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if (permissionCodes == null || permissionCodes.isEmpty()) {
            role.setPermissions(new HashSet<>());
        } else {
            List<Permission> permissions = permissionRepository.findAllByActionIn(permissionCodes);
            if (permissions.size() < permissionCodes.size()) {
                throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
            }
            role.setPermissions(new HashSet<>(permissions));
        }
        roleRepository.save(role);
    }
}
