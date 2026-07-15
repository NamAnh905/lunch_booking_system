package vn.vnpost.lunchorder.core.modules.menu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vnpost.lunchorder.common.entity.Menu;
import vn.vnpost.lunchorder.common.enums.MenuType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;

public interface MenuRepository extends JpaRepository<Menu, Long> {
       List<Menu> findByMenuDate(LocalDate menuDate);

       Optional<Menu> findByMenuDateAndPrice_Amount(LocalDate menuDate, BigDecimal amount);

       List<Menu> findByMenuDateBetween(LocalDate startDate, LocalDate endDate);

       Optional<Menu> findByMenuDateAndPriceId(LocalDate menuDate, Long priceId);

       Optional<Menu> findByMenuDateAndType(LocalDate menuDate, MenuType type);

       // LEFT JOIN để không loại menu ảnh (price_id = NULL). Dùng m.price.name sẽ tạo INNER JOIN ngầm.
       @Query("SELECT m FROM Menu m LEFT JOIN m.price p WHERE " +
                     "(:keyword IS NULL OR :keyword = '' OR " +
                     "LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(m.status) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "CAST(m.menuDate AS string) LIKE CONCAT('%', :keyword, '%'))")
       Page<Menu> searchMenus(@Param("keyword") String keyword, Pageable pageable);

       @Query("SELECT m FROM Menu m LEFT JOIN m.price p WHERE " +
                     "(:keyword IS NULL OR :keyword = '' OR " +
                     "LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(m.status) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "CAST(m.menuDate AS string) LIKE CONCAT('%', :keyword, '%'))")
       List<Menu> searchMenusList(@Param("keyword") String keyword, Sort sort);
}
