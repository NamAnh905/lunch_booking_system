package vn.vnpost.lunchorder.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(
    name = "menu",
    uniqueConstraints = @UniqueConstraint(name = "unique_menu_date_special", columnNames = {"menu_date", "is_special"})
)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_date")
    private LocalDate menuDate;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_special")
    private Boolean isSpecial = false;

    @Column(name = "status", length = 20)
    private String status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "menu_dish",
        joinColumns = @JoinColumn(name = "menu_id"),
        inverseJoinColumns = @JoinColumn(name = "dish_id")
    )
    private Set<Dish> dishes;
}
