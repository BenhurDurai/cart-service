package com.poc.shoppingCartMicroservice.dto;

import lombok.Data;

@Data
public class ProductResponse {

    private long id;
    private String productName;
    private String productDescription;
    private double price;
    private int quantity;

}
