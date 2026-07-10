package vn.vnpost.lunchorder.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.vnpost.lunchorder.common.entity.SystemConfig;

import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
