package vn.vnpost.lunchorder.core.modules.menu.service;

import java.time.LocalDate;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuUpdateRequest;

public interface MenuService {
    MenuResponse create(MenuCreateRequest request);

    MenuResponse update(Long id, MenuUpdateRequest request);

    void delete(Long id);

    PageResponse<MenuResponse> findAll(int page);

    MenuResponse findById(Long id);

    MenuResponse findByDate(LocalDate date);
}
