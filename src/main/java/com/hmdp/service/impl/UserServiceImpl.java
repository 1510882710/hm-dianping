package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
//import org.slf4j;
import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

//    改用redis的添加内容
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sendcode(String phone, HttpSession session) {
//       1,校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            //2，不符合规范，重新校验手号
            return Result.fail("手机号格式错误！");
        }
//        3，符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
//        4，保存验证码到session
//        session.setAttribute("code",code);
//        改用redis的添加内容,,login:code:定义为常量
        //        4，保存验证码到redis,加业务前缀区分key   set key value ex 120
//stringRedisTemplate.opsForValue().set("login:code:" + phone,code,2, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

//        5,发送验证码
//        log.debug("发送验证码成功，验证码: {}",code);
        log.debug(code);
//6，返回成功标识
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
//  1,验证手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            //2，不符合规范，重新校验手号
            return Result.fail("手机号格式错误！");
        }
//        2，验证验证码
//        Object cacheCode = session.getAttribute("code");
//        从redis获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);


        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.toString().equals(code)){
//            验证码不一致，报错
            return Result.fail("验证码错误");
        }
//        3，查询手机用户
        User user = query().eq("phone", phone).one();
//        4，用户不存在，创建
        if (user ==null){
            user = createUserWithPhone(phone);
        }
//        5，保存到session
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
//        session.setAttribute("user", user);

//      6  保存到redis
//        6.1随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();
//        6.2将user对象转为hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue) ->fieldValue.toString()));
//        6.3存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
//        7,返回客户端
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
//        1,创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
//    2,保存用户
        save(user);
        return user;
    }

}
