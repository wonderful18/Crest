# Crest 系统管理模块审查报告

> 审查范围：用户管理、角色权限、组织架构、认证授权、审计日志、SSO、系统参数
> 审查日期：2026-06-03
> 修复版本：v1.5.18 (2026-07-11)
> 审查状态：6 个高危问题已在 v1.5.18 中全部修复

---

## 一、高严重度问题（安全/数据风险）

### 1. SQL 注入风险 — PlatformPermissionManage.resourceScopeSql()
- **文件**：core/core-backend/src/main/java/io/crest/substitute/permissions/auth/PlatformPermissionManage.java:203
- **问题**：使用字符串拼接直接构建 SQL，roleIds 直接拼接到 IN 子句中，且 resourceType 未经转义直接拼接。
- **影响**：攻击者如果控制 resourceType 或 roleIds，可构造恶意 SQL。
- **修复建议**：使用 JdbcTemplate 参数化查询，或至少对 resourceType 做白名单校验。

### 2. SQL 注入风险 — CrestPermissionUtils.communityScopeSql()
- **文件**：sdk/common/src/main/java/io/crest/utils/CrestPermissionUtils.java
- **问题**：safeUid 只是简单替换单引号，整个 SQL 使用字符串拼接。
- **影响**：代码模式不安全，存在注入风险。
- **修复建议**：使用参数化查询替代字符串拼接。

### 3. SSO Callback 缺少 Nonce 验证
- **文件**：core/core-backend/src/main/java/io/crest/system/manage/SsoManage.java:161
- **问题**：login() 方法生成 nonce 并存入 cache，但 callback() 方法没有验证返回的 id_token 中的 nonce 是否与存入的一致。
- **影响**：存在重放攻击风险。
- **修复建议**：在 callback() 中解析 id_token 的 nonce claim，并与 ssoState.nonce() 比对。

### 4. SSO Claims 来源未验证一致性
- **文件**：core/core-backend/src/main/java/io/crest/system/manage/SsoManage.java:334
- **问题**：identityClaims() 合并了 id_token、access_token 和 userInfo 三个来源的 claims，但 id_token 未经签名验证（jwtClaims() 只做了 Base64 解码）。
- **影响**：攻击者可能伪造 id_token payload 中的 claims。
- **修复建议**：id_token 必须使用 SignedTokenUtils.verify() 验证签名后再解析 claims。

### 5. JWT Token 无过期时间（SSO 生成）
- **文件**：core/core-backend/src/main/java/io/crest/system/manage/SsoManage.java:408
- **问题**：generate() 方法中 SignedTokenUtils.sign(claims, user.getPasswordHash(), null) 第三个参数 expiresAt 为 null。
- **影响**：SSO 生成的 token 永不过期，即使密码修改后旧 token 仍然有效。
- **修复建议**：设置合理的过期时间（如 24 小时）。

### 6. 导入用户静默失败
- **文件**：core/core-backend/src/main/java/io/crest/substitute/permissions/user/CrestUserManage.java:317
- **问题**：importUsers() 中 catch (Exception ignored) 完全忽略异常，没有任何日志或反馈。
- **影响**：数据导入不可靠，失败无感知。
- **修复建议**：至少记录日志，并在返回结果中说明失败原因。

---

## 二、中严重度问题（逻辑缺陷/功能残缺）

### 7. 角色管理前端缺少错误处理
- **文件**：core/core-frontend/src/views/system/role/index.vue
- **问题**：save() 和 remove() 方法没有 try-catch，后端报错时前端无反馈。
- **影响**：用户操作失败时没有任何提示。
- **修复建议**：添加 try-catch 或统一错误处理。

### 8. defaultPwd() 返回空字符串
- **文件**：core/core-backend/src/main/java/io/crest/substitute/permissions/user/SubstituteUserServer.java:132
- **问题**：@GetMapping("/default-password") 直接返回空字符串。
- **影响**：前端如果调用此接口获取默认密码，会得到空值。
- **修复建议**：返回 initialPassword() 的值（注意脱敏或仅管理员可见）。

### 9. CrestPermissionUtils.currentUserIsAdmin() 使用反射
- **文件**：sdk/common/src/main/java/io/crest/utils/CrestPermissionUtils.java
- **问题**：每次调用都通过反射查找 isAdmin 方法并调用，性能差且可能因方法签名变化导致 NPE。
- **影响**：性能损耗，潜在 NPE 风险。
- **修复建议**：注入 CrestUserManage 直接调用，或缓存 Method 对象。

### 10. TokenFilter 中 Link Token 长度硬编码检查
- **文件**：sdk/common/src/main/java/io/crest/auth/filter/TokenFilter.java:86
- **问题**：StringUtils.length(linkToken) < 100 这个阈值没有说明依据。
- **影响**：潜在误判。
- **修复建议**：使用更合理的校验逻辑（如 JWT 格式校验）。

### 11. SysParameterServer.RequestTimeOut() 缺少异常处理
- **文件**：core/core-backend/src/main/java/io/crest/system/server/SysParameterServer.java:62
- **问题**：Integer.parseInt(settingItemVO.getPval()) 没有 try-catch，如果配置值不是整数会抛 NumberFormatException。
- **影响**：系统参数配置错误时导致接口 500。
- **修复建议**：添加 try-catch，默认值兜底。

### 12. i18nOptions() 路径遍历风险
- **文件**：core/core-backend/src/main/java/io/crest/system/server/SysParameterServer.java:106
- **问题**：直接读取 StaticResourceConstants.I18N_DIR 目录下的文件，通过文件名解析语言。
- **影响**：低概率但存在路径遍历风险。
- **修复建议**：校验文件名格式，限制读取目录。

### 13. SsoManage.config() 返回空 clientSecret 但标记已配置
- **文件**：core/core-backend/src/main/java/io/crest/system/manage/SsoManage.java:107
- **问题**：vo.setClientSecret("") 清空 secret，但 vo.setSecretConfigured(...) 标记是否已配置。
- **影响**：用户体验混淆。
- **修复建议**：返回脱敏后的 secret（如 ******）。

### 14. initAdmin() 硬编码 id=1
- **文件**：core/core-backend/src/main/java/io/crest/substitute/permissions/user/CrestUserManage.java:87
- **问题**：initAdmin() 只检查和初始化 id=1 的 admin，如果数据库中 admin 的 id 不是 1（如数据迁移后），会创建重复 admin。
- **影响**：数据迁移场景下可能产生重复管理员。
- **修复建议**：按 account='admin' 查询，而不是固定 id=1。

---

## 三、低严重度问题（代码质量/优化）

### 15. 审计日志 getClientIp 可能被伪造
- **文件**：core/core-backend/src/main/java/io/crest/log/CrestAuditAspect.java:190
- **问题**：X-Forwarded-For 第一个 IP 可以被客户端伪造，如果系统不位于可信反向代理后，审计日志记录的是假 IP。
- **修复建议**：增加可信代理白名单配置，或记录所有 X-Forwarded-For 值。

### 16. CrestAuditAspect 中异常日志丢失
- **文件**：core/core-backend/src/main/java/io/crest/log/CrestAuditAspect.java:89
- **问题**：catch (Throwable e) 中 responseMsg = e.getMessage()，但如果 e.getMessage() 为 null（如 NPE），审计日志记录空消息。
- **修复建议**：使用 e.getClass().getName() + ": " + e.getMessage() 兜底。

### 17. SecurityHeadersConfig 与 nginx 配置冲突
- **文件**：core/core-backend/src/main/java/io/crest/config/SecurityHeadersConfig.java
- **问题**：后端设置 X-Frame-Options: DENY，但 nginx 配置中 /api/v1/ 路径下也设置了安全头，同时 nginx 的 SPA 路径设置了 X-Frame-Options: SAMEORIGIN。
- **修复建议**：统一在后端或 nginx 中配置安全头，不要两边都配。

### 18. PlatformPermissionManage.bindUserToOrg() 并发问题
- **文件**：core/core-backend/src/main/java/io/crest/substitute/permissions/auth/PlatformPermissionManage.java:132
- **问题**：UPDATE 和 INSERT IGNORE 不是原子操作，并发下可能出现多个 default_org=1。
- **修复建议**：使用数据库唯一约束或事务隔离级别提升。

### 19. SsoManage.normalizeRedirect() 未过滤所有非法前缀
- **文件**：core/core-backend/src/main/java/io/crest/system/manage/SsoManage.java:452
- **问题**：只检查了 http://、https://、//，但没有过滤 javascript:、data: 等伪协议。
- **修复建议**：使用白名单机制，只允许相对路径。

### 20. resourceScopeSql() 返回值可能为 null 但调用方未处理
- **文件**：core/core-backend/src/main/java/io/crest/substitute/permissions/auth/PlatformPermissionManage.java:203
- **问题**：当 uid == null || isSystemAdmin(uid) 时返回 null，调用方如果直接拼接到 SQL 中，可能出现 WHERE null 的情况。
- **修复建议**：在调用方明确处理 null 情况。

---

## 四、前端问题补充

### 21. 审计日志页面无分页加载状态提示
- **文件**：core/core-frontend/src/views/system/audit-log/index.vue
- **问题**：如果审计日志数据量大，查询慢时用户看不到加载状态。
- **建议**：添加 v-loading。

### 22. 系统参数 BasicEdit.vue 等子组件未审查
- **问题**：参数编辑表单可能缺少前端校验，导致非法值提交到后端。
- **建议**：增加前端校验（如 RequestTimeOut 必须是数字）。

### 23. SSO 配置前端 sso/index.vue 未审查
- **问题**：SSO 配置的端点 URL 输入框可能缺少格式校验。
- **建议**：增加 URL 格式前端校验。

---

## 五、总结

| 优先级 | 数量 | 核心问题 |
|-------|------|---------|
| 高 | 6 | SQL注入、SSO安全缺陷、导入静默失败 |
| 中 | 8 | 逻辑缺陷、错误处理缺失、反射性能 |
| 低 | 9 | 代码质量、配置冲突、审计完整性 |

最紧急需要修复的 Top 3：
1. SQL 注入（resourceScopeSql 和 communityScopeSql）
2. SSO 安全（nonce 验证、id_token 签名验证、token 过期）
3. 导入用户静默失败（至少记录日志和返回错误信息）
