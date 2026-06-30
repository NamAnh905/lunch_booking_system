package vn.vnpost.lunchorder.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseEntity;

import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "\"user\"")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 50, unique = true)
    private String username;

    @Column(name = "employee_code", length = 50, unique = true)
    private String employeeCode;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}
