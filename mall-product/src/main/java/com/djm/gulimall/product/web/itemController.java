package com.djm.gulimall.product.web;

import com.djm.gulimall.product.service.SkuInfoService;
import com.djm.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.acl.LastOwnerException;
import java.util.concurrent.ExecutionException;

/**
 * @author djm
 * @create 2022-02-03 12:49
 */
@Controller
public class itemController {
    @Autowired
    SkuInfoService skuInfoService;
    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model) throws ExecutionException, InterruptedException {
       SkuItemVo skuItemVo = skuInfoService.item(skuId);
       model.addAttribute("item",skuItemVo );
        return "item";
    }
}
