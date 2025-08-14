package org.kubo.mybatislab.user.controller;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.kubo.mybatislab.mapper.UserMapper;
import org.kubo.mybatislab.user.model.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 简单用户接口，便于触发 MyBatis 查询以观察插件效果。
 */
@RestController
public class UserController {

    private final UserMapper userMapper;
    private final SqlSessionFactory sqlSessionFactory;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition(); ;


    public UserController(UserMapper userMapper, SqlSessionFactory sqlSessionFactory) {
        this.userMapper = userMapper;
        this.sqlSessionFactory = sqlSessionFactory;
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

    @GetMapping("/api/users/cache/l1")
    public String level1Cache() {
        // 开启 session，并在同一个 SqlSession 中重复查询以命中一级缓存
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            mapper.findAll(); // 第一次查询，发送 SQL
            mapper.findAll(); // 第二次相同查询，命中一级缓存（不再发 SQL）
            return "level1Cache";
        }
    }

    @GetMapping("/api/users/cache/l1/t")
    @Transactional
    public String level1CacheT() throws InterruptedException {
        // 开启 session，并在同一个 SqlSession 中重复查询以命中一级缓存
        List<User> all = userMapper.findAll();// 第一次查询，发送 SQL
        System.out.println("第一次查询 = " + all);
        lock.lock();
        condition.await();
        lock.unlock();
        List<User> all1 = userMapper.findAll();// 第二次相同查询，命中一级缓存（不再发 SQL）
        System.out.println("第二次查询 = " + all1);
        return "level1CacheT" ;
    }



    @GetMapping("/api/users/cache/l1/t/notify")
    public String level1CacheTN() {
        lock.lock();
        condition.signal();
        lock.unlock();
        return "唤醒成功" ;
    }
}


