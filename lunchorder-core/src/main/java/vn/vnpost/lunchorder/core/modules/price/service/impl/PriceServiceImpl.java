package vn.vnpost.lunchorder.core.modules.price.service.impl;

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
import vn.vnpost.lunchorder.common.entity.Price;
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

    @Override
    @Transactional
    @CacheEvict(value = "prices", allEntries = true)
    public PriceResponse create(PriceCreateRequest request) {
        if (priceRepository.findByName(request.getName()).isPresent()) {
            throw new AppException(ErrorCode.PRICE_ALREADY_EXISTS);
        }
        Price price = priceMapper.toEntity(request);
        price = priceRepository.save(price);
        return priceMapper.toDto(price);
    }

    @Override
    @Transactional
    @CacheEvict(value = "prices", allEntries = true)
    public PriceResponse update(Long id, PriceUpdateRequest request) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRICE_NOT_FOUND));

        priceMapper.update(request, price);
        price = priceRepository.save(price);
        return priceMapper.toDto(price);
    }

    @Override
    @Transactional
    @CacheEvict(value = "prices", allEntries = true)
    public void delete(Long id) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRICE_NOT_FOUND));
        priceRepository.delete(price);
    }

    @Override
    @Cacheable(value = "prices")
    public PriceResponse findById(Long id) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRICE_NOT_FOUND));
        return priceMapper.toDto(price);
    }

    @Override
    @Cacheable(value = "prices")
    public PageResponse<PriceResponse> findAll(int page, int size, String keyword) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<Price> pricePage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            pricePage = priceRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            pricePage = priceRepository.findAll(pageable);
        }

        List<PriceResponse> dtoList = priceMapper.toDtoList(pricePage.getContent());

        return PageResponse.<PriceResponse>builder()
                .currentPage(page)
                .totalPages(pricePage.getTotalPages())
                .pageSize(size)
                .totalElements(pricePage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    @Cacheable(value = "prices")
    public List<PriceResponse> getActivePrices() {
        List<Price> activePrices = priceRepository.findByIsActiveTrue();
        return priceMapper.toDtoList(activePrices);
    }
}
