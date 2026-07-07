package vn.vnpost.lunchorder.core.modules.menu.service;

import java.time.LocalDate;
import java.util.List;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuUpdateRequest;

public interface MenuService {
    MenuResponse create(MenuCreateRequest request);

    MenuResponse update(Long id, MenuUpdateRequest request);

    void delete(Long id);

    PageResponse<MenuResponse> findAll(int page, int size, String keyword);

    MenuResponse findById(Long id);

    List<MenuResponse> findByDate(LocalDate date);
}
