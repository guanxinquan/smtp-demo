package com.example.mapper;

import com.example.model.MailModel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;

/**
 * Created by guanxinquan on 15-3-9.
 */
public interface MailMapper {

    public static final String TABLE_NAME = "mail";

    @Insert("insert into "+TABLE_NAME+" (mailboxId,content,size) values (#{mailboxId},#{content},#{size})")
    @SelectKey(before = false, keyProperty = "id", resultType = Long.class, statementType = StatementType.STATEMENT, statement = "SELECT LAST_INSERT_ID() AS id")
    public Integer createMail(@Param("mailboxId")Integer mailboxId,@Param("size")Integer size,@Param("content")String content);


    @Select("select id,mailboxId,content,size from "+TABLE_NAME + " where id=#{id}")
    public MailModel selectMailById(Integer id);

    @Select("select id,mailboxId,size from "+ TABLE_NAME + " where mailboxId=#{mailboxId}")
    public List<MailModel> selectMails(Integer mailboxId);
}
