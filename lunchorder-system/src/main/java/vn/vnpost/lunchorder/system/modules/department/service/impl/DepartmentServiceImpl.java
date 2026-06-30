package vn.vnpost.lunchorder.system.modules.department.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.entity.Department;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.system.modules.department.repository.DepartmentRepository;
import vn.vnpost.lunchorder.system.modules.department.service.DepartmentService;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentCreateRequest;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentResponse;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentUpdateRequest;
import vn.vnpost.lunchorder.system.modules.department.service.mapstruct.DepartmentMapper;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        if (departmentRepository.findByCode(request.getCode()).isPresent()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        Department department = departmentMapper.toEntity(request);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toDto(savedDepartment);
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpdateRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        departmentMapper.update(request, department);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toDto(savedDepartment);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        departmentRepository.delete(department);
    }

    @Override
    public DepartmentResponse findByCode(String code) {
        Department department = departmentRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        return departmentMapper.toDto(department);
    }

    @Override
    public List<DepartmentResponse> search(String keyword) {
        List<Department> departments = departmentRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword);
        return departmentMapper.toDtoList(departments);
    }

    @Override
    public PageResponse<DepartmentResponse> findAll(int page) {
        int pageSize = 10;
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Department> departmentPage = departmentRepository.findAll(pageable);
        List<DepartmentResponse> dtoList = departmentMapper.toDtoList(departmentPage.getContent());

        return PageResponse.<DepartmentResponse>builder()
                .currentPage(page)
                .totalPages(departmentPage.getTotalPages())
                .pageSize(pageSize)
                .totalElements(departmentPage.getTotalElements())
                .data(dtoList)
                .build();
    }
}
