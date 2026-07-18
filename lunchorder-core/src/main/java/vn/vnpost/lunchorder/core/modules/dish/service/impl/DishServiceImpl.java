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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DishServiceImpl implements DishService {
    private final DishRepository dishRepository;
    private final DishMapper dishMapper;

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
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, PaginationConstants.clampSize(size), Sort.by(Sort.Direction.DESC, "id"));

        Specification<Dish> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (types != null && !types.isEmpty()) {
                List<DishType> dishTypes = types.stream()
                        .map(DishType::valueOf)
                        .toList();
                predicates.add(root.get("type").in(dishTypes));
            }
            if (isActives != null && !isActives.isEmpty()) {
                predicates.add(root.get("isActive").in(isActives));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Dish> dishPage = dishRepository.findAll(spec, pageable);
        
        List<DishResponse> dtoList = dishMapper.toDtoList(dishPage.getContent());

        return PageResponse.<DishResponse>builder()
                .currentPage(page)
                .totalPages(dishPage.getTotalPages())
                .pageSize(size)
                .totalElements(dishPage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    @Cacheable(value = "dishes", key = "'all'")
    public List<DishResponse> getAll() {
        Pageable pageable = PageRequest.of(0, PaginationConstants.MAX_LOOKUP_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        List<Dish> dishes = dishRepository.findAll(pageable).getContent();
        return dishMapper.toDtoList(dishes);
    }

    @Override
    @Cacheable(value = "dishes", key = "'search:' + #name")
    public List<DishResponse> search(String name) {
        List<Dish> dishes = dishRepository.findByNameContainingIgnoreCase(name);
        return dishMapper.toDtoList(dishes);
    }

    @Override
    @Cacheable(value = "dishes", key = "'export:' + #keyword")
    public List<DishResponse> export(String keyword) {
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
