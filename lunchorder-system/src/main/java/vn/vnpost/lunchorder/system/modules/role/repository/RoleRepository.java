package vn.vnpost.lunchorder.system.modules.role.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Sort;
import vn.vnpost.lunchorder.system.modules.role.entity.Role;

import java.util.List;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);

    List<Role> findByCodeIn(Set<String> codes);

    List<Role> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Sort sort);
}
