package vn.vnpost.lunchorder.core.modules.menu.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.entity.Dish;
import vn.vnpost.lunchorder.common.entity.Menu;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.dish.repository.DishRepository;
import vn.vnpost.lunchorder.core.modules.menu.repository.MenuRepository;
import vn.vnpost.lunchorder.core.modules.menu.service.MenuService;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuUpdateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.mapstruct.MenuMapper;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final DishRepository dishRepository;

    @Override
    @Transactional
    public MenuResponse create(MenuCreateRequest request) {
        if (menuRepository.findByMenuDate(request.getMenuDate()).isPresent()) {
            throw new AppException(ErrorCode.MENU_ALREADY_EXISTS);
        }

        Menu menu = menuMapper.toEntity(request);

        if (request.getDishIds() != null && !request.getDishIds().isEmpty()) {
            List<Dish> dishes = dishRepository.findAllById(request.getDishIds());
            menu.setDishes(new HashSet<>(dishes));
        }

        menu = menuRepository.save(menu);
        return menuMapper.toDto(menu);
    }

    @Override
    @Transactional
    public MenuResponse update(Long id, MenuUpdateRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));

        Optional<Menu> existingMenuOpt = menuRepository.findByMenuDate(request.getMenuDate());
        if (existingMenuOpt.isPresent() && !existingMenuOpt.get().getId().equals(id)) {
            throw new AppException(ErrorCode.MENU_ALREADY_EXISTS);
        }

        menuMapper.update(request, menu);

        if (request.getDishIds() != null) {
            List<Dish> dishes = dishRepository.findAllById(request.getDishIds());
            menu.setDishes(new HashSet<>(dishes));
        } else {
            menu.setDishes(new HashSet<>());
        }

        menu = menuRepository.save(menu);
        return menuMapper.toDto(menu);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));
        menuRepository.delete(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MenuResponse> findAll(int page) {
        int pageSize = 10;
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Menu> menuPage = menuRepository.findAll(pageable);
        List<MenuResponse> dtoList = menuMapper.toDtoList(menuPage.getContent());

        return PageResponse.<MenuResponse>builder()
                .currentPage(page)
                .totalPages(menuPage.getTotalPages())
                .pageSize(pageSize)
                .totalElements(menuPage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuResponse findById(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));
        return menuMapper.toDto(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuResponse findByDate(LocalDate date) {
        Menu menu = menuRepository.findByMenuDate(date)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));
        return menuMapper.toDto(menu);
    }
}
