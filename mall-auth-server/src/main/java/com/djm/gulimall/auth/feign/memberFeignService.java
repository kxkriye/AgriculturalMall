package com.djm.gulimall.auth.feign;

import com.djm.common.utils.R;
import com.djm.gulimall.auth.vo.UserLoginVo;
import com.djm.gulimall.auth.vo.UserRegistVo;
import com.djm.gulimall.auth.vo.gitSocialUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid; /**
 * @author djm
 * @create 2022-02-07 21:35
 */
@FeignClient("gulimall-member")
public interface memberFeignService {
    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo registVo);
    @PostMapping("/member/member/login")
    public R login(@RequestBody UserLoginVo memberLoginVo);

    @PostMapping("/member/member/oauth/login")
    public R OAuthlogin(@RequestBody gitSocialUser gitSocialUser);
}
