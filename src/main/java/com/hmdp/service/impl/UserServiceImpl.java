package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;
//import org.slf4j;
import javax.servlet.http.HttpSession;

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
        session.setAttribute("code",code);
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
        Object cacheCode = session.getAttribute("code");
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
        session.setAttribute("user",user);
        return Result.ok();
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
