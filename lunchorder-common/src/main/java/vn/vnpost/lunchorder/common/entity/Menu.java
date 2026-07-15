package vn.vnpost.lunchorder.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseEntity;
import vn.vnpost.lunchorder.common.enums.MenuType;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "menu", uniqueConstraints = @UniqueConstraint(name = "unique_menu_date_price", columnNames = {
        "menu_date", "price_id" }))
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private MenuType type = MenuType.LIST;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "menu_date")
    private LocalDate menuDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_id")
    private Price price;

    @Column(name = "status", length = 20)
    private String status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "menu_dish", joinColumns = @JoinColumn(name = "menu_id"), inverseJoinColumns = @JoinColumn(name = "dish_id"))
    @OrderColumn(name = "display_order")
    private List<Dish> dishes;
}
