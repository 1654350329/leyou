package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties jwtProperties;

    private final static Logger logger=LoggerFactory.getLogger(AuthService.class);
    public String authentication(String username, String password) {
        User user = this.userClient.queryUser(username, password);
        // 如果查询结果为null，则直接返回null
        if (user == null) {
            return null;
        }
        String token=null;
        try {
            UserInfo userInfo = new UserInfo(user.getId(), user.getUsername());
            token = JwtUtils.generateToken(userInfo, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("token生成错误"+username);
        }
        return token;
    }
}
