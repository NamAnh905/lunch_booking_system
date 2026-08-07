package vn.vnpost.lunchorder.core.modules.guestmeal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseEntity;
import vn.vnpost.lunchorder.system.modules.department.entity.Department;
import vn.vnpost.lunchorder.system.modules.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "guest_meal")
public class GuestMeal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id")
    private User requestedBy;

    @Column(name = "meal_date")
    private LocalDate mealDate;

    @Column(name = "normal_quantity")
    private Integer normalQuantity = 0;

    @Column(name = "special_quantity")
    private Integer specialQuantity = 0;

    @Column(name = "normal_unit_price", precision = 10, scale = 2)
    private BigDecimal normalUnitPrice;

    @Column(name = "special_unit_price", precision = 10, scale = 2)
    private BigDecimal specialUnitPrice;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "note", columnDefinition = "text")
    private String note;
}
