package com.djm.gulimall.order.dao;

import com.djm.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:12:03
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
