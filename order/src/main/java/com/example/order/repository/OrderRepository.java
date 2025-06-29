package com.example.order.repository;

import com.example.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select o from Order o where o.orderEventId = :orderEventId")
    Optional<Order> findByOrderEventId(String orderEventId);

    @Query("select o from Order o where o.buyerId = :buyerId")
    List<Order> findAllByBuyerId(Long buyerId);
}
