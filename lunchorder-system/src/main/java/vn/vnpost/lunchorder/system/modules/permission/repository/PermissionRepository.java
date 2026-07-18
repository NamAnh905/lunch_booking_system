package vn.vnpost.lunchorder.system.modules.permission.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.vnpost.lunchorder.system.modules.permission.entity.Permission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByAction(String action);

    List<Permission> findAllByActionIn(Set<String> actions);

    Page<Permission> findByActionContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String action,
            String description, Pageable pageable);
}
