package com.example.mapper;

import com.example.model.UserModel;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.StatementType;

/**
 * Created by guanxinquan on 15-3-9.
 */
public interface UserMapper {

    @Insert("insert into user (name,password) values (#{name},#{password}")
    @SelectKey(before = false, keyProperty = "id", resultType = Long.class, statementType = StatementType.STATEMENT, statement = "SELECT LAST_INSERT_ID() AS id")
    public Integer createUser(UserModel user);

    @Select("select id,name,password from user where name = #{name}")
    public UserModel selectUserByName(String name);

    @Update("update user set password = #{password} where name=#{name} ")
    public void changePassword(@Param("name")String name,@Param("password")String password);

}
