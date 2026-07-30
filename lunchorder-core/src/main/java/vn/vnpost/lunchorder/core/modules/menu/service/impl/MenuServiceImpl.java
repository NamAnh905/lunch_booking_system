package vn.vnpost.lunchorder.core.modules.menu.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.core.modules.dish.entity.Dish;
import vn.vnpost.lunchorder.core.modules.menu.entity.Menu;
import vn.vnpost.lunchorder.common.enums.MenuType;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.dish.repository.DishRepository;
import vn.vnpost.lunchorder.core.modules.menu.repository.MenuRepository;
import vn.vnpost.lunchorder.core.modules.price.repository.PriceRepository;
import vn.vnpost.lunchorder.core.modules.price.entity.Price;
import vn.vnpost.lunchorder.core.modules.menu.service.MenuService;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuImageCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuUpdateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.mapstruct.MenuMapper;
import vn.vnpost.lunchorder.tools.excel.ExcelExportService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final DishRepository dishRepository;
    private final PriceRepository priceRepository;
    private final ExcelExportService excelExportService;

    @Override
    @Transactional
    @CacheEvict(value = "menus", allEntries = true)
    public MenuResponse create(MenuCreateRequest request) {
        if (menuRepository.findByMenuDateAndPriceId(request.getMenuDate(), request.getPriceId()).isPresent()) {
            throw new AppException(ErrorCode.MENU_ALREADY_EXISTS);
        }

        Price price = priceRepository.findById(request.getPriceId())
                .orElseThrow(() -> new AppException(ErrorCode.PRICE_NOT_FOUND));

        Menu menu = menuMapper.toEntity(request);
        menu.setPrice(price);
        menu.setDishes(resolveOrderedDishes(request.getDishIds()));

        menu = menuRepository.save(menu);
        return menuMapper.toDto(menu);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menus", allEntries = true)
    public MenuResponse createImageMenu(MenuImageCreateRequest request) {
        LocalDate monday = request.getWeekDate().with(DayOfWeek.MONDAY);

        if (menuRepository.findByMenuDateAndType(monday, MenuType.IMAGE).isPresent()) {
            throw new AppException(ErrorCode.MENU_ALREADY_EXISTS);
        }

        Menu menu = new Menu();
        menu.setName(request.getName());
        menu.setType(MenuType.IMAGE);
        menu.setImageUrl(request.getImageUrl());
        menu.setMenuDate(monday);
        menu.setStatus("ACTIVE");
        menu.setDishes(new ArrayList<>());

        menu = menuRepository.save(menu);
        return menuMapper.toDto(menu);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menus", allEntries = true)
    public MenuResponse updateImageMenu(Long id, MenuImageCreateRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));

        LocalDate monday = request.getWeekDate().with(DayOfWeek.MONDAY);

        Optional<Menu> existing = menuRepository.findByMenuDateAndType(monday, MenuType.IMAGE);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new AppException(ErrorCode.MENU_ALREADY_EXISTS);
        }

        menu.setName(request.getName());
        menu.setType(MenuType.IMAGE);
        menu.setImageUrl(request.getImageUrl());
        menu.setMenuDate(monday);

        menu = menuRepository.save(menu);
        return menuMapper.toDto(menu);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menus", allEntries = true)
    public MenuResponse update(Long id, MenuUpdateRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));

        Optional<Menu> existingMenuOpt = menuRepository.findByMenuDateAndPriceId(request.getMenuDate(),
                request.getPriceId());
        if (existingMenuOpt.isPresent() && !existingMenuOpt.get().getId().equals(id)) {
            throw new AppException(ErrorCode.MENU_ALREADY_EXISTS);
        }

        Price price = priceRepository.findById(request.getPriceId())
                .orElseThrow(() -> new AppException(ErrorCode.PRICE_NOT_FOUND));

        menuMapper.update(request, menu);
        menu.setPrice(price);
        menu.setDishes(resolveOrderedDishes(request.getDishIds()));

        menu = menuRepository.save(menu);
        return menuMapper.toDto(menu);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menus", allEntries = true)
    public void delete(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));
        menuRepository.delete(menu);
    }

    @Override
    @Cacheable(value = "menus", key = "'list:' + #page + '-' + #size + '-' + #keyword")
    public PageResponse<MenuResponse> findAll(int page, int size, String keyword) {
        Pageable pageable = PaginationConstants.toPageable(page, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<Menu> menuPage = menuRepository.searchMenus(keyword, pageable);

        return PageResponse.of(menuPage, menuMapper.toDtoList(menuPage.getContent()), page, size);
    }

    @Override
    @Cacheable(value = "menus", key = "'byRange:' + #startDate + '-' + #endDate")
    public List<MenuResponse> findByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Menu> menus = menuRepository.findByMenuDateBetween(startDate, endDate);
        return menuMapper.toDtoList(menus);
    }

    private List<Dish> resolveOrderedDishes(List<Long> dishIds) {
        if (dishIds == null || dishIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Dish> dishMap = dishRepository.findAllById(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, dish -> dish));

        return dishIds.stream()
                .map(dishMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportExcel(String keyword) {
        try (ByteArrayInputStream in = excelExportService.exportToExcel(export(keyword), "Danh sách thực đơn")) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.EXPORT_FAILED);
        }
    }

    private List<MenuResponse> export(String keyword) {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        List<Menu> menus = menuRepository.searchMenusList(keyword, sort);
        return menuMapper.toDtoList(menus);
    }
}
