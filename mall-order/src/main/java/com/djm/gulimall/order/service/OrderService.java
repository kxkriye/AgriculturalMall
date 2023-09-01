package com.djm.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.djm.common.to.mq.SeckillOrderTo;
import com.djm.common.utils.PageUtils;
import com.djm.gulimall.order.entity.OrderEntity;
import com.djm.gulimall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:12:03
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity orderEntity);

    PayVo getOrderPay(String orderSn);

    PageUtils listwithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo asyncVo);

    void createSeckillOrder(SeckillOrderTo orderTo);
}

