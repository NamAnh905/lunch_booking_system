package vn.vnpost.lunchorder.core.modules.ticketexchange.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.vnpost.lunchorder.common.entity.TicketExchange;
import vn.vnpost.lunchorder.common.enums.TicketExchangeStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TicketExchangeRepository extends JpaRepository<TicketExchange, Long> {
    Page<TicketExchange> findByStatus(TicketExchangeStatus status, Pageable pageable);

    Optional<TicketExchange> findByOrderIdAndStatus(Long orderId, TicketExchangeStatus status);

    List<TicketExchange> findByCreatedAtAfterAndStatus(Instant startDate, TicketExchangeStatus status);

    List<TicketExchange> findByCreatedAtAfter(Instant startDate);

    @EntityGraph(attributePaths = {"order", "order.originalUser", "order.menu", "buyer"})
    @Query("SELECT t FROM TicketExchange t WHERE t.order.user.id = :userId AND t.status = :status")
    List<TicketExchange> findBySellerIdAndStatus(@Param("userId") Long userId, @Param("status") TicketExchangeStatus status);

    @EntityGraph(attributePaths = {"order", "order.originalUser", "order.menu", "buyer"})
    @Query("SELECT t FROM TicketExchange t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN o.originalUser ou " +
           "LEFT JOIN t.buyer b " +
           "WHERE (cast(:startDate as date) IS NULL OR o.orderDate >= :startDate) AND " +
           "(cast(:endDate as date) IS NULL OR o.orderDate <= :endDate) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(cast(:keyword as text) IS NULL OR cast(:keyword as text) = '' OR " +
           "LOWER(ou.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR " +
           "LOWER(ou.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR " +
           "LOWER(b.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR " +
           "LOWER(b.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))) " +
           "ORDER BY o.orderDate DESC, t.id DESC")
    Page<TicketExchange> findForAdmin(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("status") TicketExchangeStatus status,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TicketExchange t where t.id = :id")
    Optional<TicketExchange> findByIdForUpdate(@Param("id") Long id);

    /**
     * Public market listing: only OPEN tickets, excluding the requesting
     * user's own listings. Time-window expiry is enforced separately by
     * {@code TicketExchangeAutoRevertScheduler}, which flips stale OPEN
     * tickets to EXPIRED, so this query does not need to duplicate that logic.
     */
    @EntityGraph(attributePaths = {"order", "order.originalUser", "order.menu", "buyer"})
    @Query("SELECT t FROM TicketExchange t " +
           "LEFT JOIN t.order o " +
           "LEFT JOIN o.originalUser ou " +
           "WHERE t.status = :status AND o.user.id != :currentUserId AND " +
           "(cast(:keyword as text) IS NULL OR cast(:keyword as text) = '' OR " +
           "LOWER(ou.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR " +
           "LOWER(ou.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))) " +
           "ORDER BY o.orderDate ASC, t.id ASC")
    Page<TicketExchange> findOpenForMarket(@Param("status") TicketExchangeStatus status,
                                            @Param("currentUserId") Long currentUserId,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);

    /**
     * Candidates for auto-revert: still OPEN tickets whose menu date has
     * reached or passed. Used to notify original owners before the bulk
     * status update runs.
     */
    @EntityGraph(attributePaths = {"order", "order.user", "order.originalUser"})
    @Query("SELECT t FROM TicketExchange t WHERE t.status = :currentStatus AND t.order.orderDate <= :today")
    List<TicketExchange> findByStatusAndOrderDateLessThanEqual(@Param("currentStatus") TicketExchangeStatus currentStatus,
                                                                 @Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE TicketExchange t SET t.status = :newStatus " +
           "WHERE t.status = :currentStatus AND t.order.orderDate <= :today")
    int updateStatusByOrderDateLessThanEqualAndCurrentStatus(@Param("today") LocalDate today,
                                                               @Param("currentStatus") TicketExchangeStatus currentStatus,
                                                               @Param("newStatus") TicketExchangeStatus newStatus);
}
