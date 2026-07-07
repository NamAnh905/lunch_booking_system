package vn.vnpost.lunchorder.core.modules.price.service;

import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceCreateRequest;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceUpdateRequest;

import java.util.List;

public interface PriceService {
    PriceResponse create(PriceCreateRequest request);
    PriceResponse update(Long id, PriceUpdateRequest request);
    void delete(Long id);
    PriceResponse findById(Long id);
    PageResponse<PriceResponse> findAll(int page, int size, String keyword);
    List<PriceResponse> getActivePrices();
}
