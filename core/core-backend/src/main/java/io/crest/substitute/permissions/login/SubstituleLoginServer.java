package io.crest.substitute.permissions.login;

import io.crest.api.permissions.login.dto.PwdLoginDTO;
import io.crest.auth.bo.TokenUserBO;
import io.crest.auth.vo.TokenVO;
import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.log.CrestAudit;
import io.crest.substitute.permissions.user.CrestUserManage;
import io.crest.substitute.permissions.user.model.CrestUser;
import io.crest.system.manage.SsoManage;
import io.crest.utils.LogUtil;
import io.crest.utils.RsaUtils;
import io.crest.utils.SignedTokenUtils;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@ConditionalOnMissingBean(name = "loginServer")
@RestController
@RequestMapping
public class SubstituleLoginServer {

    @Resource
    private CrestUserManage crestUserManage;

    @Resource
    private SsoManage ssoManage;

    @Resource
    private PlatformPermissionManage platformPermissionManage;

    @CrestAudit(ot = LogOT.LOGIN, st = LogST.USER)
    @PostMapping("/login/local-login")
    public TokenVO localLogin(@RequestBody PwdLoginDTO dto) {

        String name = dto.getName();
        name = RsaUtils.decryptStr(name);
        String pwd = dto.getPwd();
        pwd = RsaUtils.decryptStr(pwd);

        dto.setName(name);
        dto.setPwd(pwd);

        CrestUser user = crestUserManage.queryByAccount(name);
        if (user == null || !crestUserManage.passwordMatches(user, pwd)) {
            CrestException.throwException("用户名或密码错误");
        }
        if (Boolean.FALSE.equals(user.getEnable())) {
            CrestException.throwException("用户已停用");
        }
        boolean emergencyAdmin = Boolean.TRUE.equals(dto.getEmergency()) && Boolean.TRUE.equals(user.getAdmin());
        if (CrestUserManage.AUTH_TYPE_SSO.equalsIgnoreCase(user.getAuthType()) && !emergencyAdmin) {
            CrestException.throwException("单点登录用户请使用企业账号登录");
        }
        if (!ssoManage.localLoginAllowed() && !emergencyAdmin) {
            CrestException.throwException("当前已启用单点登录，请使用企业账号登录");
        }
        TokenUserBO tokenUserBO = new TokenUserBO();
        tokenUserBO.setUserId(user.getId());
        tokenUserBO.setDefaultOid(platformPermissionManage.defaultOrgId(user.getId()));
        crestUserManage.markLoginSuccess(user.getId());
        return generate(tokenUserBO, user.getPasswordHash());
    }


    @GetMapping("/login/refresh")
    public TokenVO refresh() {
        // 获取当前用户
        io.crest.auth.bo.TokenUserBO userBO = io.crest.utils.AuthUtils.getUser();
        if (userBO == null) {
            CrestException.throwException("用户未登录");
        }

        // 查询用户信息用于生成新Token
        io.crest.substitute.permissions.user.model.CrestUser user = crestUserManage.queryById(userBO.getUserId());
        if (user == null) {
            CrestException.throwException("用户不存在");
        }

        return generate(userBO, user.getPasswordHash());
    }

    @CrestAudit(ot = LogOT.LOGIN, st = LogST.USER)
    @GetMapping("/logout")
    public void logout() {
        LogUtil.info("substitule logout");
    }

    private TokenVO generate(TokenUserBO bo, String secret) {
        Long userId = bo.getUserId();
        Long defaultOid = bo.getDefaultOid();

        // Token过期时间：24小时
        long expirationMillis = 24 * 60 * 60 * 1000L;
        Date expiresAt = new Date(System.currentTimeMillis() + expirationMillis);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("uid", userId);
        claims.put("oid", defaultOid);
        String token = SignedTokenUtils.sign(claims, secret, expiresAt);
        return new TokenVO(token, expirationMillis);
    }
}
