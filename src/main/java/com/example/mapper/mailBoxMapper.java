package com.example.mapper;

import com.example.model.MailBoxModel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.mapping.StatementType;

/**
 * Created by guanxinquan on 15-3-9.
 */
public interface MailBoxMapper {

    public static final String TABLE_NAME = "mailbox";

    @Insert("insert into "+TABLE_NAME+" (name,userId) values (#{name},#{userId})")
    @SelectKey(before = false, keyProperty = "id", resultType = Long.class, statementType = StatementType.STATEMENT, statement = "SELECT LAST_INSERT_ID() AS id")
    public Integer createMailbox(@Param("name")String name,@Param("userId")Integer userId);

    @Select("select id,name,userId from "+ TABLE_NAME + " where name=#{name} and userId = #{userId}")
    public MailBoxModel selectMailbox(@Param("name")String name,@Param("userId")Integer userId);


}
