package com.djm.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.djm.common.utils.PageUtils;
import com.djm.gulimall.order.entity.OrderOperateHistoryEntity;

import java.util.Map;

/**
 * 订单操作历史记录
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:12:03
 */
public interface OrderOperateHistoryService extends IService<OrderOperateHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

