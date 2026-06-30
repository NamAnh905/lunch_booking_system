package vn.vnpost.lunchorder.system.modules.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.vnpost.lunchorder.common.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmployeeCode(String employeeCode);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    List<User> findByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCase(String fullName, String employeeCode);
}
