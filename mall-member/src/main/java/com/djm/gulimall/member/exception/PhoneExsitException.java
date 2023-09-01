package com.djm.gulimall.member.exception;

/**
 * @author djm
 * @create 2022-02-07 22:30
 */
public class PhoneExsitException extends RuntimeException {
    public PhoneExsitException() {
        super("手机号已存在");
    }
}
