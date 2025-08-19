package org.miowing.mioverify.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.miowing.mioverify.pojo.AdminUser;

@Mapper
public interface AdminUserDao extends BaseMapper<AdminUser> {
}
