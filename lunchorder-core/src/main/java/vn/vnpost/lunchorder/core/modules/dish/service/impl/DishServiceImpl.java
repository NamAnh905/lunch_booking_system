package vn.vnpost.lunchorder.core.modules.dish.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.entity.Dish;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.dish.repository.DishRepository;
import vn.vnpost.lunchorder.core.modules.dish.service.DishService;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishCreateRequest;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishUpdateRequest;
import vn.vnpost.lunchorder.core.modules.dish.service.mapstruct.DishMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishServiceImpl implements DishService {
    private final DishRepository dishRepository;
    private final DishMapper dishMapper;

    @Override
    public DishResponse create(DishCreateRequest request) {
        Dish dish = dishMapper.toEntity(request);
        dish = dishRepository.save(dish);
        return dishMapper.toDto(dish);
    }

    @Override
    public DishResponse update(Long id, DishUpdateRequest request) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISH_NOT_FOUND));
        dishMapper.update(request, dish);
        dish = dishRepository.save(dish);
        return dishMapper.toDto(dish);
    }

    @Override
    public void delete(Long id) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISH_NOT_FOUND));
        dishRepository.delete(dish);
    }

    @Override
    public PageResponse<DishResponse> findAll(int page) {
        int pageSize = 10;
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Dish> dishPage = dishRepository.findAll(pageable);
        List<DishResponse> dtoList = dishMapper.toDtoList(dishPage.getContent());

        return PageResponse.<DishResponse>builder()
                .currentPage(page)
                .totalPages(dishPage.getTotalPages())
                .pageSize(pageSize)
                .totalElements(dishPage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    public List<DishResponse> search(String name) {
        List<Dish> dishes = dishRepository.findByNameContainingIgnoreCase(name);
        return dishMapper.toDtoList(dishes);
    }
}
