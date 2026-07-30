package vn.vnpost.lunchorder.core.modules.price.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.audit.AuditEvent;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.common.enums.MealType;
import vn.vnpost.lunchorder.core.modules.price.entity.Price;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.price.repository.PriceRepository;
import vn.vnpost.lunchorder.core.modules.price.service.PriceService;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceCreateRequest;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceUpdateRequest;
import vn.vnpost.lunchorder.core.modules.price.service.mapstruct.PriceMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PriceServiceImpl implements PriceService {

    private final PriceRepository priceRepository;
    private final PriceMapper priceMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = "prices", allEntries = true)
    public PriceResponse create(PriceCreateRequest request) {
        if (priceRepository.findByName(request.getName()).isPresent()) {
            throw new AppException(ErrorCode.PRICE_ALREADY_EXISTS);
        }
        assertMealTypeAvailable(request.getMealType(), request.getIsActive(), null);

        Price price = priceMapper.toEntity(request);
        price = priceRepository.save(price);
        eventPublisher.publishEvent(new AuditEvent("CREATE_PRICE", "Price", price.getId(), null, priceMapper.toDto(price)));
        return priceMapper.toDto(price);
    }

    @Override
    @Transactional
    @CacheEvict(value = "prices", allEntries = true)
    public PriceResponse update(Long id, PriceUpdateRequest request) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRICE_NOT_FOUND));
        PriceResponse before = priceMapper.toDto(price);

        assertMealTypeAvailable(
                request.getMealType() != null ? request.getMealType() : price.getMealType(),
                request.getIsActive() != null ? request.getIsActive() : price.getIsActive(),
                id);

        priceMapper.update(request, price);
        price = priceRepository.save(price);
        eventPublisher.publishEvent(new AuditEvent("UPDATE_PRICE", "Price", id, before, priceMapper.toDto(price)));
        return priceMapper.toDto(price);
    }

    private void assertMealTypeAvailable(MealType mealType, Boolean isActive, Long currentId) {
        if (mealType == null || !Boolean.TRUE.equals(isActive)) {
            return;
        }
        priceRepository.findByMealTypeAndIsActiveTrue(mealType)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.PRICE_MEAL_TYPE_ALREADY_ACTIVE);
                });
    }

    @Override
    @Transactional
    @CacheEvict(value = "prices", allEntries = true)
    public void delete(Long id) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRICE_NOT_FOUND));
        PriceResponse before = priceMapper.toDto(price);

        price.setIsActive(false);
        price = priceRepository.save(price);
        eventPublisher.publishEvent(new AuditEvent("DELETE_PRICE", "Price", id, before, priceMapper.toDto(price)));
    }

    @Override
    @Cacheable(value = "prices", key = "'list:' + #page + '-' + #size + '-' + #keyword")
    public PageResponse<PriceResponse> findAll(int page, int size, String keyword) {
        Pageable pageable = PaginationConstants.toPageable(page, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<Price> pricePage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            pricePage = priceRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            pricePage = priceRepository.findAll(pageable);
        }

        return PageResponse.of(pricePage, priceMapper.toDtoList(pricePage.getContent()), page, size);
    }

    @Override
    @Cacheable(value = "prices", key = "'active'")
    public List<PriceResponse> getActivePrices() {
        List<Price> activePrices = priceRepository.findByIsActiveTrue();
        return priceMapper.toDtoList(activePrices);
    }
}
