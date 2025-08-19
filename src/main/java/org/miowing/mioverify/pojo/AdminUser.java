package org.miowing.mioverify.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("admin_users")
public class AdminUser {
    @TableId
    private String id;
    private String username;
    private String password;
    private String role = "ADMIN";
    private Boolean enabled = true;
    private String createdAt;
    private String lastLoginAt;
}
