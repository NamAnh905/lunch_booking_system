package vn.vnpost.lunchorder.core.modules.menu.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuImageCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuUpdateRequest;

public interface MenuService {
    MenuResponse create(MenuCreateRequest request);

    MenuResponse createImageMenu(MenuImageCreateRequest request, MultipartFile image);

    MenuResponse updateImageMenu(Long id, MenuImageCreateRequest request, MultipartFile image);

    MenuResponse update(Long id, MenuUpdateRequest request);

    void delete(Long id);

    PageResponse<MenuResponse> findAll(int page, int size, String keyword);



    List<MenuResponse> findByDateRange(LocalDate startDate, LocalDate endDate);

    byte[] exportExcel(String keyword);
}
