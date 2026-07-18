package vn.vnpost.lunchorder.core.modules.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.common.enums.TicketSource;
import vn.vnpost.lunchorder.core.modules.menu.entity.Menu;
import vn.vnpost.lunchorder.system.modules.user.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
    name = "\"order\"",
    uniqueConstraints = @UniqueConstraint(name = "unique_user_order_date", columnNames = {"user_id", "order_date"})
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

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_source", length = 20)
    private TicketSource ticketSource;

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
