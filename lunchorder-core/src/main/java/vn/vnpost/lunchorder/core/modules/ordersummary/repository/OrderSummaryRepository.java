package vn.vnpost.lunchorder.core.modules.ordersummary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vnpost.lunchorder.common.entity.Order;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderSummaryRepository extends JpaRepository<Order, Long> {

    /**
     * Tổng hợp đơn đặt suất ăn theo ngày, nhóm theo user.
     * Chỉ tính đơn có status = PRINTED.
     * Trả về: [userId, fullName, employeeCode, departmentName, normalCount, specialCount, totalAmount]
     */
    @Query("""
        SELECT u.id, u.fullName, d.name,
               SUM(CASE WHEN m.isSpecial = false THEN 1 ELSE 0 END),
               SUM(CASE WHEN m.isSpecial = true THEN 1 ELSE 0 END),
               SUM(o.price)
        FROM Order o
        JOIN o.user u
        JOIN u.department d
        JOIN o.menu m
        WHERE m.menuDate = :date
          AND o.status = 'PRINTED'
          AND (:departmentId IS NULL OR d.id = :departmentId)
        GROUP BY u.id, u.fullName, d.name
        ORDER BY d.name, u.fullName
    """)
    List<Object[]> findDailySummary(@Param("date") LocalDate date,
                                     @Param("departmentId") Long departmentId);

    /**
     * Tổng hợp đơn đặt suất ăn theo tháng, nhóm theo user.
     * Chỉ tính đơn có status = PRINTED.
     */
    @Query("""
        SELECT u.id, u.fullName, d.name,
               SUM(CASE WHEN m.isSpecial = false THEN 1 ELSE 0 END),
               SUM(CASE WHEN m.isSpecial = true THEN 1 ELSE 0 END),
               SUM(o.price)
        FROM Order o
        JOIN o.user u
        JOIN u.department d
        JOIN o.menu m
        WHERE EXTRACT(MONTH FROM m.menuDate) = :month
          AND EXTRACT(YEAR FROM m.menuDate) = :year
          AND o.status = 'PRINTED'
          AND (:departmentId IS NULL OR d.id = :departmentId)
        GROUP BY u.id, u.fullName, d.name
        ORDER BY d.name, u.fullName
    """)
    List<Object[]> findMonthlySummary(@Param("month") int month,
                                       @Param("year") int year,
                                       @Param("departmentId") Long departmentId);

    /**
     * Lấy chi tiết lịch sử đặt cơm của từng user trong tháng.
     * Trả về danh sách: [userId, menuDate, isSpecial]
     */
    @Query("""
        SELECT u.id, m.menuDate, m.isSpecial
        FROM Order o
        JOIN o.user u
        JOIN o.menu m
        LEFT JOIN u.department d
        WHERE EXTRACT(MONTH FROM m.menuDate) = :month
          AND EXTRACT(YEAR FROM m.menuDate) = :year
          AND o.status = 'PRINTED'
          AND (:departmentId IS NULL OR d.id = :departmentId)
    """)
    List<Object[]> findMonthlyOrderDetails(@Param("month") int month,
                                           @Param("year") int year,
                                           @Param("departmentId") Long departmentId);
}
