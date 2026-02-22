package org.miowing.mioverify.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * <h1>用户 数据表映射</h1>
 */
@Data
@Accessors(chain = true)
@TableName("users")
public class User {
    @TableId
    private String id;
    private String username;
    private @Nullable String password;
    private @Nullable String preferredLang;
    private @Nullable String microsoftId;
    private @Nullable String githubId;
    private @Nullable String mcjpgId;
    private @Nullable String customId;
}