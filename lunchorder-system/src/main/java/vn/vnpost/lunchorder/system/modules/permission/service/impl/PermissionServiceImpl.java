package vn.vnpost.lunchorder.system.modules.permission.service.impl;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.vnpost.lunchorder.common.audit.AuditEvent;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.system.modules.permission.entity.Permission;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.system.modules.permission.repository.PermissionRepository;
import vn.vnpost.lunchorder.system.modules.permission.service.PermissionService;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionCreateRequest;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionResponse;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionUpdateRequest;
import vn.vnpost.lunchorder.system.modules.permission.service.mapstruct.PermissionMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionResponse create(PermissionCreateRequest request) {
        if (permissionRepository.findByAction(request.getAction()).isPresent()) {
            throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
        }
        Permission permission = permissionMapper.toEntity(request);
        Permission savedPermission = permissionRepository.save(permission);
        PermissionResponse created = permissionMapper.toDto(savedPermission);
        eventPublisher.publishEvent(new AuditEvent(
                "CREATE_PERMISSION", "Permission", savedPermission.getId(), null, created));
        return created;
    }

    @Override
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionResponse update(Long id, PermissionUpdateRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        PermissionResponse before = permissionMapper.toDto(permission);

        permissionMapper.update(request, permission);
        Permission savedPermission = permissionRepository.save(permission);
        PermissionResponse after = permissionMapper.toDto(savedPermission);
        eventPublisher.publishEvent(new AuditEvent("UPDATE_PERMISSION", "Permission", id, before, after));
        return after;
    }

    @Override
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        PermissionResponse before = permissionMapper.toDto(permission);

        permissionRepository.delete(permission);
        eventPublisher.publishEvent(new AuditEvent("DELETE_PERMISSION", "Permission", id, before, null));
    }

    @Override
    @Cacheable(value = "permissions", key = "'findByAction:' + #action")
    public PermissionResponse findByAction(String action) {
        Permission permission = permissionRepository.findByAction(action)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        return permissionMapper.toDto(permission);
    }

    @Override
    @Cacheable(value = "permissions", key = "'all'")
    public List<PermissionResponse> getAll() {
        Pageable pageable = PageRequest.of(0, PaginationConstants.MAX_LOOKUP_SIZE);
        List<Permission> permissions = permissionRepository.findAll(pageable).getContent();
        return permissionMapper.toDtoList(permissions);
    }

    @Override
    @Cacheable(value = "permissions", key = "'findAll:' + #keyword + ':' + #page + ':' + #size")
    public PageResponse<PermissionResponse> findAll(String keyword, int page, int size) {
        Pageable pageable = PaginationConstants.toPageable(page, size);

        Page<Permission> permissionPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            permissionPage = permissionRepository.findByActionContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            permissionPage = permissionRepository.findAll(pageable);
        }

        return PageResponse.of(permissionPage, permissionMapper.toDtoList(permissionPage.getContent()), page, size);
    }
}
