package vn.vnpost.lunchorder.core.modules.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vnpost.lunchorder.core.modules.payment.entity.Payment;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

       @Query("SELECT p FROM Payment p JOIN FETCH p.user WHERE " +
                     "(:userId IS NULL OR p.user.id = :userId) AND " +
                     "(:month IS NULL OR p.paymentMonth = :month) AND " +
                     "(:year IS NULL OR p.paymentYear = :year) " +
                     "ORDER BY p.paidAt DESC")
       List<Payment> findByFilters(@Param("userId") Long userId,
                     @Param("month") Integer month,
                     @Param("year") Integer year);

       /**
        * Tổng tiền đã thanh toán của 1 user trong 1 tháng/năm.
        */
       @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
                     "WHERE p.user.id = :userId AND p.paymentMonth = :month AND p.paymentYear = :year")
       BigDecimal sumByUserIdAndMonth(@Param("userId") Long userId,
                     @Param("month") int month,
                     @Param("year") int year);

       /**
        * Tổng tiền đã thanh toán nhóm theo userId cho 1 tháng/năm.
        * Trả về [userId, totalPaid].
        */
       @Query("SELECT p.user.id, SUM(p.amount) FROM Payment p " +
                     "WHERE p.paymentMonth = :month AND p.paymentYear = :year " +
                     "GROUP BY p.user.id")
       List<Object[]> sumGroupByUserForMonth(@Param("month") int month,
                     @Param("year") int year);
}
