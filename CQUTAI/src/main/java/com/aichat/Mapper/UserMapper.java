package com.aichat.Mapper;

import com.aichat.Model.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE id = #{id}")
    User selectById(@Param("id") Integer id);

    @Select("SELECT * FROM users WHERE email = #{email}")
    User selectByEmail(@Param("email") String email);

    @Insert("INSERT INTO users(username, password, full_name, email, phone, avatar_url, gender, birth_date, " +
            "wechat_openid, wechat_nickname, university, major, age, work_experience) " +
            "VALUES(#{username}, #{password}, #{fullName}, #{email}, #{phone}, #{avatarUrl}, #{gender}, #{birthDate}, " +
            "#{wechatOpenid}, #{wechatNickname}, #{university}, #{major}, #{age}, #{workExperience})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    @Update("UPDATE users SET full_name = #{fullName}, email = #{email}, phone = #{phone}, " +
            "avatar_url = #{avatarUrl}, gender = #{gender}, birth_date = #{birthDate}, " +
            "university = #{university}, major = #{major}, age = #{age}, work_experience = #{workExperience} " +
            "WHERE id = #{id}")
    void update(User user);
}