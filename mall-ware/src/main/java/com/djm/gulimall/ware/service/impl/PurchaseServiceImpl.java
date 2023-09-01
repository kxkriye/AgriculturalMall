package com.djm.gulimall.ware.service.impl;

import com.djm.common.constant.WareConstant;
import com.djm.common.utils.R;
import com.djm.gulimall.ware.dao.WareSkuDao;
import com.djm.gulimall.ware.entity.PurchaseDetailEntity;
import com.djm.gulimall.ware.feign.ProductFeignService;
import com.djm.gulimall.ware.service.PurchaseDetailService;
import com.djm.gulimall.ware.service.WareInfoService;
import com.djm.gulimall.ware.service.WareSkuService;
import com.djm.gulimall.ware.vo.MergeVo;
import com.djm.gulimall.ware.vo.PurchaseDoneVo;
import com.djm.gulimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.ware.dao.PurchaseDao;
import com.djm.gulimall.ware.entity.PurchaseEntity;
import com.djm.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {
    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    WareInfoService wareInfoService;
    @Autowired
    WareSkuService wareSkuService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status",0).or().eq("status",1)
        );

        return new PageUtils(page);
    }
    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
            Long purchaseId = mergeVo.getPurchaseId();

            if (purchaseId==null){
                PurchaseEntity purchaseEntity = new PurchaseEntity();
                purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
                purchaseEntity.setCreateTime(new Date());
                purchaseEntity.setUpdateTime(new Date());
                this.save(purchaseEntity);
                purchaseId = purchaseEntity.getId();
            }
            //TODO 确认采购单状态是0,1才可以合并
            List<Long> items = mergeVo.getItems();
             Long finalPurchaseId = purchaseId;

            purchaseDetailService.listByIds(items).forEach(m ->{
                m.setPurchaseId(finalPurchaseId);
                m.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());

                purchaseDetailService.updateById(m);
            });
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    @Override
    public void received(List<Long> ids) {
        //1、确认当前采购单是新建或者已分配状态
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter(item -> {
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item->{
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());

        //2、改变采购单的状态
        this.updateBatchById(collect);
        //3、改变采购项的状态
        collect.forEach((item)->{
            List<PurchaseDetailEntity> entities = purchaseDetailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> detailEntities = entities.stream().map(entity -> {
                entity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return entity;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(detailEntities);
        });
    }
//{
//   id: 123,//采购单id
//   items: [{itemId:1,status:4,reason:""}]//完成/失败的需求详情
//}
    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {
        Long id = doneVo.getId();
        List<PurchaseItemDoneVo> items = doneVo.getItems();
        Boolean flag=true;
        List<PurchaseDetailEntity> purchaseDetailEntities = new ArrayList<>();
        //远程调用
        for (PurchaseItemDoneVo i : items) {
            PurchaseDetailEntity byId = purchaseDetailService.getById(i.getItemId());
            if (i.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                flag=false;
            }else {
                wareSkuService.updateBypurchase(byId.getId());
            }
            byId.setStatus(i.getStatus());
            purchaseDetailEntities.add(byId);
//            R info = productFeignService.info(byId.getSkuId());
        }
        purchaseDetailService.updateBatchById(purchaseDetailEntities);

            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setId(id);
            purchaseEntity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISH.getCode():WareConstant.PurchaseStatusEnum.HASERROR.getCode());
            purchaseEntity.setUpdateTime(new Date());
            this.updateById(purchaseEntity);





    }

}