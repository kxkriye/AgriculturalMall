package com.djm.gulimall.cart.service;

import com.djm.gulimall.cart.vo.Cart;
import com.djm.gulimall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author djm
 * @create 2022-02-23 22:13
 */
public interface CartService {
    CartItem getCartItem(Long skuId);

    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    Cart getCart()  throws ExecutionException, InterruptedException;

    void checkItem(Long skuId, Integer checked);

    void changeItemCount(Long skuId, Integer num);

    void deleteIdCartInfo(Integer skuId);

    List<CartItem> getUserCartItems();
}
