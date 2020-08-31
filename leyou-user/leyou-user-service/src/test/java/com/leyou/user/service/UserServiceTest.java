package com.leyou.user.service;

import com.leyou.common.utils.CodecUtils;
import com.leyou.user.LeyouUserServiceApplication;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LeyouUserServiceApplication.class)
public class UserServiceTest {
    @Autowired
    private UserMapper userMapper;

    @Test
    public void registerUser() {
        User user=new User();

        //生成盐
        String salt = CodecUtils.generateSalt();
        // 强制设置不能指定的参数为null
        user.setId(null);
        user.setCreated(new Date());
        user.setPhone("12323");
        user.setUsername("46546");
        user.setPassword("46456456");
        user.setSalt(salt);
        //对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        this.userMapper.insertSelective(user);



    }

    @Test
    public void password(){
        User user=new User();
        user.setUsername("46546");
        User user1 = this.userMapper.selectOne(user);
        String pp = CodecUtils.md5Hex(user1.getPassword(), user1.getSalt());
        System.out.println(pp);
    }
}