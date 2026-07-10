package vn.vnpost.lunchorder.core.modules.ordersummary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vnpost.lunchorder.common.entity.Order;

import java.time.LocalDate;
import java.util.List;

public interface OrderSummaryRepository extends JpaRepository<Order, Long> {

  @Query("""
          SELECT u.id, u.fullName, d.name,
                 SUM(CASE WHEN COALESCE(o.price, :normalPrice) <= :normalPrice THEN 1 ELSE 0 END),
                 SUM(CASE WHEN COALESCE(o.price, :normalPrice) > :normalPrice THEN 1 ELSE 0 END),
                 SUM(COALESCE(o.price, :normalPrice))
          FROM Order o
          JOIN o.user u
          JOIN u.department d
          WHERE o.orderDate = :date
            AND o.status != 'CANCELLED'
            AND (:departmentId IS NULL OR d.id = :departmentId)
          GROUP BY u.id, u.fullName, d.name
          ORDER BY d.name, u.fullName
      """)
  List<Object[]> findDailySummary(@Param("date") LocalDate date,
      @Param("departmentId") Long departmentId,
      @Param("normalPrice") java.math.BigDecimal normalPrice);

  /**
   * Tổng hợp đơn đặt suất ăn theo tháng, nhóm theo user.
   * Chỉ tính đơn có status = PRINTED.
   */
  @Query("""
          SELECT u.id, u.fullName, d.name,
                 SUM(CASE WHEN COALESCE(o.price, :normalPrice) <= :normalPrice THEN 1 ELSE 0 END),
                 SUM(CASE WHEN COALESCE(o.price, :normalPrice) > :normalPrice THEN 1 ELSE 0 END),
                 SUM(COALESCE(o.price, :normalPrice))
          FROM Order o
          JOIN o.user u
          JOIN u.department d
          WHERE EXTRACT(MONTH FROM o.orderDate) = :month
            AND EXTRACT(YEAR FROM o.orderDate) = :year
            AND o.status != 'CANCELLED'
            AND (:departmentId IS NULL OR d.id = :departmentId)
          GROUP BY u.id, u.fullName, d.name
          ORDER BY d.name, u.fullName
      """)
  List<Object[]> findMonthlySummary(@Param("month") int month,
      @Param("year") int year,
      @Param("departmentId") Long departmentId,
      @Param("normalPrice") java.math.BigDecimal normalPrice);

  /**
   * Lấy chi tiết lịch sử đặt cơm của từng user trong tháng.
   * Trả về danh sách: [userId, menuDate, isSpecial]
   */
  @Query("""
          SELECT u.id, o.orderDate, COALESCE(o.price, :normalPrice)
          FROM Order o
          JOIN o.user u
          LEFT JOIN u.department d
          WHERE EXTRACT(MONTH FROM o.orderDate) = :month
            AND EXTRACT(YEAR FROM o.orderDate) = :year
            AND o.status != 'CANCELLED'
            AND (:departmentId IS NULL OR d.id = :departmentId)
      """)
  List<Object[]> findMonthlyOrderDetails(@Param("month") int month,
      @Param("year") int year,
      @Param("departmentId") Long departmentId,
      @Param("normalPrice") java.math.BigDecimal normalPrice);
}
