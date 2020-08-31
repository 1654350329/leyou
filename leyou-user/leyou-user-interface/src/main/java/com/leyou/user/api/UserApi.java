package com.leyou.user.api;

import com.leyou.user.pojo.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


public interface UserApi {
    @PostMapping("query")
    User queryUser(@RequestParam("username")String username, @RequestParam("password")String password);
}
