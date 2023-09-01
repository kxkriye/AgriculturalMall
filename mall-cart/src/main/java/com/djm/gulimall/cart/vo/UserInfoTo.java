package com.djm.gulimall.cart.vo;

import lombok.Data;

/**
 * @author djm
 * @create 2022-02-23 21:58
 */
@Data
public class UserInfoTo {
    private Long userId;
    private String userKey;
    /**
     * 是否临时用户
     */
    private Boolean tempUser = false;
}
