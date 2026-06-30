package vn.vnpost.lunchorder.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
