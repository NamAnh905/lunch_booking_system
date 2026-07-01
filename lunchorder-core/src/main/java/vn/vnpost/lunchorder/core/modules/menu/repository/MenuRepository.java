package vn.vnpost.lunchorder.core.modules.menu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vnpost.lunchorder.common.entity.Menu;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByMenuDate(LocalDate menuDate);
    Optional<Menu> findByMenuDateAndIsSpecial(LocalDate menuDate, Boolean isSpecial);
}
