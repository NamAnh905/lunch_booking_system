package vn.vnpost.lunchorder.system.modules.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.vnpost.lunchorder.system.modules.user.entity.User;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = "department")
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "department")
    Page<User> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "department")
    List<User> findAll(Sort sort);

    @EntityGraph(attributePaths = "department")
    List<User> findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String fullName, String username);

    @EntityGraph(attributePaths = "department")
    Page<User> findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String fullName, String username, Pageable pageable);
}
