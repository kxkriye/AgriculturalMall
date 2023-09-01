package com.djm.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.djm.common.to.MemberRegistVo;
import com.djm.common.utils.PageUtils;
import com.djm.gulimall.member.entity.MemberEntity;
import com.djm.gulimall.member.entity.MemberLoginVo;
import com.djm.gulimall.member.entity.gitSocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:05:43
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo registVo);
    void checkPhoneUnique(String phone);
    void checkUserNameUnique(String username);

    MemberEntity login(MemberLoginVo memberLoginVo);

    MemberEntity login(gitSocialUser gitSocialUser);
}

