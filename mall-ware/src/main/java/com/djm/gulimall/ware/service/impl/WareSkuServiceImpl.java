package com.djm.gulimall.ware.service.impl;


import com.alibaba.fastjson.TypeReference;
import com.djm.common.exception.NoStockException;
import com.djm.common.to.OrderTo;
import com.djm.common.to.mq.StockDetailTo;
import com.djm.common.to.mq.StockLockedTo;
import com.djm.common.utils.R;
import com.djm.gulimall.ware.entity.PurchaseDetailEntity;
import com.djm.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.djm.gulimall.ware.entity.WareOrderTaskEntity;
import com.djm.gulimall.ware.feign.OrderFeignService;
import com.djm.gulimall.ware.feign.ProductFeignService;
import com.djm.gulimall.ware.service.PurchaseDetailService;
import com.djm.gulimall.ware.service.WareOrderTaskDetailService;
import com.djm.gulimall.ware.service.WareOrderTaskService;
import com.djm.gulimall.ware.vo.OrderItemVo;
import com.djm.gulimall.ware.vo.OrderVo;
import com.djm.gulimall.ware.vo.SkuHasStockVo;
import com.djm.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.ware.dao.WareSkuDao;
import com.djm.gulimall.ware.entity.WareSkuEntity;
import com.djm.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;
    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;
    @Autowired
    WareOrderTaskService wareOrderTaskService;
    @Autowired
    private OrderFeignService orderFeignService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        /**
         * skuId: 1
         * wareId: 2
         */
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            queryWrapper.eq("sku_id",skuId);
        }

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            queryWrapper.eq("ware_id",wareId);
        }


        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }
    @Transactional
    @Override
    public void updateBypurchase(Long id) {
        PurchaseDetailEntity byId = purchaseDetailService.getById(id);
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", byId.getSkuId()).eq("ware_id", byId.getWareId()));

        if(entities == null || entities.size() == 0){
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            //TODO 远程查询sku的名字，如果失败，整个事务无需回滚
            //1、自己catch异常
            //TODO 还可以用什么办法让异常出现以后不回滚？高级
//            try {
//                R info = productFeignService.info(byId.getSkuId());
            //JSON.parseObject(String str)是将str转化为相应的JSONObject对象
//                SkuInfoEntity data = (SkuInfoEntity) JSONObject.toJavaObject(JSON.parseObject(JSONObject.toJSONString(info.get("skuInfo"))),SkuInfoEntity.class);
//                if(info.getCode() == 0){
//                    System.out.println(data.getSkuName()+"1111111111111111112");
////                    wareSkuEntity.setSkuName((String) data.get("skuName"));
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//            }
            try {
                R info = productFeignService.info(byId.getSkuId());
                Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");

                if(info.getCode() == 0){
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            }catch (Exception e){

            }

            wareSkuEntity.setSkuId(byId.getSkuId());
            wareSkuEntity.setStock(byId.getSkuNum());
            wareSkuEntity.setStockLocked(0);
            wareSkuEntity.setWareId(byId.getWareId());
            this.save(wareSkuEntity);
        }else {
            wareSkuDao.addStock(byId.getSkuId(),byId.getWareId(),byId.getSkuNum());
        }
    }

    @Override
    public List<SkuHasStockVo> getSkusStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            // 查询当前 sku 的总库存良
            // SELECT SUM(stock-stock_locked) FROM `wms_ware_sku` WHERE sku_id = 1
            Long count = baseMapper.getSkuStock(skuId);
            skuHasStockVo.setSkuId(skuId);
            skuHasStockVo.setHasStock(count == null?false:count>0);
            return skuHasStockVo;
        }).collect(Collectors.toList());

   return collect;
    }
    /**
     * 为某个订单锁定库存
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean lockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单详情信息
         * 追溯
         */
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskEntity.setCreateTime(new Date());
        wareOrderTaskService.save(wareOrderTaskEntity);

        //1、按照下单的收货地址，找到一个就近仓库，锁定库存
        //2、找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(m -> {
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            Long skuId = m.getSkuId();
            skuWareHasStock.setNum(m.getCount());
            skuWareHasStock.setSkuId(skuId);
            //查询这个商品在哪个仓库有库存
            List<Long> wareIdList = wareSkuDao.listWareIdHasSkuStock(skuId);
            skuWareHasStock.setWareId(wareIdList);
            return skuWareHasStock;
        }).collect(Collectors.toList());
        for (SkuWareHasStock skuWareHasStock : collect) {
            boolean skuStocked = false;
            List<Long> wareIds = skuWareHasStock.getWareId();
            Long skuId = skuWareHasStock.getSkuId();
            if (org.springframework.util.StringUtils.isEmpty(wareIds)) {
                //没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }

            for (Long aLong : wareIds) {
                Long count = this.baseMapper.lockSkuStock(skuId, aLong, skuWareHasStock.getNum());
                if (count==1){
                    skuStocked = true;
                    WareOrderTaskDetailEntity taskDetailEntity = WareOrderTaskDetailEntity.builder()
                            .skuId(skuId)
                            .skuName("")
                            .skuNum(skuWareHasStock.getNum())
                            .taskId(wareOrderTaskEntity.getId())
                            .wareId(aLong)
                            .lockStatus(1)
                            .build();
                    wareOrderTaskDetailService.save(taskDetailEntity);

                    //TODO 告诉MQ库存锁定成功
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity,detailTo);
                    lockedTo.setDetailTo(detailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",lockedTo);
                    break;
                }
                else {
                //当前仓库锁失败，重试下一个仓库
                }
            }
            if (skuStocked == false) {
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }
        //3、肯定全部都是锁定成功的
        return true;

    }

    @Override
    public void unlockStock(StockLockedTo to) {
        /**
         * 解锁
         * 1、查询数据库关于这个订单锁定库存信息
         *   有：证明库存锁定成功了
         *      解锁：订单状况
         *          1、没有这个订单，必须解锁库存
         *          2、有这个订单，不一定解锁库存
         *              订单状态：已取消：解锁库存
         *                      已支付：不能解锁库存
         */
        WareOrderTaskEntity byId = wareOrderTaskService.getById(to.getId());
        StockDetailTo detail = to.getDetailTo();
        if (byId!=null) {
            String orderSn = byId.getOrderSn();
            R orderData = orderFeignService.getOrderStatus(orderSn);
            if (orderData.getCode() == 0) {
                //订单数据返回成功
                OrderVo orderInfo = orderData.getData("data", new TypeReference<OrderVo>() {});
                //判断订单状态是否已取消或者支付或者订单不存在
                if (orderInfo == null || orderInfo.getStatus() == 4) {
                    //订单已被取消，才能解锁库存
                    if (detail.getLockStatus() == 1) {
                        //当前库存工作单详情状态1，已锁定，但是未解锁才可以解锁
                        unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),detail.getId());
                    }
                }
            } else {
                //消息拒绝以后重新放在队列里面，让别人继续消费解锁
                //远程调用服务失败
                throw new RuntimeException("远程调用服务失败");
            }
        } else {
            //无需解锁
        }
        }
//
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查一下最新的库存解锁状态，防止重复解锁库存
        WareOrderTaskEntity orderTaskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);

        //按照工作单的id找到所有 没有解锁的库存，进行解锁
        //害怕刚刚关单就支付完成，保证幂等性，库存服务也要做相应的验证，也可以不做
        Long id = orderTaskEntity.getId();
//        R orderData = orderFeignService.getOrderStatus(orderSn);
//        if (orderData.getCode() == 0) {
//            //订单数据返回成功
//            OrderVo orderInfo = orderData.getData("data", new TypeReference<OrderVo>() {});
            //判断订单状态是否已取消或者支付或者订单不存在
//            if (orderInfo == null || orderInfo.getStatus() == 4) {
        List<WareOrderTaskDetailEntity> list = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", id).eq("lock_status", 1));


        for (WareOrderTaskDetailEntity taskDetailEntity : list) {
            unLockStock(taskDetailEntity.getSkuId(),
                    taskDetailEntity.getWareId(),
                    taskDetailEntity.getSkuNum(),
                    taskDetailEntity.getId());
        }
//      }
    }
//    }

    private void unLockStock(Long skuId, Long wareId, Integer skuNum, Long id) {
        //库存解锁
        wareSkuDao.unLockStock(skuId,wareId,skuNum);

        //更新工作单的状态
        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
        taskDetailEntity.setId(id);
        //变为已解锁
        taskDetailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(taskDetailEntity);
    }



    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}