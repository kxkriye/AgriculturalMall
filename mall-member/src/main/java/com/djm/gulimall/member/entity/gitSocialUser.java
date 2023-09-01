package com.djm.gulimall.member.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author djm
 * @create 2022-02-10 22:14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class gitSocialUser {


        private String access_token;
        private String token_type;
        private String expires_in;
        private String refresh_token;
        private String scope;
        private long created_at;

    }

