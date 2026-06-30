package vn.vnpost.lunchorder.system.modules.department.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vnpost.lunchorder.common.entity.Department;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);

    Optional<Department> findByName(String name);

    List<Department> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);
}
