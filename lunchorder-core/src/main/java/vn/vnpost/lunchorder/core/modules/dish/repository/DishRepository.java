package vn.vnpost.lunchorder.core.modules.dish.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.vnpost.lunchorder.core.modules.dish.entity.Dish;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DishRepository extends JpaRepository<Dish, Long>, JpaSpecificationExecutor<Dish> {
    Optional<Dish> findByName(String name);

    List<Dish> findByNameContainingIgnoreCase(String name);

    Page<Dish> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
