package com.aichat.Mapper;

import com.aichat.Model.Company;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CompanyMapper {
    @Select("SELECT * FROM companies WHERE id = #{id}")
    Company selectById(@Param("id") Integer id);

    @Select("SELECT * FROM companies WHERE license_no = #{licenseNo}")
    Company selectByLicenseNo(@Param("licenseNo") String licenseNo);

    @Insert("INSERT INTO companies(name, license_no, admin_email, address, logo_url, status, " +
            "description, scale, main_business) " +
            "VALUES(#{name}, #{licenseNo}, #{adminEmail}, #{address}, #{logoUrl}, #{status}, " +
            "#{description}, #{scale}, #{mainBusiness})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Company company);

    @Update("UPDATE companies SET name = #{name}, admin_email = #{adminEmail}, address = #{address}, " +
            "logo_url = #{logoUrl}, status = #{status}, description = #{description}, " +
            "scale = #{scale}, main_business = #{mainBusiness} WHERE id = #{id}")
    void update(Company company);
}