package org.kubo.mybatislab.user.model;

/**
 * 用户实体。
 *
 * <p>为简化演示，字段较少。结合 MyBatis 的下划线转驼峰配置，
 * 数据表字段可使用下划线命名。</p>
 */
public class User {
    private Long id;
    private String username;
    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}


