package org.miowing.mioverify.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * <h1>角色 数据表映射</h1>
 */
@Data
@Accessors(chain = true)
@TableName("profiles")
public class Profile {
    @TableId
    private String id;
    private String name;
    private String bindUser;
    private Boolean skinUpAllow = false;
    private Boolean capeUpAllow = false;
    private @Nullable String skinHash;
    private @Nullable String capeHash;
    private Boolean skinSlim = false;
}