package com.djm.gulimall.coupon.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.djm.gulimall.coupon.entity.SeckillSessionEntity;
import com.djm.gulimall.coupon.service.SeckillSessionService;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.R;



/**
 * 秒杀活动场次
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:10:42
 */
@RestController
@RequestMapping("coupon/seckillsession")
public class SeckillSessionController {
    @Autowired
    private SeckillSessionService seckillSessionService;
    /**
     * 查询最近三天需要参加秒杀商品的信息
     * @return
     */
    @GetMapping(value = "/Lates3DaySession")
    public R getLates3DaySession() {

        List<SeckillSessionEntity> seckillSessionEntities = seckillSessionService.getLates3DaySession();
        System.out.println(seckillSessionEntities);
        return R.ok().setData(seckillSessionEntities);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("coupon:seckillsession:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = seckillSessionService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("coupon:seckillsession:info")
    public R info(@PathVariable("id") Long id){
		SeckillSessionEntity seckillSession = seckillSessionService.getById(id);

        return R.ok().put("seckillSession", seckillSession);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("coupon:seckillsession:save")
    public R save(@RequestBody SeckillSessionEntity seckillSession){
		seckillSessionService.save(seckillSession);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("coupon:seckillsession:update")
    public R update(@RequestBody SeckillSessionEntity seckillSession){
		seckillSessionService.updateById(seckillSession);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("coupon:seckillsession:delete")
    public R delete(@RequestBody Long[] ids){
		seckillSessionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
