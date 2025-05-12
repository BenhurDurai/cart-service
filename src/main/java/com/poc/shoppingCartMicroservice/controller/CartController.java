package com.poc.shoppingCartMicroservice.controller;


import com.poc.dto.CartCheckoutRequest;
import com.poc.shoppingCartMicroservice.dto.CartDto;
import com.poc.shoppingCartMicroservice.model.Cart;
import com.poc.shoppingCartMicroservice.service.CartService;
import com.poc.shoppingCartMicroservice.serviceImpl.CartServiceImpl;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<Cart> addCard(@RequestBody @Valid CartDto cartDto){
        Cart cart = cartService.addToCart(cartDto);
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<Cart>> getCartByUsername(@PathVariable String username){
        List<Cart> cartList = cartService.getCartByUsername(username);
        return ResponseEntity.ok(cartList);
    }

    @DeleteMapping("/remove/id/{cartId}")
    public ResponseEntity<String> removeCartItem(@PathVariable Long cartId){
        cartService.removeCartItem(cartId);
        return ResponseEntity.ok("Cart item removed successfully");
    }

    @DeleteMapping("/remove/user/{username}")
    public ResponseEntity<String> removeCartItemByUsername(@PathVariable String username){
        cartService.removeCartItemByUsername(username);
        return ResponseEntity.ok("Cart item removed successfully");
    }

    @PostMapping("/checkout/{username}")
    public CartCheckoutRequest checkoutCart(@PathVariable String username) {
        log.info("Received checkout request for user: {}", username);
        return cartService.checkoutCart(username);
    }

}
