package vn.vnpost.lunchorder.core.modules.ordersummary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vnpost.lunchorder.core.modules.order.entity.Order;
import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection.DailyMealCount;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection.MonthlyOrderDetail;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection.OrderSummaryRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface OrderSummaryRepository extends JpaRepository<Order, Long> {

  @Query("""
          SELECT u.id AS userId, u.fullName AS fullName, d.name AS departmentName,
                 SUM(CASE WHEN COALESCE(o.price, :normalPrice) <= :normalPrice THEN 1 ELSE 0 END) AS normalMealCount,
                 SUM(CASE WHEN COALESCE(o.price, :normalPrice) > :normalPrice THEN 1 ELSE 0 END) AS specialMealCount,
                 SUM(COALESCE(o.price, :normalPrice)) AS totalAmount
          FROM Order o
          JOIN o.user u
          JOIN u.department d
          WHERE o.orderDate = :date
            AND o.status != OrderStatus.CANCELLED
            AND (:departmentId IS NULL OR d.id = :departmentId)
          GROUP BY u.id, u.fullName, d.name
          ORDER BY d.name, u.fullName
      """)
  List<OrderSummaryRow> findDailySummary(@Param("date") LocalDate date,
      @Param("departmentId") Long departmentId,
      @Param("normalPrice") BigDecimal normalPrice);

  /**
   * Tổng hợp đơn đặt suất ăn theo tháng, nhóm theo user.
   */
  @Query("""
          SELECT u.id AS userId, u.fullName AS fullName, d.name AS departmentName,
                 SUM(CASE WHEN COALESCE(o.price, :normalPrice) <= :normalPrice THEN 1 ELSE 0 END) AS normalMealCount,
                 SUM(CASE WHEN COALESCE(o.price, :normalPrice) > :normalPrice THEN 1 ELSE 0 END) AS specialMealCount,
                 SUM(COALESCE(o.price, :normalPrice)) AS totalAmount
          FROM Order o
          JOIN o.user u
          JOIN u.department d
          WHERE EXTRACT(MONTH FROM o.orderDate) = :month
            AND EXTRACT(YEAR FROM o.orderDate) = :year
            AND o.status != OrderStatus.CANCELLED
            AND (:departmentId IS NULL OR d.id = :departmentId)
          GROUP BY u.id, u.fullName, d.name
          ORDER BY d.name, u.fullName
      """)
  List<OrderSummaryRow> findMonthlySummary(@Param("month") int month,
      @Param("year") int year,
      @Param("departmentId") Long departmentId,
      @Param("normalPrice") BigDecimal normalPrice);

  /**
   * Lấy chi tiết lịch sử đặt cơm của từng user trong tháng để dựng ma trận.
   */
  @Query("""
          SELECT u.id AS userId, o.orderDate AS orderDate, COALESCE(o.price, :normalPrice) AS price
          FROM Order o
          JOIN o.user u
          LEFT JOIN u.department d
          WHERE EXTRACT(MONTH FROM o.orderDate) = :month
            AND EXTRACT(YEAR FROM o.orderDate) = :year
            AND o.status != OrderStatus.CANCELLED
            AND (:departmentId IS NULL OR d.id = :departmentId)
      """)
  List<MonthlyOrderDetail> findMonthlyOrderDetails(@Param("month") int month,
      @Param("year") int year,
      @Param("departmentId") Long departmentId,
      @Param("normalPrice") BigDecimal normalPrice);

  /**
   * Đếm số suất ăn theo từng ngày trong tháng (GROUP BY tại DB thay vì gom trong bộ nhớ).
   */
  @Query("""
          SELECT o.orderDate AS date, COUNT(o) AS totalMeals
          FROM Order o
          JOIN o.user u
          LEFT JOIN u.department d
          WHERE EXTRACT(MONTH FROM o.orderDate) = :month
            AND EXTRACT(YEAR FROM o.orderDate) = :year
            AND o.status != OrderStatus.CANCELLED
            AND (:departmentId IS NULL OR d.id = :departmentId)
          GROUP BY o.orderDate
      """)
  List<DailyMealCount> findMonthlyDailyCounts(@Param("month") int month,
      @Param("year") int year,
      @Param("departmentId") Long departmentId);
}
