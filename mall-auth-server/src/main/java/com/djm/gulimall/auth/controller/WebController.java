package com.djm.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.djm.common.constant.AuthServerConstant;
import com.djm.common.exception.BizCodeEnume;
import com.djm.common.utils.R;
import com.djm.common.vo.MemberRespVo;
import com.djm.gulimall.auth.feign.ThirdPartFeignService;
import com.djm.gulimall.auth.feign.memberFeignService;
import com.djm.gulimall.auth.utils.HttpUtils;
import com.djm.gulimall.auth.vo.UserLoginVo;
import com.djm.gulimall.auth.vo.UserRegistVo;
import com.djm.gulimall.auth.vo.gitSocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author djm
 * @create 2022-02-07 14:03
 */
@Controller
@Slf4j
public class WebController {
    /**
     * 发送短信验证码
     * @param phone 手机号
     * @return
     */
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ThirdPartFeignService thirdPartFeignService;
    @Autowired
    memberFeignService memberFeignService;
    @GetMapping("/sms/sendCode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) {
        // TODO 1、接口防刷
        // 先从redis中拿取
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)) {
            // 拆分
            long l = Long.parseLong(redisCode.split("_")[1]);
            // 当前系统事件减去之前验证码存入的事件 小于60000毫秒=60秒
            if (System.currentTimeMillis() -l < 60000) {
                // 60秒内不能再发
              return   R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        // 2、验证码的再次效验
        // 数据存入 =》redis key-phone value - code sms:code:131xxxxx - >45678
        String code = UUID.randomUUID().toString().substring(0,5).toUpperCase();
        // 拼接验证码
        String substring = code+"_" + System.currentTimeMillis();
        // redis缓存验证码 防止同一个phone在60秒内发出多次验证吗
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,substring,10, TimeUnit.MINUTES);

        // 调用第三方服务发送验证码
        thirdPartFeignService.sendCode(phone,code);
        return R.ok();
    }
    /**
     * //TODO 重定向携带数据，利用session原理，将数据放在session中，
     * 只要跳转到下一个页面取出这个数据，session中的数据就会删掉
     * //TODO分布式下 session 的问题
     * RedirectAttributes redirectAttributes 重定向携带数据
     * redirectAttributes.addFlashAttribute("errors", errors); 只能取一次
     * @param vo 数据传输对象
     * @param result 用于验证参数
     * @param redirectAttributes 数据重定向
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result,
                         RedirectAttributes redirectAttributes) {
        // 校验是否通过
        if (result.hasErrors()) {
            // 拿到错误信息转换成Map
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            //用一次的属性
            redirectAttributes.addFlashAttribute("errors",errors);
            // 校验出错，转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }

        // 将传递过来的验证码 与 存redis中的验证码进行比较
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(s)) {
            // 验证码和redis中的一致
            if(code.equals(s.split("_")[0])) {
                // 删除验证码：令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                // 调用远程服务，真正注册
                R r = memberFeignService.regist(vo);
                if (r.getCode() == 0) {
                    // 远程调用注册服务成功
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg",r.getData(new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);
                // 校验出错，转发到注册页
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);
            // 校验出错，转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }
@PostMapping("/login")
public String login(UserLoginVo userLoginVo,RedirectAttributes redirectAttributes,HttpSession session){
    R r = memberFeignService.login(userLoginVo);

    if (r.getCode()==0){
        MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
        });
        session.setAttribute("loginUser",data);
        return "redirect:http://gulimall.com";
    }else {
        HashMap<String, String> errors = new HashMap<>();
        errors.put("msg",r.getData("msg", new TypeReference<String>(){}) );
        redirectAttributes.addFlashAttribute("errors", errors);
        return "redirect:http://auth.gulimall.com/login.html";
    }
}
        @GetMapping("login.html")
        public String loginpage(HttpSession session){
        if (session.getAttribute("loginUser")!=null){
            return "redirect:http://gulimall.com";
        }
            return "login";

        }
    /**
     * 回调接口
     * @param code
     * @return
     * @throws Exception
     */
    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(@RequestParam(value = "code",required = false) String code, @RequestParam(value = "error",required = false,defaultValue = "") String error, HttpSession session) throws Exception {
//        access_denied
        if (error.equals("access_denied")){
            return "redirect:http://auth.gulimall.com/login.html";
        }
        // 1、根据code换取accessToken
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "f0029ce2f0a32d1fd207093ff757be21115e75b76ea3913155765a5f74b611f3");
        map.put("client_secret", "68ec36acede0860c48d0ced791cb643052e5cd5878cc3091b81b719d226e13f4");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/gitee/success");
        map.put("code", code);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        HttpResponse response = HttpUtils.doPost("https://gitee.com/oauth/token","","post",headers,null,map);
        System.out.println(response.toString());

        // 状态码为200请求成功
        if (response.getStatusLine().getStatusCode() == 200 ){
            // 获取到了accessToken
            String json = EntityUtils.toString(response.getEntity());
            gitSocialUser socialUser = JSON.parseObject(json, gitSocialUser.class);
            R r = memberFeignService.OAuthlogin(socialUser);
            if (r.getCode() == 0) {
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登录成功:用户:{}",data.toString());
                session.setAttribute("loginUser",data );
                // 2、登录成功跳转到首页
                return "redirect:http://gulimall.com";
            } else {
                // 注册失败
                return "redirect:http://auth.gulimall.com/login.html";
            }
        } else {
            // 请求失败
            // 注册失败
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

}
