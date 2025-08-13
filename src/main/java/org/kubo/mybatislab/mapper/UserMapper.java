package org.kubo.mybatislab.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.cache.decorators.LruCache;
import org.kubo.mybatislab.user.model.User;

import java.util.List;

/**
 * 用户表 Mapper。
 *
 * <p>示例中既可以使用 XML，也可以使用注解。为简洁，这里先用注解演示。</p>
 */
@Mapper
@CacheNamespace(eviction = LruCache.class, size = 512, readWrite = true)
public interface UserMapper {

    @Select("select id, username, email from t_user where id = #{id}")
    @Options(useCache = true, flushCache = Options.FlushCachePolicy.FALSE)
    User findById(@Param("id") Long id);

    @Select("select id, username, email from t_user order by id")
    @Options(useCache = true, flushCache = Options.FlushCachePolicy.FALSE)
    List<User> findAll();


    @Update("update t_user set username = 'kubo'")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    void updateAll();


    @Update("update t_user set username = 'kubo' where id = 1")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    void updateOne();

    @Update("delete from t_user")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    void deleteAll();
}


