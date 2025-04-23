package com.poc.shoppingCartMicroservice.repository;

import com.poc.shoppingCartMicroservice.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart,Long> {
    List<Cart> findByUsername(String username);

    @Modifying
    @Query("DELETE FROM Cart c WHERE c.username = :username")
    void deleteByUsername(@Param("username") String username);
}
