package org.kubo.mybatislab.user.controller;

import org.kubo.mybatislab.mapper.UserMapper;
import org.kubo.mybatislab.user.model.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 简单用户接口，便于触发 MyBatis 查询以观察插件效果。
 */
@RestController
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 按 ID 查询用户。
     */
    @GetMapping("/api/users/{id}")
    public User getById(@PathVariable Long id) {
        return userMapper.findById(id);
    }

    /**
     * 查询全部用户。
     */
    @GetMapping("/api/users")
    public List<User> list() {
        System.out.println("Fetching all users");
        return userMapper.findAll();
    }


    @GetMapping("/api/users/updateAll")
    public String updateAll() {
        userMapper.updateAll();
        return "All users updated to kubo";
    }

    @GetMapping("/api/users/updateOne")
    public String updateOne() {
        userMapper.updateOne();
        return "User with id=1 updated to kubo";
    }

    @GetMapping("/api/users/deleteAll")
    public String deleteAll() {
        userMapper.deleteAll();
        return "All users deleted";
    }
}


