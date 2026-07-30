package vn.vnpost.lunchorder.core.modules.price.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseEntity;
import vn.vnpost.lunchorder.common.enums.MealType;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "price")
public class Price extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", length = 20, nullable = false)
    private MealType mealType = MealType.NORMAL;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
