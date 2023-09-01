package com.djm.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.djm.common.utils.PageUtils;
import com.djm.gulimall.member.entity.MemberLoginLogEntity;

import java.util.Map;

/**
 * 会员登录记录
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:05:43
 */
public interface MemberLoginLogService extends IService<MemberLoginLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

