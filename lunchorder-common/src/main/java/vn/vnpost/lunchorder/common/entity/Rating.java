package vn.vnpost.lunchorder.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "rating")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Column(name = "score")
    private Integer score;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
