package com.djm.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.djm.common.to.MemberRegistVo;
import com.djm.gulimall.member.dao.MemberLevelDao;
import com.djm.gulimall.member.entity.MemberLevelEntity;
import com.djm.gulimall.member.entity.MemberLoginVo;
import com.djm.gulimall.member.entity.gitSocialUser;
import com.djm.gulimall.member.exception.PhoneExsitException;
import com.djm.gulimall.member.exception.UserNameExistException;
import com.djm.gulimall.member.utils.HttpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.member.dao.MemberDao;
import com.djm.gulimall.member.entity.MemberEntity;
import com.djm.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
@Autowired
MemberLevelDao memberLevelDao;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo registVo) {
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = new MemberEntity();

        // 设置默认等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(memberLevelEntity.getId());
        entity.setNickname(registVo.getUserName());


        // 检查手机号和用户名是否唯一
        checkPhoneUnique(registVo.getPhone());
        checkUserNameUnique(registVo.getUserName());

        entity.setMobile(registVo.getPhone());
        entity.setUsername(registVo.getUserName());

        //密码要加密存储
//        MD5盐值加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(registVo.getPassword());
        entity.setPassword(encode);

        memberDao.insert(entity);
    }
    @Override
    public void checkPhoneUnique(String phone) throws PhoneExsitException {
        MemberDao memberDao = this.baseMapper;
        Integer mobile = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (mobile > 0) {
            throw new PhoneExsitException();
        }
    }

    @Override
    public void checkUserNameUnique(String username) throws UserNameExistException {
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count > 0) {
            throw new PhoneExsitException();
        }
}

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        // 1、去数据库查询 select * from  ums_member where username=? or mobile =?
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>()
                .eq("username", loginacct).or().
                        eq("mobile", loginacct));
        if (memberDao == null) {
            // 登录失败
            return null;
        } else {
            // 获取数据库的密码
            String passwordDB = memberEntity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            // 和用户密码进行校验
            boolean matches = passwordEncoder.matches(password, passwordDB);
            if(matches) {
                // 密码验证成功 返回对象
                return memberEntity;
            } else {
                return null;
            }
        }
    }

    @Override
    public MemberEntity login(gitSocialUser vo) {
        // 登录和注册合并逻辑
//        {
//            "access_token": "6d8d963d0f0595c69e859126fe1ffbab",
//                "token_type": "bearer",
//                "expires_in": 86400,
//                "refresh_token": "2b2fa4328d83dc3b70b3aeb4345b5e7bb0a142a32b08de975c1391f4bcf2f2c9",
//                "scope": "user_info projects pull_requests issues notes keys hook groups gists enterprises emails",
//                "created_at": 1644502343
//        }
        HashMap<String, String> Map = new HashMap<>();
       Map.put("access_token", vo.getAccess_token());
        Map<String, String> headers = new HashMap<>();
        String uid =null;
        String name =null;
        String email=null;
        try {
            HttpResponse response = HttpUtils.doGet("https://gitee.com/api/v5/user", null, "get", headers, Map);
            if (response.getStatusLine().getStatusCode() == 200){
                // 将返回结果转换成json
                String json = EntityUtils.toString(response.getEntity());
                // 利用fastjson将请求返回的json转换为对象
                JSONObject jsonObject = JSON.parseObject(json);
                // 拿到需要的值
               uid = jsonObject.getString("id");
                email = jsonObject.getString("email");
                name = jsonObject.getString("name");
                System.out.println(uid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        MemberDao memberDao = this.baseMapper;
        // 根据社交用户的uuid查询
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>()
                .eq("social_uid", uid));
        // 能查询到该用户
        if (memberEntity != null ){
            // 更新对应值
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(vo.getAccess_token());
            update.setExpiresIn(vo.getExpires_in());

            memberDao.updateById(update);

            memberEntity.setAccessToken(vo.getAccess_token());
            memberEntity.setExpiresIn(vo.getExpires_in());
            return memberEntity;
        } else {
            // 2、没有查询到当前社交用户对应的记录就需要注册一个
            MemberEntity regist = new MemberEntity();
               regist.setNickname(name);
                regist.setEmail(email);
                regist.setSocialUid(uid);
                regist.setAccessToken(vo.getAccess_token());
                regist.setExpiresIn(vo.getExpires_in());
                regist.setUsername(name);
            // 设置社交用户相关信息
            memberDao.insert(regist);
            return regist;
        }
    }


}