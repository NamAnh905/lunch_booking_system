package vn.vnpost.lunchorder.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "invalidated_token")
public class InvalidatedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", length = 500, unique = true)
    private String token;

    @Column(name = "expiry_time")
    private Instant expiryTime;
}
