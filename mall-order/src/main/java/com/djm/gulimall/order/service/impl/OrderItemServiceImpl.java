package com.djm.gulimall.order.service.impl;

import com.djm.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.order.dao.OrderItemDao;
import com.djm.gulimall.order.entity.OrderItemEntity;
import com.djm.gulimall.order.service.OrderItemService;


@Service("orderItemService")
//@RabbitListener(queues = "hello-java-queue")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }
//    @RabbitHandler
//public void asd(OrderReturnReasonEntity orderReturnReasonEntity, Message message, Channel channel) throws IOException {
//        long deliveryTag = message.getMessageProperties().getDeliveryTag();
//        System.out.println(orderReturnReasonEntity);
//        System.out.println("asdasdas"+deliveryTag);
////        channel.basicAck(deliveryTag,false );
//    }
}