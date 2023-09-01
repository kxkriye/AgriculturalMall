package com.djm.gulimall.order.controller;

import java.util.Arrays;
import java.util.Map;

import org.apache.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.djm.gulimall.order.entity.OrderEntity;
import com.djm.gulimall.order.service.OrderService;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.R;

import javax.servlet.http.HttpServletRequest;


/**
 * 订单
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:12:03
 */
@RestController
@RequestMapping("order/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @PostMapping("listWithItem")
    public R listWithItem(@RequestBody Map<String, Object> params){
        PageUtils pageUtils = orderService.listwithItem(params);
        return R.ok().put("page", pageUtils);
    }
    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("gulimall:gulimall:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = orderService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("gulimall:gulimall:info")
    public R info(@PathVariable("id") Long id){
		OrderEntity order = orderService.getById(id);

        return R.ok().put("order", order);
    }
    /**
     * 根据订单编号查询订单状态
     * @param orderSn
     * @return
     */
    @GetMapping(value = "/status/{orderSn}")
    public R getOrderStatus(@PathVariable("orderSn") String orderSn) {
        OrderEntity orderEntity = orderService.getOrderByOrderSn(orderSn);
        return R.ok().setData(orderEntity);
    }
    @GetMapping(value = "/status/payed/{orderSn}")
    public R getOrderStatus123(@PathVariable("orderSn") String orderSn) {
        OrderEntity orderEntity = orderService.getOrderByOrderSn(orderSn);
        return R.ok().setData(orderEntity);
    }
    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("gulimall:gulimall:save")
    public R save(@RequestBody OrderEntity order){
		orderService.save(order);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("gulimall:gulimall:update")
    public R update(@RequestBody OrderEntity order){
		orderService.updateById(order);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("gulimall:gulimall:delete")
    public R delete(@RequestBody Long[] ids){
		orderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
