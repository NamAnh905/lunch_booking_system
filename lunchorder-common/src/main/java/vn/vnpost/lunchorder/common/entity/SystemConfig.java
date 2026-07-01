package vn.vnpost.lunchorder.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "system_config")
@EntityListeners(AuditingEntityListener.class)
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", length = 100, unique = true, nullable = false)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "description")
    private String description;

    @Column(name = "updated_by")
    @LastModifiedBy
    private Long updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
