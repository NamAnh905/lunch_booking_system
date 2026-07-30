package vn.vnpost.lunchorder.core.modules.dish.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.core.modules.dish.entity.Dish;
import vn.vnpost.lunchorder.common.enums.DishType;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.dish.repository.DishRepository;
import vn.vnpost.lunchorder.core.modules.dish.service.DishService;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishCreateRequest;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishUpdateRequest;
import vn.vnpost.lunchorder.core.modules.dish.service.mapstruct.DishMapper;
import vn.vnpost.lunchorder.tools.excel.ExcelExportService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DishServiceImpl implements DishService {
    private final DishRepository dishRepository;
    private final DishMapper dishMapper;
    private final ExcelExportService excelExportService;

    @Override
    @Transactional
    @CacheEvict(value = "dishes", allEntries = true)
    public DishResponse create(DishCreateRequest request) {
        if (dishRepository.findByName(request.getName()).isPresent()) {
            throw new AppException(ErrorCode.DISH_ALREADY_EXISTS);
        }
        Dish dish = dishMapper.toEntity(request);
        dish = dishRepository.save(dish);
        return dishMapper.toDto(dish);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dishes", allEntries = true)
    public DishResponse update(Long id, DishUpdateRequest request) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISH_NOT_FOUND));
        dishMapper.update(request, dish);
        dish = dishRepository.save(dish);
        return dishMapper.toDto(dish);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dishes", allEntries = true)
    public void delete(Long id) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISH_NOT_FOUND));
        dishRepository.delete(dish);
    }

    @Override
    @Cacheable(value = "dishes", key = "'list:' + #page + '-' + #size + '-' + #keyword + '-' + #types + '-' + #isActives")
    public PageResponse<DishResponse> findAll(int page, int size, String keyword, List<String> types, List<Boolean> isActives) {
        Pageable pageable = PaginationConstants.toPageable(page, size, Sort.by(Sort.Direction.DESC, "id"));
        List<DishType> dishTypes = parseDishTypes(types);

        Specification<Dish> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (!dishTypes.isEmpty()) {
                predicates.add(root.get("type").in(dishTypes));
            }
            if (isActives != null && !isActives.isEmpty()) {
                predicates.add(root.get("isActive").in(isActives));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Dish> dishPage = dishRepository.findAll(spec, pageable);

        return PageResponse.of(dishPage, dishMapper.toDtoList(dishPage.getContent()), page, size);
    }

    @Override
    @Cacheable(value = "dishes", key = "'all'")
    public List<DishResponse> getAll() {
        Pageable pageable = PageRequest.of(0, PaginationConstants.MAX_LOOKUP_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        List<Dish> dishes = dishRepository.findAll(pageable).getContent();
        return dishMapper.toDtoList(dishes);
    }

    @Override
    public byte[] exportExcel(String keyword) {
        try (ByteArrayInputStream in = excelExportService.exportToExcel(export(keyword), "Danh sách món ăn")) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.EXPORT_FAILED);
        }
    }

    private List<DishType> parseDishTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return List.of();
        }
        List<DishType> parsed = new ArrayList<>();
        for (String type : types) {
            if (type == null || type.isBlank()) {
                continue;
            }
            try {
                parsed.add(DishType.valueOf(type.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_ENUM_VALUE);
            }
        }
        return parsed;
    }

    private List<DishResponse> export(String keyword) {
        List<Dish> dishes;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if (keyword != null && !keyword.trim().isEmpty()) {
            dishes = dishRepository.findByNameContainingIgnoreCase(keyword.trim());
        } else {
            dishes = dishRepository.findAll(sort);
        }
        return dishMapper.toDtoList(dishes);
    }
}
