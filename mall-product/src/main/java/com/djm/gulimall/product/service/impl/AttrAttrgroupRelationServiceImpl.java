package com.djm.gulimall.product.service.impl;

import com.djm.gulimall.product.vo.AttrGroupRelationVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.djm.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.djm.gulimall.product.service.AttrAttrgroupRelationService;


@Service("attrAttrgroupRelationService")
public class AttrAttrgroupRelationServiceImpl extends ServiceImpl<AttrAttrgroupRelationDao, AttrAttrgroupRelationEntity> implements AttrAttrgroupRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrAttrgroupRelationEntity> page = this.page(
                new Query<AttrAttrgroupRelationEntity>().getPage(params),
                new QueryWrapper<AttrAttrgroupRelationEntity>()
        );

        return new PageUtils(page);
    }

        @Override
        public void saveBatch(List<AttrGroupRelationVo> vos) {
            List<AttrAttrgroupRelationEntity> collect = vos.stream().map(item -> {
                AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
                BeanUtils.copyProperties(item, relationEntity);
                return relationEntity;
            }).collect(Collectors.toList());
            this.saveBatch(collect);
        }


}