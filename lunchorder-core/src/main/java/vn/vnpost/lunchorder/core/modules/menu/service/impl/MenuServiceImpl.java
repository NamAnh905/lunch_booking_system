package vn.vnpost.lunchorder.core.modules.menu.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.common.entity.Dish;
import vn.vnpost.lunchorder.common.entity.Menu;
import vn.vnpost.lunchorder.common.enums.MenuType;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.dish.repository.DishRepository;
import vn.vnpost.lunchorder.core.modules.menu.repository.MenuRepository;
import vn.vnpost.lunchorder.core.modules.price.repository.PriceRepository;
import vn.vnpost.lunchorder.common.entity.Price;
import vn.vnpost.lunchorder.core.modules.menu.service.MenuService;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuImageCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuUpdateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.mapstruct.MenuMapper;

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

        if (request.getDishIds() != null && !request.getDishIds().isEmpty()) {
            List<Dish> fetchedDishes = dishRepository.findAllById(request.getDishIds());
            Map<Long, Dish> dishMap = fetchedDishes.stream().collect(Collectors.toMap(Dish::getId, d -> d));
            List<Dish> orderedDishes = request.getDishIds().stream()
                    .map(dishMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            menu.setDishes(orderedDishes);
        } else {
            menu.setDishes(new ArrayList<>());
        }

        menu = menuRepository.save(menu);
        return menuMapper.toDto(menu);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menus", allEntries = true)
    public MenuResponse createImageMenu(MenuImageCreateRequest request) {
        // Chuẩn hóa về Thứ Hai của tuần để mỗi tuần chỉ có một menu ảnh.
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

        if (request.getDishIds() != null && !request.getDishIds().isEmpty()) {
            List<Dish> fetchedDishes = dishRepository.findAllById(request.getDishIds());
            Map<Long, Dish> dishMap = fetchedDishes.stream().collect(Collectors.toMap(Dish::getId, d -> d));
            List<Dish> orderedDishes = request.getDishIds().stream()
                    .map(dishMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            menu.setDishes(orderedDishes);
        } else {
            menu.setDishes(new ArrayList<>());
        }

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
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, PaginationConstants.clampSize(size), Sort.by(Sort.Direction.ASC, "id"));

        Page<Menu> menuPage = menuRepository.searchMenus(keyword, pageable);
        List<MenuResponse> dtoList = menuMapper.toDtoList(menuPage.getContent());

        return PageResponse.<MenuResponse>builder()
                .currentPage(page)
                .totalPages(menuPage.getTotalPages())
                .pageSize(size)
                .totalElements(menuPage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    public MenuResponse findById(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));
        return menuMapper.toDto(menu);
    }

    @Override
    @Cacheable(value = "menus", key = "'byDate:' + #date")
    public List<MenuResponse> findByDate(LocalDate date) {
        List<Menu> menus = menuRepository.findByMenuDate(date);
        if (menus.isEmpty()) {
            throw new AppException(ErrorCode.MENU_NOT_FOUND);
        }
        return menuMapper.toDtoList(menus);
    }

    @Override
    @Cacheable(value = "menus", key = "'byRange:' + #startDate + '-' + #endDate")
    public List<MenuResponse> findByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Menu> menus = menuRepository.findByMenuDateBetween(startDate, endDate);
        return menuMapper.toDtoList(menus);
    }

    @Override
    public List<MenuResponse> export(String keyword) {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        List<Menu> menus = menuRepository.searchMenusList(keyword, sort);
        return menuMapper.toDtoList(menus);
    }
}
