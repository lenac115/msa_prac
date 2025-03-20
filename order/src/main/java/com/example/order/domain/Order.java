package com.example.order.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long buyerId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @OneToMany(mappedBy = "order")
    @Builder.Default
    private List<OrderedProduct> orderedProductList = new ArrayList<>();

    public void updateStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        PENDING,
        PAID,
        FAILED,
        CANCELED
    }
}
