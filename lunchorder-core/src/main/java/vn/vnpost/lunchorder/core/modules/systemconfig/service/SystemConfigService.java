package vn.vnpost.lunchorder.core.modules.systemconfig.service;

import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.BusinessConfigResponse;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.SystemConfigResponse;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.SystemConfigUpdateRequest;

import java.util.List;

public interface SystemConfigService {
    List<SystemConfigResponse> findAll();

    List<SystemConfigResponse> updateAll(List<SystemConfigUpdateRequest> requests);

    BusinessConfigResponse getBusinessTimes();
}
