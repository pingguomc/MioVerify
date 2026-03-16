# MioVerify API 接口文档

**Yggdrasil-服务端技术规范** 未规定，但在本项目使用的 API 的文档。

## 基本约定

基本上与 **Yggdrasil-服务端技术规范** 保持一致。

## OAuth 第三方登录/注册

### OAuth 状态查询

`POST /oauth/status`

用于检查 OAuth 是否启用及可用的提供商列表。

响应格式：

```json5
{
  "enabled": true,
  //ture/false
  "providers": [
    "microsoft",
    "github",
    "mcjpg",
    "custom"
  ]
  // 已启用的服务商列表
}
```

### 授权端点

`GET /oauth/authorize/{provider}`

将被 `302` 重定向至指定的 Provider （ Authorization Server ）的授权页。前端应通过新窗口或直接跳转的方式访问此端点。

### 回调与重定向端点

`GET /oauth/callback/{provider}`

此端点由 Provider 在用户授权后自动调用，**无需前端直接访问**。

后端处理完毕后会将浏览器重定向至配置的 `OAuth 前端回调地址`，并在 URL **查询** 参数中附加以下信息：

|      参数名      |   类型    | 描述                                                    |
|:-------------:|:-------:|:------------------------------------------------------|
|  `tempToken`  | string  | 临时令牌，用于调用 `POST /oauth/authenticate` 换取 `accessToken` |
|  `provider`   | string  | 使用的 OAuth 提供商名称（如 `github`、`microsoft`）               |
| `needProfile` | boolean | 是否需要用户创建角色                                            |
|   `userId`    | string  | 当 `needProfile=true` 时返回用户 ID，便于前端引导创建角色              |

### 登录 Yggdrasil

`POST /oauth/authenticate`

请求格式：

```json5
{
  "tempToken": "OAuth 回调端点后返回的临时 Token",
  "clientToken": "由客户端指定的令牌的 clientToken（可选）",
  "requestUser": false,
  // 是否在响应中包含用户信息，默认 false
}
```

若请求中未包含 `clientToken`，服务端应该随机生成一个无符号 UUID 作为 `clientToken`。但需要注意 `clientToken`
可以为任何字符串，即请求中提供任何 `clientToken` 都是可以接受的，不一定要为无符号 UUID。

响应格式：

```json5
{
  "accessToken": "令牌的 accessToken",
  "clientToken": "令牌的 clientToken",
  "availableProfiles": [
    // 用户可用角色列表
    // ,... 每一项为一个角色（格式见 §角色信息的序列化）
  ],
  "selectedProfile": {
    // ... 绑定的角色，若为空，则不需要包含（格式见 §角色信息的序列化）
  },
  "user": {
    // ... 用户信息（仅当请求中 requestUser 为 true 时包含，格式见 §用户信息的序列化）
  }
}
```

### 绑定第三方账号到当前用户

`POST /oauth/bind/{provider}`

TODO

将被 `302` 重定向至指定的 Provider （ Authorization Server ）的授权页

### 解绑第三方账号

`DELETE /oauth/bind/{provider}`

请求需要带上 HTTP 头部 `Authorization: Bearer {accessToken}` 进行认证。若未包含 Authorization 头或 accessToken 无效，则返回
`401 Unauthorized`。

若用户仅剩一个第三方认证，则解绑失败，返回 `403	Forbidden`

若操作成功或从未绑定这个服务商，服务端应返回 HTTP 状态 `204 No Content`

### 获取所有支持的 OAuth 提供商及当前用户的绑定状态

`GET /oauth/providers`

请求需要带上 HTTP 头部 `Authorization: Bearer {accessToken}` 进行认证。若未包含 Authorization 头或 accessToken 无效，则返回
`401 Unauthorized`。

响应格式：

```json5
{
  "providers": [
    {
      "provider": "唯一标识符",  // 例如 github
      "bond": true // true or false 表示是否绑定
    },
    // 可以包含更多
  ]
}
```

### 登出

`POST /oauth/signout`

请求需要带上 HTTP 头部 `Authorization: Bearer {accessToken}` 进行认证。若未包含 Authorization 头或 accessToken 无效，则返回
`401 Unauthorized`。

若操作成功，服务端应返回 HTTP 状态 `204 No Content`。

## 密码管理

## 其他（待定）