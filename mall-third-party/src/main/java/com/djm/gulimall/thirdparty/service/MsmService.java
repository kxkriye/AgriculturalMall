package com.djm.gulimall.thirdparty.service;

public interface MsmService {
    //发送短信的方法
    boolean send(String Code, String phone);
}
