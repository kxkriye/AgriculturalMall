package com.djm.gulimall.member.dao;

import com.djm.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:05:43
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
