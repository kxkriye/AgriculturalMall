package com.djm.gulimall.member.exception;

/**
 * @author djm
 * @create 2022-02-07 22:31
 */
public class UserNameExistException extends RuntimeException {
    public UserNameExistException() {
        super("用户名已存在");
    }
}
