package vn.vnpost.lunchorder.system.modules.permission.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
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

    @Override
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionResponse create(PermissionCreateRequest request) {
        if (permissionRepository.findByAction(request.getAction()).isPresent()) {
            throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
        }
        Permission permission = permissionMapper.toEntity(request);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toDto(savedPermission);
    }

    @Override
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionResponse update(Long id, PermissionUpdateRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));

        permissionMapper.update(request, permission);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toDto(savedPermission);
    }

    @Override
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        permissionRepository.delete(permission);
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
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, PaginationConstants.clampSize(size));

        Page<Permission> permissionPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            permissionPage = permissionRepository.findByActionContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            permissionPage = permissionRepository.findAll(pageable);
        }

        List<PermissionResponse> dtoList = permissionMapper.toDtoList(permissionPage.getContent());

        return PageResponse.<PermissionResponse>builder()
                .currentPage(page)
                .totalPages(permissionPage.getTotalPages())
                .pageSize(size)
                .totalElements(permissionPage.getTotalElements())
                .data(dtoList)
                .build();
    }
}
