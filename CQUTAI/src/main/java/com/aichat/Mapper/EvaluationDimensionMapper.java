package com.aichat.Mapper;

import com.aichat.Model.EvaluationDimension;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EvaluationDimensionMapper {
    @Select("SELECT * FROM evaluation_dimensions WHERE company_id = #{companyId} AND job_id = #{jobId}")
    List<EvaluationDimension> selectByCompanyAndJob(@Param("companyId") Integer companyId,
                                                    @Param("jobId") Integer jobId);

    @Insert("INSERT INTO evaluation_dimensions(company_id, name, description, weight) " +
            "VALUES(#{companyId}, #{name}, #{description}, #{weight})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(EvaluationDimension dimension);

    @Update("UPDATE evaluation_dimensions SET name = #{name}, description = #{description}, " +
            "weight = #{weight} WHERE id = #{id}")
    void update(EvaluationDimension dimension);

    @Delete("DELETE FROM evaluation_dimensions WHERE id = #{id}")
    void delete(@Param("id") Integer id);
}