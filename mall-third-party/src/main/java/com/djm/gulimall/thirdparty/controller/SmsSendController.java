package com.djm.gulimall.thirdparty.controller;

import com.djm.common.utils.R;
import com.djm.gulimall.thirdparty.service.MsmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author djm
 * @create 2022-02-07 20:42
 */
@RestController
@RequestMapping("/sms")
public class SmsSendController {
    @Autowired
    MsmService msmService;
    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code")String code){
        System.out.println(phone);
        System.out.println(code);
        msmService.send(phone,code);
        return R.ok();
    }
}
