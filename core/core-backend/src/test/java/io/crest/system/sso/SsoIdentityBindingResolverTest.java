package io.crest.system.sso;

import io.crest.exception.CrestException;
import io.crest.substitute.permissions.user.CrestUserManage;
import io.crest.substitute.permissions.user.model.CrestUser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

// 验证单点登录身份绑定决策逻辑。
public class SsoIdentityBindingResolverTest {

    private final SsoIdentityBindingResolver resolver = new SsoIdentityBindingResolver();

    // 已绑定外部身份时更新绑定用户。
    @Test
    public void updatesUserWhenProviderSubjectIsAlreadyBound() {
        CrestUser bound = user(1L, "old.account", true);

        SsoIdentityDecision decision = resolver.resolve(profile("new.account"), bound, null, true);

        assertEquals(SsoIdentityAction.UPDATE_BOUND_USER, decision.getAction());
        assertEquals(1L, decision.getUserId().longValue());
    }

    // 已绑定用户禁用时拒绝登录。
    @Test
    public void rejectsDisabledBoundUser() {
        CrestUser bound = user(1L, "zhang.san", false);

        assertThrows(CrestException.class, () -> resolver.resolve(profile("zhang.san"), bound, null, true));
    }

    // 已绑定用户与同账号用户不一致时拒绝登录。
    @Test
    public void rejectsAccountConflictWhenBoundUserDiffersFromAccountUser() {
        CrestUser bound = user(1L, "old.account", true);
        CrestUser accountUser = user(2L, "zhang.san", true);

        assertThrows(CrestException.class, () -> resolver.resolve(profile("zhang.san"), bound, accountUser, true));
    }

    // 本地账号匹配时绑定已有用户。
    @Test
    public void bindsExistingLocalUserWhenAccountMatches() {
        CrestUser accountUser = user(2L, "zhang.san", true);

        SsoIdentityDecision decision = resolver.resolve(profile("zhang.san"), null, accountUser, true);

        assertEquals(SsoIdentityAction.BIND_EXISTING_USER, decision.getAction());
        assertEquals(2L, decision.getUserId().longValue());
    }

    // 待绑定本地账号禁用时拒绝登录。
    @Test
    public void rejectsDisabledAccountUserBeforeBinding() {
        CrestUser accountUser = user(2L, "zhang.san", false);

        assertThrows(CrestException.class, () -> resolver.resolve(profile("zhang.san"), null, accountUser, true));
    }

    // 未匹配用户且允许自动创建时创建用户。
    @Test
    public void createsUserWhenNoBindingAndAutoCreateEnabled() {
        SsoIdentityDecision decision = resolver.resolve(profile("zhang.san"), null, null, true);

        assertEquals(SsoIdentityAction.CREATE_USER, decision.getAction());
        assertEquals("zhang.san", decision.getProfile().getAccount());
    }

    // 未知用户且不允许自动创建时拒绝登录。
    @Test
    public void rejectsUnknownUserWhenAutoCreateDisabled() {
        assertThrows(CrestException.class, () -> resolver.resolve(profile("zhang.san"), null, null, false));
    }

    // 已绑定用户与本地账号为同一用户时允许更新绑定。
    @Test
    public void acceptsSameUserForBoundAndAccountMatch() {
        CrestUser bound = user(1L, "zhang.san", true);
        CrestUser accountUser = user(1L, "zhang.san", true);

        SsoIdentityDecision decision = resolver.resolve(profile("zhang.san"), bound, accountUser, true);

        assertEquals(SsoIdentityAction.UPDATE_BOUND_USER, decision.getAction());
        assertEquals(1L, decision.getUserId().longValue());
    }

    // 身份资料缺少提供方 ID 时拒绝处理。
    @Test
    public void rejectsProfileWithoutProviderId() {
        SsoIdentityProfile profile = profile("zhang.san");
        profile.setProviderId(null);

        assertThrows(CrestException.class, () -> resolver.resolve(profile, null, null, true));
    }

    // 身份资料缺少提供方类型时拒绝处理。
    @Test
    public void rejectsProfileWithoutProviderType() {
        SsoIdentityProfile profile = profile("zhang.san");
        profile.setProviderType(null);

        assertThrows(CrestException.class, () -> resolver.resolve(profile, null, null, true));
    }

    // 身份资料缺少外部主体时拒绝处理。
    @Test
    public void rejectsProfileWithoutSubject() {
        SsoIdentityProfile profile = profile("zhang.san");
        profile.setExternalSubject(null);

        assertThrows(CrestException.class, () -> resolver.resolve(profile, null, null, true));
    }

    // 决策前补齐账号和展示名称。
    @Test
    public void normalizesAccountAndDisplayNameBeforeDecision() {
        SsoIdentityProfile profile = profile("zhang.san");
        profile.setName(null);

        SsoIdentityDecision decision = resolver.resolve(profile, null, null, true);

        assertEquals("zhang.san", decision.getProfile().getAccount());
        assertEquals("zhang.san", decision.getProfile().getName());
    }

    // 构造测试用身份资料。
    private SsoIdentityProfile profile(String account) {
        SsoIdentityProfile profile = new SsoIdentityProfile();
        profile.setProviderId(100L);
        profile.setProviderType(SsoProviderType.CASDOOR);
        profile.setExternalSubject("external-1");
        profile.setAccount(account);
        profile.setName("张三");
        profile.setEmail("zhang.san@example.com");
        return profile;
    }

    // 构造测试用本地用户。
    private CrestUser user(Long id, String account, boolean enable) {
        CrestUser user = new CrestUser();
        user.setId(id);
        user.setAccount(account);
        user.setName(account);
        user.setEnable(enable);
        user.setAuthType(CrestUserManage.AUTH_TYPE_LOCAL);
        return user;
    }
}
