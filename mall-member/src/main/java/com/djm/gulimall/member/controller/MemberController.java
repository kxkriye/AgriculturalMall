package com.djm.gulimall.member.controller;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

import com.djm.common.exception.BizCodeEnume;
import com.djm.common.to.MemberRegistVo;
import com.djm.gulimall.member.entity.MemberLoginVo;
import com.djm.gulimall.member.entity.gitSocialUser;
import com.djm.gulimall.member.exception.PhoneExsitException;
import com.djm.gulimall.member.exception.UserNameExistException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.djm.gulimall.member.entity.MemberEntity;
import com.djm.gulimall.member.service.MemberService;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.R;



/**
 * 会员
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:05:43
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;
    /**
     * 注册
     * @param registVo
     * @return
     */
    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo registVo) {
        try {
            memberService.regist(registVo);
        } catch (PhoneExsitException e) {
            // 返回对应的异常信息
            return R.error(BizCodeEnume.PHONE_EXIST_EXCEPTION.getCode(),BizCodeEnume.PHONE_EXIST_EXCEPTION.getMsg());
        } catch (UserNameExistException e) {
            return R.error(BizCodeEnume.USER_EXIST_EXCEPTION.getCode(),BizCodeEnume.USER_EXIST_EXCEPTION.getMsg());
        }
        return R.ok();
    }
@PostMapping("oauth/login")
public R OAuthlogin(@RequestBody gitSocialUser gitSocialUser){
    MemberEntity memberEntity =  memberService.login(gitSocialUser);
    if (memberEntity!=null){
        System.out.println(memberEntity);
        return R.ok().setData(memberEntity);
    }else { return R.error(BizCodeEnume.LOGINCAAT_PASSWORD_EXCEPTION.getCode(),BizCodeEnume.LOGINCAAT_PASSWORD_EXCEPTION.getMsg() );
    }
}

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo memberLoginVo){
        MemberEntity memberEntity = memberService.login(memberLoginVo);
        if (memberEntity!=null){
            return R.ok().setData(memberEntity);
        }else { return R.error(BizCodeEnume.LOGINCAAT_PASSWORD_EXCEPTION.getCode(),BizCodeEnume.LOGINCAAT_PASSWORD_EXCEPTION.getMsg() );
        }

    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
