package vn.vnpost.lunchorder.system.modules.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vnpost.lunchorder.system.modules.audit.entity.AuditLog;

import java.time.OffsetDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(value = """
            SELECT a FROM AuditLog a
            LEFT JOIN FETCH a.user u
            WHERE (:action = '' OR a.action = :action)
              AND (LOWER(COALESCE(u.username, '')) LIKE :keyword
                   OR LOWER(COALESCE(u.fullName, '')) LIKE :keyword
                   OR LOWER(a.action) LIKE :keyword
                   OR LOWER(COALESCE(a.targetEntity, '')) LIKE :keyword)
              AND a.createdAt >= :startDateTime
              AND a.createdAt < :endDateTime
            ORDER BY a.id DESC
            """,
            countQuery = """
            SELECT COUNT(a) FROM AuditLog a
            LEFT JOIN a.user u
            WHERE (:action = '' OR a.action = :action)
              AND (LOWER(COALESCE(u.username, '')) LIKE :keyword
                   OR LOWER(COALESCE(u.fullName, '')) LIKE :keyword
                   OR LOWER(a.action) LIKE :keyword
                   OR LOWER(COALESCE(a.targetEntity, '')) LIKE :keyword)
              AND a.createdAt >= :startDateTime
              AND a.createdAt < :endDateTime
            """)
    Page<AuditLog> search(@Param("keyword") String keyword,
                          @Param("action") String action,
                          @Param("startDateTime") OffsetDateTime startDateTime,
                          @Param("endDateTime") OffsetDateTime endDateTime,
                          Pageable pageable);

    @Query("SELECT DISTINCT a.action FROM AuditLog a ORDER BY a.action")
    List<String> findDistinctActions();
}
