package vn.vnpost.lunchorder.core.modules.dish.service;

import java.util.List;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishCreateRequest;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishUpdateRequest;

public interface DishService {
    DishResponse create(DishCreateRequest request);

    DishResponse update(Long id, DishUpdateRequest request);

    void delete(Long id);

    PageResponse<DishResponse> findAll(int page, int size, String keyword, List<String> types, List<Boolean> isActives);

    List<DishResponse> getAll();

    List<DishResponse> search(String name);

    List<DishResponse> export(String keyword);
}
