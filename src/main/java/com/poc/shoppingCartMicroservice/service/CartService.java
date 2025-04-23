package com.poc.shoppingCartMicroservice.service;


import com.poc.common.dto.CartCheckoutRequest;
import com.poc.shoppingCartMicroservice.dto.CartDto;
import com.poc.shoppingCartMicroservice.model.Cart;

import java.util.List;

public interface CartService {

    Cart addToCart(CartDto cartDto);

    List<Cart> getCartByUsername(String username);

    void removeCartItem(Long cartId);

    CartCheckoutRequest checkoutCart(String username);

    void removeCartItemByUsername(String username);
}
