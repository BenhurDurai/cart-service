package com.poc.shoppingCartMicroservice.model;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "cart")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long Id;

    private String username;

    private String productName;

    private int quantity;

    private double price;

}
