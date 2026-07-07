package vn.vnpost.lunchorder.system.modules.department.service;

import java.util.List;

import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentCreateRequest;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentResponse;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentUpdateRequest;

public interface DepartmentService {
    DepartmentResponse create(DepartmentCreateRequest request);

    DepartmentResponse update(Long id, DepartmentUpdateRequest request);

    void delete(Long id);

    DepartmentResponse findByCode(String code);

    List<DepartmentResponse> search(String keyword);

    PageResponse<DepartmentResponse> findAll(String keyword, int page, int size);
}
