package com.djm.gulimall.product.service.impl;

import com.djm.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.djm.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.djm.gulimall.product.entity.AttrEntity;
import com.djm.gulimall.product.service.AttrService;
import com.djm.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.djm.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.product.dao.AttrGroupDao;
import com.djm.gulimall.product.entity.AttrGroupEntity;
import com.djm.gulimall.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {
    @Autowired
    private AttrAttrgroupRelationDao relationDao;
    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        QueryWrapper<AttrGroupEntity> Wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        //select * from pms_attr_group where catelog_id=? and (attr_group_id=key or attr_group_name like %key%)
        if(!StringUtils.isEmpty(key)){
            Wrapper.and(w ->{
                w.eq("attr_group_id",key).or().like("attr_group_name",key);
            });
        }
        if (catelogId==0){
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    Wrapper
            );
            return new PageUtils(page);
        }else {
            Wrapper.eq("catelog_id",catelogId);
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    Wrapper
            );
            return new PageUtils(page);
        }


    }

    @Override
    public void removeByIdsAttr(List<Long> longs) {
        baseMapper.deleteBatchIds(longs);
        for (Long a:longs){
            List<AttrAttrgroupRelationEntity> attr_group_id = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", a));
            List<Long> collect = attr_group_id.stream().map(m -> {
                Long id = m.getId();
                return id;
            }).collect(Collectors.toList());
            relationDao.deleteBatchIds(collect);
        }


    }

    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        //com.atguigu.gulimall.product.vo
        //1、查询分组信息
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        //2、查询所有属性
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map(group -> {
            AttrGroupWithAttrsVo attrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(group,attrsVo);
            List<AttrEntity> attrs = attrService.getRelationAttr(attrsVo.getAttrGroupId());
            attrsVo.setAttrs(attrs);
            return attrsVo;
        }).collect(Collectors.toList());

        return collect;


    }

    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrBySpuId(Long spuId, Long catelogId) {

        return   this.baseMapper.getSpuItem(spuId,catelogId);


    }


}