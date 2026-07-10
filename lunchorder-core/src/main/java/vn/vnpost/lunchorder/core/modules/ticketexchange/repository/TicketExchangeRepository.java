package vn.vnpost.lunchorder.core.modules.ticketexchange.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.vnpost.lunchorder.common.entity.TicketExchange;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketExchangeRepository extends JpaRepository<TicketExchange, Long> {
    Page<TicketExchange> findByStatus(String status, Pageable pageable);

    Optional<TicketExchange> findByOrderIdAndStatus(Long orderId, String status);

    List<TicketExchange> findByCreatedAtAfterAndStatus(Instant startDate, String status);

    List<TicketExchange> findByCreatedAtAfter(Instant startDate);

    @Query("SELECT t FROM TicketExchange t WHERE t.order.user.id = :userId AND t.status = :status")
    List<TicketExchange> findBySellerIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT t FROM TicketExchange t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN o.originalUser ou " +
           "LEFT JOIN t.buyer b " +
           "WHERE (cast(:startDate as date) IS NULL OR o.orderDate >= :startDate) AND " +
           "(cast(:endDate as date) IS NULL OR o.orderDate <= :endDate) AND " +
           "(cast(:status as text) IS NULL OR cast(:status as text) = '' OR t.status = :status) AND " +
           "(cast(:keyword as text) IS NULL OR cast(:keyword as text) = '' OR " +
           "LOWER(ou.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR " +
           "LOWER(ou.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR " +
           "LOWER(b.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR " +
           "LOWER(b.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))) " +
           "ORDER BY o.orderDate DESC, t.id DESC")
    Page<TicketExchange> findForAdmin(@Param("startDate") java.time.LocalDate startDate, 
                                      @Param("endDate") java.time.LocalDate endDate, 
                                      @Param("status") String status, 
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TicketExchange t where t.id = :id")
    Optional<TicketExchange> findByIdForUpdate(@Param("id") Long id);
}
