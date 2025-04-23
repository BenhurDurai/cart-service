package com.poc.shoppingCartMicroservice.serviceImpl;

import com.poc.common.dto.CartCheckoutRequest;
import com.poc.common.dto.CartItemDto;
import com.poc.shoppingCartMicroservice.dto.*;
import com.poc.shoppingCartMicroservice.exception.ProductNotFoundException;
import com.poc.shoppingCartMicroservice.exception.ProductOutOfStockException;
import com.poc.shoppingCartMicroservice.exception.UserNotFoundException;
import com.poc.shoppingCartMicroservice.kafka.CartEventProducer;
import com.poc.shoppingCartMicroservice.model.Cart;
import com.poc.shoppingCartMicroservice.repository.CartRepository;
import com.poc.shoppingCartMicroservice.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private CartRepository cartRepository;
    private RestTemplate restTemplate;

    @Autowired
    private CartEventProducer cartEventProducer;


    @Autowired
    public CartServiceImpl(CartRepository cartRepository, RestTemplate restTemplate){
        this.cartRepository=cartRepository;
        this.restTemplate=restTemplate;
    }

    @Override
    public Cart addToCart(CartDto cartDto) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(getCurrentRequestToken());
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);

        // Validate user and product existence
        validateUser(cartDto.getUsername(), entity);
        ProductResponse productResponse = validateProduct(cartDto.getProductName(), entity);

        // Check if product already exists in user's cart
        Optional<Cart> existingCartOpt = cartRepository.findByUsername(cartDto.getUsername())
                .stream()
                .filter(c -> c.getProductName().equalsIgnoreCase(cartDto.getProductName()))
                .findFirst();

        Cart cart;
        if (existingCartOpt.isPresent()) {
            // Update quantity
            cart = existingCartOpt.get();
            int updatedQuantity = cart.getQuantity() + cartDto.getQuantity();

            if (updatedQuantity > productResponse.getQuantity()) {
                throw new ProductOutOfStockException("Only " + (productResponse.getQuantity() - cart.getQuantity()) +
                        " more items can be added to cart for product: " + cartDto.getProductName());
            }

            cart.setQuantity(updatedQuantity);
            cart.setPrice(productResponse.getPrice()); // Update price if changed
            log.info("Updated existing cart item: {}", cart);
        } else {
            // New cart item
            cart = new Cart();
            cart.setUsername(cartDto.getUsername());
            cart.setProductName(cartDto.getProductName());
            cart.setQuantity(cartDto.getQuantity());
            cart.setPrice(productResponse.getPrice());
            log.info("Added new cart item: {}", cart);
        }

        return cartRepository.save(cart);
    }

    private String getCurrentRequestToken() {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null){
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")){
                return authHeader.substring(7);
            }
        }
        throw new RuntimeException("Authorization token is missing or invalid");
    }

    @Override
    public List<Cart> getCartByUsername(String username) {
        return cartRepository.findByUsername(username);
    }

    @Override
    public void removeCartItem(Long cartId) {
        cartRepository.deleteById(cartId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeCartItemByUsername(String username) {
        cartRepository.deleteByUsername(username);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "userServiceFallback")
    private void validateUser(String username, HttpEntity<String> entity) {
        String userServiceUrl = "http://localhost:8080/api/users/" + username;
        ResponseEntity<User> userResponse = restTemplate.exchange(userServiceUrl, HttpMethod.GET, entity, User.class);
        if (userResponse.getBody() == null) {
            throw new UserNotFoundException("User not found: " + username);
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "productServiceFallback")
    private ProductResponse validateProduct(String productName, HttpEntity<String> entity) {
        String productServiceUrl = "http://localhost:8081/api/products/" + productName;
        ResponseEntity<ProductResponse> productResponse = restTemplate.exchange(productServiceUrl, HttpMethod.GET, entity, ProductResponse.class);
        if (productResponse.getBody() == null) {
            throw new ProductNotFoundException("Product not found: " + productName);
        }
        return productResponse.getBody();
    }

    private ProductResponse productServiceFallback(String productName, HttpEntity<String> entity, Throwable throwable) {
        log.error("Product service is down. Fallback executed for product: {}", productName);
        throw new ProductNotFoundException("Product Service unavailable. Please try again later.");
    }

    private void userServiceFallback(String username, HttpEntity<String> entity, Throwable throwable) {
        log.error("User service is down. Fallback executed for user: {}", username);
        throw new UserNotFoundException("User Service unavailable. Please try again later.");
    }

    @Override
    public CartCheckoutRequest checkoutCart(String username) {
        List<Cart> cartItems = cartRepository.findByUsername(username);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("No items in cart to checkout for user: " + username);
        }

        CartCheckoutRequest checkoutRequest = new CartCheckoutRequest();
        checkoutRequest.setUsername(username);

        List<CartItemDto> cartItemDtos = cartItems.stream()
                .map(cart -> new CartItemDto(
                        cart.getProductName(),
                        cart.getQuantity(),
                        cart.getPrice()
                ))
                .toList();

        double totalPrice = cartItemDtos.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        checkoutRequest.setCartItems(cartItemDtos);
        checkoutRequest.setTotalPrice(totalPrice);

        // 🔥 Send to Kafka after preparing
        cartEventProducer.sendOrder(checkoutRequest);

        log.info("Checkout cart prepared and sent to Kafka: {}", checkoutRequest);

        return checkoutRequest;
    }


}
