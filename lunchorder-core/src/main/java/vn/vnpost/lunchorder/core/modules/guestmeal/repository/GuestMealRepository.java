package vn.vnpost.lunchorder.core.modules.guestmeal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vnpost.lunchorder.core.modules.guestmeal.entity.GuestMeal;
import vn.vnpost.lunchorder.core.modules.guestmeal.repository.projection.GuestMealDailyCount;
import vn.vnpost.lunchorder.core.modules.guestmeal.repository.projection.GuestMealSummaryRow;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GuestMealRepository extends JpaRepository<GuestMeal, Long> {

    @EntityGraph(attributePaths = { "department", "requestedBy" })
    @Query("SELECT g FROM GuestMeal g WHERE g.id = :id")
    Optional<GuestMeal> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = { "department", "requestedBy" })
    @Query("""
            SELECT g FROM GuestMeal g
            LEFT JOIN g.department d
            LEFT JOIN g.requestedBy u
            WHERE (cast(:startDate as date) IS NULL OR g.mealDate >= :startDate)
              AND (cast(:endDate as date) IS NULL OR g.mealDate <= :endDate)
              AND (:departmentId IS NULL OR d.id = :departmentId)
              AND (:requestedByUserId IS NULL OR u.id = :requestedByUserId)
            ORDER BY g.mealDate DESC, g.id DESC
        """)
    Page<GuestMeal> findForAdmin(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("departmentId") Long departmentId,
            @Param("requestedByUserId") Long requestedByUserId,
            Pageable pageable);

    @Query("""
            SELECT d.id AS departmentId, d.name AS departmentName, u.fullName AS requestedByName,
                   SUM(g.normalQuantity) AS normalMealCount,
                   SUM(g.specialQuantity) AS specialMealCount,
                   SUM(g.totalAmount) AS totalAmount
            FROM GuestMeal g
            JOIN g.department d
            LEFT JOIN g.requestedBy u
            WHERE g.mealDate = :date
              AND (:departmentId IS NULL OR d.id = :departmentId)
            GROUP BY d.id, d.name, u.fullName
            ORDER BY d.name, u.fullName
        """)
    List<GuestMealSummaryRow> findDailySummary(@Param("date") LocalDate date,
            @Param("departmentId") Long departmentId);

    @Query("""
            SELECT d.id AS departmentId, d.name AS departmentName, u.fullName AS requestedByName,
                   SUM(g.normalQuantity) AS normalMealCount,
                   SUM(g.specialQuantity) AS specialMealCount,
                   SUM(g.totalAmount) AS totalAmount
            FROM GuestMeal g
            JOIN g.department d
            LEFT JOIN g.requestedBy u
            WHERE EXTRACT(MONTH FROM g.mealDate) = :month
              AND EXTRACT(YEAR FROM g.mealDate) = :year
              AND (:departmentId IS NULL OR d.id = :departmentId)
            GROUP BY d.id, d.name, u.fullName
            ORDER BY d.name, u.fullName
        """)
    List<GuestMealSummaryRow> findMonthlySummary(@Param("month") int month,
            @Param("year") int year,
            @Param("departmentId") Long departmentId);

    @Query("""
            SELECT g.mealDate AS date, SUM(g.normalQuantity + g.specialQuantity) AS totalMeals
            FROM GuestMeal g
            JOIN g.department d
            WHERE EXTRACT(MONTH FROM g.mealDate) = :month
              AND EXTRACT(YEAR FROM g.mealDate) = :year
              AND (:departmentId IS NULL OR d.id = :departmentId)
            GROUP BY g.mealDate
        """)
    List<GuestMealDailyCount> findMonthlyDailyCounts(@Param("month") int month,
            @Param("year") int year,
            @Param("departmentId") Long departmentId);
}
