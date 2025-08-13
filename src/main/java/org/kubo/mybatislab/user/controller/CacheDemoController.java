package org.kubo.mybatislab.user.controller;

import org.kubo.mybatislab.mapper.UserMapper;
import org.kubo.mybatislab.user.model.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示 MyBatis 一级/二级缓存的端点。
 */
@RestController
public class CacheDemoController {

    private final UserMapper userMapper;

    public CacheDemoController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 一级缓存演示：在同一事务（同一 SqlSession）内，重复查询相同语句只会发出一次 SQL。
     */
    @GetMapping("/api/cache/l1/{id}")
    @Transactional(readOnly = true)
    public User level1Cache(@PathVariable Long id) {
        User u1 = userMapper.findById(id);
        // 重复相同查询，命中一级缓存（SqlSession 本地缓存）
        User u2 = userMapper.findById(id);
        // 返回第二次查询结果（与第一次相同）
        return u2;
    }

    /**
     * 二级缓存演示：两次请求分别命中不同的 SqlSession。
     * 第一次请求将结果写入二级缓存；第二次请求直接命中缓存，不再发送 SQL。
     */
    @GetMapping("/api/cache/l2/{id}")
    public User level2Cache(@PathVariable Long id) {
        return userMapper.findById(id);
    }

    /**
     * 执行更新以演示缓存失效：更新操作会清空当前命名空间的二级缓存。
     */
    @GetMapping("/api/cache/evict")
    public String evictByUpdate() {
        userMapper.updateOne();
        return "Updated one row and flushed second-level cache for UserMapper namespace";
    }
}


