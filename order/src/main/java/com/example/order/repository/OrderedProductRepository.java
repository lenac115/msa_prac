package com.example.order.repository;

import com.example.order.domain.OrderedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderedProductRepository extends JpaRepository<OrderedProduct, Long> {

    @Query("select o from OrderedProduct o where o.order.id = :orderId")
    List<OrderedProduct> findAllByOrderId(Long orderId);
}
