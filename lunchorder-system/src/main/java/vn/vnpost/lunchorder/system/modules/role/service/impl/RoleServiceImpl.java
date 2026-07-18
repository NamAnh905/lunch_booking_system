package vn.vnpost.lunchorder.system.modules.role.service.impl;

import java.util.HashSet;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.vnpost.lunchorder.system.modules.permission.entity.Permission;
import vn.vnpost.lunchorder.system.modules.role.entity.Role;
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
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public RoleResponse create(RoleCreateRequest request) {
        if (roleRepository.findByCode(request.getCode()).isPresent()) {
            throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
        }
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
    @CacheEvict(value = "roles", allEntries = true)
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
    @CacheEvict(value = "roles", allEntries = true)
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        roleRepository.delete(role);
    }

    @Override
    @Cacheable(value = "roles", key = "'findByCode:' + #code")
    public RoleResponse findByCode(String code) {
        Role role = roleRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toDto(role);
    }

    @Override
    @Cacheable(value = "roles", key = "'findAll:' + #keyword")
    public List<RoleResponse> findAll(String keyword) {
        List<Role> roles;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if (keyword != null && !keyword.trim().isEmpty()) {
            roles = roleRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword, sort);
        } else {
            roles = roleRepository.findAll(sort);
        }
        return roleMapper.toDtoList(roles);
    }

    @Override
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
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
