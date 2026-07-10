package vn.vnpost.lunchorder.core.modules.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vnpost.lunchorder.common.entity.Order;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.orderDate BETWEEN :fromDate AND :toDate " +
           "AND NOT EXISTS (SELECT 1 FROM TicketExchange te WHERE te.order.id = o.id AND te.status = 'OPEN')")
    List<Order> findByUserIdAndOrderDateBetween(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

       Optional<Order> findByUserIdAndOrderDate(Long userId, LocalDate orderDate);

       @Query("SELECT o FROM Order o LEFT JOIN FETCH o.menu m WHERE " +
                     "(cast(:date as date) IS NULL OR o.orderDate = :date) AND " +
                     "(cast(:status as text) IS NULL OR o.status = :status)")
       List<Order> findByDateAndStatus(@Param("date") LocalDate date, @Param("status") String status);

       @Modifying
       @Query("UPDATE Order o SET o.status = :newStatus, o.updatedAt = CURRENT_TIMESTAMP " +
                     "WHERE o.orderDate = :orderDate AND o.status = :currentStatus")
       int updateStatusByOrderDateAndCurrentStatus(
                     @Param("orderDate") LocalDate orderDate,
                     @Param("currentStatus") String currentStatus,
                     @Param("newStatus") String newStatus);
}
