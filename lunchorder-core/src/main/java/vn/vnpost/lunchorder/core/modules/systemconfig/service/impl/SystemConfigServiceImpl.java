package vn.vnpost.lunchorder.core.modules.systemconfig.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.entity.SystemConfig;
import vn.vnpost.lunchorder.common.repository.SystemConfigRepository;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.SystemConfigService;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.BusinessConfigResponse;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.SystemConfigResponse;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.SystemConfigUpdateRequest;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.mapstruct.SystemConfigMapper;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final SystemConfigMapper systemConfigMapper;
    private final CutOffPolicy cutOffPolicy;

    @Override
    public List<SystemConfigResponse> findAll() {
        List<SystemConfig> configs = systemConfigRepository.findAll(Sort.by("configKey"));
        return systemConfigMapper.toDtoList(configs);
    }

    @Override
    @Transactional
    public List<SystemConfigResponse> updateAll(List<SystemConfigUpdateRequest> requests) {
        List<SystemConfig> updated = requests.stream()
                .map(this::upsert)
                .toList();
        return systemConfigMapper.toDtoList(updated);
    }

    @Override
    public BusinessConfigResponse getBusinessTimes() {
        BusinessConfigResponse response = new BusinessConfigResponse();
        response.setCutOffTime(cutOffPolicy.getCutOffTime().toString());
        response.setTicketLockTime(cutOffPolicy.getTicketLockTime().toString());
        return response;
    }

    private SystemConfig upsert(SystemConfigUpdateRequest request) {
        String configKey = request.getConfigKey().trim();
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElseGet(() -> {
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setConfigKey(configKey);
                    return newConfig;
                });
        config.setConfigValue(request.getConfigValue().trim());
        config.setUpdatedAt(Instant.now());
        return systemConfigRepository.save(config);
    }
}
