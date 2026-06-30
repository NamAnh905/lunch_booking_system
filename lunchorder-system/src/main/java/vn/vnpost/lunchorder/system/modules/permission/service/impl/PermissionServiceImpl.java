package vn.vnpost.lunchorder.system.modules.permission.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.entity.Permission;
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
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional
    public PermissionResponse create(PermissionCreateRequest request) {
        if (permissionRepository.findByAction(request.getAction()).isPresent()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        Permission permission = permissionMapper.toEntity(request);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toDto(savedPermission);
    }

    @Override
    @Transactional
    public PermissionResponse update(Long id, PermissionUpdateRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));

        permissionMapper.update(request, permission);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toDto(savedPermission);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        permissionRepository.delete(permission);
    }

    @Override
    public PermissionResponse findByAction(String action) {
        Permission permission = permissionRepository.findByAction(action)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        return permissionMapper.toDto(permission);
    }

    @Override
    public PageResponse<PermissionResponse> findAll(int page) {
        int pageSize = 10;
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Permission> permissionPage = permissionRepository.findAll(pageable);
        List<PermissionResponse> dtoList = permissionMapper.toDtoList(permissionPage.getContent());

        return PageResponse.<PermissionResponse>builder()
                .currentPage(page)
                .totalPages(permissionPage.getTotalPages())
                .pageSize(pageSize)
                .totalElements(permissionPage.getTotalElements())
                .data(dtoList)
                .build();
    }
}
