package com.djm.gulimall.coupon.dao;

import com.djm.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:10:42
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
