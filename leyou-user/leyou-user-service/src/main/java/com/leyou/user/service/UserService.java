package com.leyou.user.service;

import com.leyou.common.utils.CodecUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    static final String KEY_PREFIX = "user:code:phone:";

    static final Logger logger = LoggerFactory.getLogger(UserService.class);


    public Boolean sendVerifyCode(String phone) {
        // 生成验证码
        String code = NumberUtils.generateCode(6);
        try {
            // 发送短信
            Map<String, String> msg = new HashMap<>();
            msg.put("phone", phone);
            msg.put("code", code);
            this.amqpTemplate.convertAndSend("leyou.sms.exchange", "sms.verify.code", msg);
            // 将code存入redis
            this.redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, 5, TimeUnit.MINUTES);
            return true;
        } catch (Exception e) {
            logger.error("发送短信失败。phone：{}， code：{}", phone, code);
            return false;
        }
    }

    /**
     * 用户注册
     * 检测是否已存在用户名或者电话号码
     * @param data
     * @param type
     * @return
     */
    public Boolean check(String data, Integer type) {
        User user=new User();
        switch (type){
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                return null;
        }

        return this.userMapper.selectCount(user)==0;
    }

    public Boolean registerUser(User user,String code) {
        //判断验证码是否一致
        String redisCode = this.redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if (!StringUtils.equals(redisCode,code)){
            return false;
        }
        //生成盐
        String salt = CodecUtils.generateSalt();
        // 强制设置不能指定的参数为null
        user.setId(null);
        user.setCreated(new Date());
        user.setSalt(salt);
        //对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        Boolean boo= this.userMapper.insertSelective(user)==1;

        //如果注册成功删除redis中的记录
        if (boo){
            this.redisTemplate.delete(KEY_PREFIX + user.getPhone());
        }
        return boo;
    }

    public User queryUser(String username, String password) {
        User record=new User();
        record.setUsername(username);
        //查询
        User user = this.userMapper.selectOne(record);
        //校验用户名
        if (user==null){
            return null;
        }
        //校验密码
        String pass = CodecUtils.md5Hex(password, user.getSalt());
        if (!StringUtils.equals(pass,user.getPassword())){
            return null;
        }
       return user;
    }
}