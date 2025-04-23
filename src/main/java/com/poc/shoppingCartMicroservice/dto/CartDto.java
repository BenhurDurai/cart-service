package com.poc.shoppingCartMicroservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartDto {

    @NotBlank
    private String username;

    @NotBlank
    private String productName;

    @Min(value = 1, message = "Quantity should be atleast 1")
    private int quantity;

}
