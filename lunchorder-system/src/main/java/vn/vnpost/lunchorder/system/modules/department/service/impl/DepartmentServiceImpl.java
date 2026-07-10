package vn.vnpost.lunchorder.system.modules.department.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    private void populateUserCounts(List<Department> departments) {
        if (departments.isEmpty()) return;
        Map<Long, Long> countMap = departmentRepository.countUsersByDepartment()
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));
        departments.forEach(dept -> dept.setUserCount(countMap.getOrDefault(dept.getId(), 0L)));
    }

    @Override
    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
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
    @CacheEvict(value = "departments", allEntries = true)
    public DepartmentResponse update(Long id, DepartmentUpdateRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        departmentMapper.update(request, department);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toDto(savedDepartment);
    }

    @Override
    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
    public void delete(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        departmentRepository.delete(department);
    }

    @Override
    @Cacheable(value = "departments", key = "'findByCode:' + #code")
    public DepartmentResponse findByCode(String code) {
        Department department = departmentRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        return departmentMapper.toDto(department);
    }

    @Override
    @Cacheable(value = "departments", key = "'search:' + #keyword")
    public List<DepartmentResponse> search(String keyword) {
        List<Department> departments = departmentRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword);
        populateUserCounts(departments);
        return departmentMapper.toDtoList(departments);
    }

    @Override
    @Cacheable(value = "departments", key = "'findAll:' + #keyword + ':' + #page + ':' + #size")
    public PageResponse<DepartmentResponse> findAll(String keyword, int page, int size) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Department> departmentPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            departmentPage = departmentRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword.trim(), keyword.trim(), pageable);
        } else {
            departmentPage = departmentRepository.findAll(pageable);
        }

        populateUserCounts(departmentPage.getContent());
        List<DepartmentResponse> dtoList = departmentMapper.toDtoList(departmentPage.getContent());

        return PageResponse.<DepartmentResponse>builder()
                .currentPage(page)
                .totalPages(departmentPage.getTotalPages())
                .pageSize(size)
                .totalElements(departmentPage.getTotalElements())
                .data(dtoList)
                .build();
    }
}

