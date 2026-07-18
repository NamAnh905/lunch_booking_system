package vn.vnpost.lunchorder.core.modules.systemconfig.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.vnpost.lunchorder.core.modules.systemconfig.entity.SystemConfig;

import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
