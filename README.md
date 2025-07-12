# MioVerify

![Static Badge](https://img.shields.io/badge/version-v1.2.0--BETA-blue) ![Static Badge](https://img.shields.io/badge/java-17-purple) ![Static Badge](https://img.shields.io/badge/developer-Fuzihara_Yukina-orange) ![Static Badge](https://img.shields.io/badge/developer-pingguomc-orange) ![Static Badge](https://img.shields.io/badge/for-Minecraft_Java_Edition-green)

MioVerify是一个根据 *[Yggdrasil 服务端技术规范](https://github.com/yushijinhun/authlib-injector/wiki/Yggdrasil-%E6%9C%8D%E5%8A%A1%E7%AB%AF%E6%8A%80%E6%9C%AF%E8%A7%84%E8%8C%83)* 实现的MC身份验证服务器。

本分支旨在修复源分支的安全问题和部分Bug。

**什么是身份验证服务器？**

就好比Minecraft启动器可以登录你的正版号一样。authlib-injector 实现了一种外置登录，允许玩家使用第三方的验证服务器。在登录时，同样会要求输入账号(一般是邮箱)和密码，并且可以上传和查看皮肤等。

另外，如果游戏服务器也使用了 authlib-injector，就可以做到一样的正版验证效果。

Yggdrasil API是一套规范，定义了如何实现身份验证。而MioVerify正是按照这个规范开发出的Web服务器。实际上，每个人都可以设计一个自己的验证服务器。

**目前所有的主流启动器都已支持外置登录。**

有关 Minecraft 的 authlib-injector 技术，请详见 [yushijinhun/authlib-injector](https://github.com/yushijinhun/authlib-injector)。

## 实现功能

* 账号密码登录获取 token，刷新 token
* 验证和吊销 token，用户登出
* 单一和批量获取角色 (Profile) 信息
* 角色属性 RSA 签名
* 服务器元数据自定义配置
* 游戏服务器 session 会话验证
* 材质上传和下载 (待完善)
* 以上功能均遵守 Yggdrasil API
* 扩展 API：玩家角色注册 (正在扩展)
* SSL 支持，https 协议

## 开发工具

**IDE**: IntelliJ IDEA

**版本管理**: Git + Maven

**本项目使用的技术：**

* Spring Boot, Spring AOP, Spring Web
* JWT
* Redis
* Mybatis Plus (SQL)

**测试工具：**

* Postman
* HMCL-3.5.5
* Spring Test
* Navicat
* redis-cli

## 使用教程

在 **RELEASE** 中获取最新的 Jar包 (或者自行 Maven package) 并解压到一个文件夹。

请手动从 Jar 中 (`BOOT-INF\classes\application.yml`) 复制 `application.yml` 到 Jar 所在的文件夹中。

请 **务必** 修改 `application.yml` 里面的配置项，如果没有填写任何数据库信息，将会在运行目录自动生成和使用 SQLite 数据库。

**必须要先启动Redis服务器** 并在配置里面填写好地址。(如果你是直接在本地启用 redis 服务器的则不需要填写，保持默认即可)

服务器配置详情已经在 `application.yml` 写清楚了。

在项目目录下输入命令行启动对应jar即可。

## 扩展API

不同于 Yggdrasil API，MioVerify 提供了一套扩展的API，方便调用实现注册等功能，但还需要前端或者客户端继续实现可视化操作。

以下都是 `POST` 请求，并且 `Content-Type` 为 `application/json`，属于请求体内容。

如果操作成功，则返回 `200 OK`，否则返回其它状态码。

### 注册用户

此特性必须要在配置中启用。

`/extern/register/user`

```json
{
  "username": "用户名(请不要与已有的重复)",
  "password": "密码",
  "preferredLang": "偏好语言代号(不包含则默认为zh_CN)",
  "key": "密钥(配置中开启permission-key才必须包含)"
}
```

### 注册角色

此特性必须要在配置中启用。

`/extern/register/profile`

```json
{
  "profileName": "要注册角色名(是否允许重复请在配置中设置)",
  "skinUploadAllow": "是否允许上传皮肤(不包含则默认为true)",
  "capeUploadAllow": "是否允许上传披风(不包含则默认为true)",
  "username": "要绑定的用户名",
  "password": "要绑定的用户密码(配置中开启profile-strict才必须包含)",
  "key": "密钥(配置中开启permission-key才必须包含)"
}
```

## 材质API

目前MioVerify实现类皮肤站功能用的是内嵌的实现，每次请求角色(Profile)json时中的材质url的域名在`application.yml`中`server-domain`定义。如果服务器的域名是xxx.com，那么url将会类似这样：

```json
"url": "http://xxx.com/texture/hash/{hash}"
```

另外，如果`use-http`选项为`true`，将会是这样的：

```json
"url": "https://xxx.com/texture/hash/{hash}"
```

然而，如果要设置SSL支持，需要在配置中独立设置证书信息。

特别地，如果要获取默认的皮肤(可以在skins文件夹下更改)，将会是以下：

```plaintext
http(s)://xxx.com/texture/skin/default
```

## 待实现

* API地址指示(ALI)
* 动态的材质资源来源
* 密码密文存储
* 域名和SSL完整支持
* 后台管理网页(可能)
* ...

## 开发信息

本项目由作者 [FuziharaYukina](https://github.com/FuziharaYukina) 个人独立开发完成，可能存在亿点bug。

如果对此项目感兴趣，可以联系作者QQ1153624453加入开发~~团队~~。

本项目遵循 Apache-2.0 license 开源协议。

### 扩展开发

当前分支由 [pingguomc](https://github.com/pingguomc) 维护。
