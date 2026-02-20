package org.miowing.mioverify.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

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