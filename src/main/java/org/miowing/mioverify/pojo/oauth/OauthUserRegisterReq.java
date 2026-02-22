package org.miowing.mioverify.pojo.oauth;

import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class OauthUserRegisterReq {

    private String username;

    private @Nullable String password; // 密码可为空

    private String preferredLang = "zh_CN";

    private String key;

    // OAuth相关字段

    private @Nullable String microsoftId;

    private @Nullable String githubId;

    private @Nullable String mcjpgId;

    private @Nullable String customId;
}
