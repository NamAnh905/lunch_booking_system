package vn.vnpost.lunchorder.core.modules.ticketexchange.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vnpost.lunchorder.common.entity.TicketExchange;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketExchangeRepository extends JpaRepository<TicketExchange, Long> {
    Page<TicketExchange> findByStatus(String status, Pageable pageable);
    Optional<TicketExchange> findByOrderIdAndStatus(Long orderId, String status);
    List<TicketExchange> findByCreatedAtAfterAndStatus(Instant startDate, String status);
    List<TicketExchange> findByCreatedAtAfter(Instant startDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TicketExchange t where t.id = :id")
    Optional<TicketExchange> findByIdForUpdate(@Param("id") Long id);
}
