package org.miowing.mioverify.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

@Data
@Accessors(chain = true)
@TableName("oauth_users")
public class OAuthUser {
    @TableId
    private String id;
    private String provider;
    private String providerUserId;
    private @Nullable String providerUsername;
    private String bindUser;
}
