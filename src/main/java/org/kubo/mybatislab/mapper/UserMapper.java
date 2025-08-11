package org.kubo.mybatislab.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.kubo.mybatislab.user.model.User;

import java.util.List;

/**
 * 用户表 Mapper。
 *
 * <p>示例中既可以使用 XML，也可以使用注解。为简洁，这里先用注解演示。</p>
 */
@Mapper
public interface UserMapper {

    @Select("select id, username, email from t_user where id = #{id}")
    User findById(@Param("id") Long id);

    @Select("select id, username, email from t_user order by id")
    List<User> findAll();
}


