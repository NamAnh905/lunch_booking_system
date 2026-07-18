package vn.vnpost.lunchorder.system.modules.permission.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "permission")
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", length = 100, unique = true)
    private String action;

    @Column(name = "description")
    private String description;
}
