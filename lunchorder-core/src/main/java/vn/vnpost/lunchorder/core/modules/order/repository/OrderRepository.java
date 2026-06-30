package vn.vnpost.lunchorder.core.modules.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vnpost.lunchorder.common.entity.Order;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByUserIdAndMenuMenuDateBetween(Long userId, LocalDate fromDate, LocalDate toDate);

    Optional<Order> findByUserIdAndMenuId(Long userId, Long menuId);

    @Query("SELECT o FROM Order o JOIN o.menu m WHERE " +
           "(:date IS NULL OR m.menuDate = :date) AND " +
           "(:status IS NULL OR o.status = :status)")
    List<Order> findByDateAndStatus(@Param("date") LocalDate date, @Param("status") String status);
}
