package vn.vnpost.lunchorder.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
    name = "\"order\"",
    uniqueConstraints = @UniqueConstraint(name = "unique_user_menu", columnNames = {"user_id", "menu_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "ticket_source", length = 10)
    private String ticketSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_user_id")
    private User originalUser;

    @Column(name = "is_printed")
    private Boolean isPrinted = false;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_by")
    @LastModifiedBy
    private Long updatedBy;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
