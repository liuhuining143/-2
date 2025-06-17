package com.aichat.Mapper;

import com.aichat.Model.Job;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface JobMapper {
    @Select("SELECT * FROM jobs WHERE id = #{id}")
    Job selectById(@Param("id") Integer id);

    @Select("SELECT * FROM jobs WHERE company_id = #{companyId} AND status = '1'")
    List<Job> selectActiveJobsByCompany(@Param("companyId") Integer companyId);

    @Insert("INSERT INTO jobs(company_id, title, description, requirements, salary_range, location, status) " +
            "VALUES(#{companyId}, #{title}, #{description}, #{requirements}, #{salaryRange}, #{location}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Job job);

    @Update("UPDATE jobs SET title = #{title}, description = #{description}, requirements = #{requirements}, " +
            "salary_range = #{salaryRange}, location = #{location}, status = #{status} WHERE id = #{id}")
    void update(Job job);
}
