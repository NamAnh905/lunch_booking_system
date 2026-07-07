package vn.vnpost.lunchorder.core.modules.price.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.vnpost.lunchorder.common.entity.Price;

import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {
    List<Price> findByIsActiveTrue();
    Page<Price> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
