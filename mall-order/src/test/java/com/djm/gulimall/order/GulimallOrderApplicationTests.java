package com.djm.gulimall.order;


import com.djm.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallOrderApplicationTests {

	@Autowired
	AmqpAdmin amqpAdmin;
	@Autowired
	RabbitTemplate rabbitTemplate;
	/**
	 * 创建Exchange
	 * 1、如何利用Exchange,Queue,Binding
	 *      1、使用AmqpAdmin进行创建
	 * 2、如何收发信息
	 */
	@Test
	public void sendMessageTest() {
		OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
		reasonEntity.setId(1L);
		reasonEntity.setCreateTime(new Date());
		reasonEntity.setName("reason");
		reasonEntity.setStatus(1);
		reasonEntity.setSort(2);
		String msg = "Hello World";
		//1、发送消息,如果发送的消息是个对象，会使用序列化机制，将对象写出去，对象必须实现Serializable接口

		//2、发送的对象类型的消息，可以是一个json
		for (int i =0;i<10;i++){
		rabbitTemplate.convertAndSend("hello-java.exchange","hello.java",
				reasonEntity,new CorrelationData(UUID.randomUUID().toString()));}
		log.info("消息发送完成:{}",reasonEntity);
	}
	@Test
	public void contextLoads() {
		//	public DirectExchange(
		//	String name, 交换机的名字
		//	boolean durable, 是否持久
		//	boolean autoDelete, 是否自动删除
		//	Map<String, Object> arguments)
		//	{
		DirectExchange directExchange = new DirectExchange("hello-java.exchange",true,false);
		amqpAdmin.declareExchange(directExchange);
		log.info("Exchange[{}]创建成功：","hello-java.exchange");
	}

	/**
	 * 创建队列
	 */
	@Test
	public void createQueue() {
		// public Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments) {
		Queue queue = new Queue("hello-java-queue",true,false,false);
		amqpAdmin.declareQueue(queue);
		log.info("Queue[{}]:","创建成功");
	}


	/**
	 * 绑定队列
	 */
	@Test
	public void createBinding() {
		// public Binding(String destination, 目的地
		// DestinationType destinationType, 目的地类型
		// String exchange,交换机
		// String routingKey,//路由键
		Binding binding = new Binding("hello-java-queue",
				Binding.DestinationType.QUEUE,
				"hello-java.exchange",
				"hello.java",null);
		amqpAdmin.declareBinding(binding);
		log.info("Binding[{}]创建成功","hello-java-binding");
	}
}