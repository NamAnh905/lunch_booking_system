package vn.vnpost.lunchorder.core.modules.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vnpost.lunchorder.core.modules.order.entity.Order;
import vn.vnpost.lunchorder.core.modules.order.service.dto.DepartmentMemberOrderResponse;
import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.common.enums.TicketExchangeStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.menu " +
           "LEFT JOIN FETCH o.user u " +
           "LEFT JOIN FETCH u.department " +
           "LEFT JOIN FETCH o.originalUser " +
           "WHERE u.id = :userId AND o.orderDate BETWEEN :fromDate AND :toDate " +
           "AND NOT EXISTS (SELECT 1 FROM TicketExchange te WHERE te.order.id = o.id AND te.status = :openStatus)")
    List<Order> findByUserIdAndOrderDateBetween(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate, @Param("openStatus") TicketExchangeStatus openStatus);

       Optional<Order> findByUserIdAndOrderDate(Long userId, LocalDate orderDate);

       List<Order> findByUserIdAndOrderDateIn(Long userId, List<LocalDate> orderDates);

       @Query("SELECT COUNT(o) > 0 FROM Order o " +
                     "WHERE o.user.id = :userId AND o.orderDate >= :fromDate AND o.status <> :excludedStatus " +
                     "AND NOT EXISTS (SELECT 1 FROM TicketExchange te WHERE te.order.id = o.id AND te.status = :openStatus)")
       boolean existsActiveOrderNotOnMarket(@Param("userId") Long userId,
                     @Param("fromDate") LocalDate fromDate,
                     @Param("excludedStatus") OrderStatus excludedStatus,
                     @Param("openStatus") TicketExchangeStatus openStatus);

       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.menu " +
                     "LEFT JOIN FETCH o.user u " +
                     "LEFT JOIN FETCH u.department " +
                     "LEFT JOIN FETCH o.originalUser " +
                     "WHERE o.orderDate = :date AND (:status IS NULL OR o.status = :status)")
       List<Order> findByDateAndStatus(@Param("date") LocalDate date, @Param("status") OrderStatus status);

       @Query("SELECT new vn.vnpost.lunchorder.core.modules.order.service.dto.DepartmentMemberOrderResponse(" +
                     "u.fullName, " +
                     "d.name, " +
                     "CASE WHEN o.id IS NOT NULL THEN true ELSE false END, " +
                     "CASE WHEN o.id IS NOT NULL AND o.mealType = MealType.SPECIAL THEN true ELSE false END) " +
                     "FROM User u " +
                     "LEFT JOIN u.department d " +
                     "LEFT JOIN Order o ON o.user = u AND o.orderDate = :today AND o.status <> :cancelled " +
                     "WHERE d.id = :departmentId AND u.isActive = true " +
                     "ORDER BY u.fullName ASC")
       List<DepartmentMemberOrderResponse> findDepartmentMealListByDate(@Param("departmentId") Long departmentId,
                     @Param("today") LocalDate today,
                     @Param("cancelled") OrderStatus cancelled);

       @Modifying
       @Query("UPDATE Order o SET o.status = :newStatus, o.updatedAt = CURRENT_TIMESTAMP " +
                     "WHERE o.orderDate <= :orderDate AND o.status = :currentStatus")
       int updateStatusByOrderDateLessThanEqualAndCurrentStatus(
                     @Param("orderDate") LocalDate orderDate,
                     @Param("currentStatus") OrderStatus currentStatus,
                     @Param("newStatus") OrderStatus newStatus);
}
