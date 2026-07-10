package vn.vnpost.lunchorder.system.modules.department.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.vnpost.lunchorder.common.entity.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);

    Optional<Department> findByName(String name);

    List<Department> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);

    org.springframework.data.domain.Page<Department> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
            String code, String name, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT department_id, count(*) FROM \"user\" WHERE department_id IS NOT NULL GROUP BY department_id", nativeQuery = true)
    List<Object[]> countUsersByDepartment();
}
