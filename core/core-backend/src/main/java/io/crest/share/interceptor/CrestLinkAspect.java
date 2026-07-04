package io.crest.share.interceptor;

import io.crest.auth.CrestLinkPermit;
import io.crest.constant.AuthConstant;
import io.crest.exception.CrestException;
import io.crest.share.manage.ShareLinkAccessManage;
import io.crest.utils.LogUtil;
import io.crest.utils.ServletUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 分享链接访问切面，校验带链接令牌的资源访问权限
 */
@Aspect
@Component
public class CrestLinkAspect {

    /**
     * SpEL 参数变量名前缀
     */
    private static final String PARAM_VARIABLE_PREFIX = "p";
    /**
     * SpEL 表达式起始标记
     */
    private static final String SPRING_EL_FLAG = "#";
    private static final Pattern LINK_EXPRESSION_PATTERN = Pattern.compile("#p\\d+(\\.[A-Za-z_$][\\w$]*)*");
    /**
     * 分享链接访问权限管理器
     */
    private final ShareLinkAccessManage shareLinkAccessManage;

    /**
     * 构造分享链接访问切面
     */
    public CrestLinkAspect(ShareLinkAccessManage shareLinkAccessManage) {
        this.shareLinkAccessManage = shareLinkAccessManage;
    }


    /**
     * 环绕拦截带分享链接许可注解的方法，并校验资源 ID 是否可访问
     */
    @Around(value = "@annotation(io.crest.auth.CrestLinkPermit)")
    public Object logAround(ProceedingJoinPoint point) throws Throwable {
        Object[] params = point.getArgs();
        String linkToken = ServletUtils.getHead(AuthConstant.LINK_TOKEN_KEY);
        if (StringUtils.isNotBlank(linkToken)) {
            MethodSignature ms = (MethodSignature) point.getSignature();
            Method method = ms.getMethod();
            CrestLinkPermit linkPermit = method.getAnnotation(CrestLinkPermit.class);
            String value = linkPermit.value();
            if (StringUtils.isBlank(value)) {
                value = SPRING_EL_FLAG + PARAM_VARIABLE_PREFIX + "0";
            }
            Long id = getExpression(params, value);
            Object resourceIdAttr = ServletUtils.request().getAttribute(AuthConstant.LINK_RESOURCE_ID_ATTR);
            if (ObjectUtils.isEmpty(resourceIdAttr)) {
                CrestException.throwException("link token invalid");
            }
            Long resourceId = Long.parseLong(resourceIdAttr.toString());
            if (!shareLinkAccessManage.canAccessWithShareResource(resourceId, id)) {
                CrestException.throwException("link token invalid");
                return false;
            }
        }
        try {
            return point.proceed(params);
        } catch (Exception e) {
            LogUtil.info(e.getMessage());
            throw e;
        }
    }

    /**
     * 从方法参数和注解表达式中解析资源 ID
     */
    public Long getExpression(Object[] params, String expression) {
        Object o = resolveValue(params, expression);
        if (ObjectUtils.isNotEmpty(o)) return Long.parseLong(o.toString());
        return null;
    }

    private Object resolveValue(Object[] params, String expression) {
        if (StringUtils.isBlank(expression)) {
            return null;
        }
        String normalized = expression.trim();
        if (!normalized.startsWith(SPRING_EL_FLAG)) {
            return normalized;
        }
        if (!LINK_EXPRESSION_PATTERN.matcher(normalized).matches()) {
            LogUtil.debug("分享链接表达式不受支持: " + expression);
            return null;
        }
        return resolvePathValue(params, normalized);
    }

    private Object resolvePathValue(Object[] params, String expression) {
        String[] parts = expression.substring(1).split("\\.");
        int index;
        try {
            index = Integer.parseInt(parts[0].substring(PARAM_VARIABLE_PREFIX.length()));
        } catch (NumberFormatException e) {
            LogUtil.debug("分享链接表达式参数索引不合法: " + expression);
            return null;
        }
        if (params == null || index < 0 || index >= params.length) {
            return null;
        }
        Object value = params[index];
        for (int i = 1; i < parts.length && value != null; i++) {
            value = resolvePropertyValue(value, parts[i]);
        }
        return value;
    }

    private Object resolvePropertyValue(Object source, String propertyName) {
        if ("class".equals(propertyName)) {
            return null;
        }
        if (source instanceof Map<?, ?> map) {
            return map.get(propertyName);
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(source);
        return wrapper.isReadableProperty(propertyName) ? wrapper.getPropertyValue(propertyName) : null;
    }
}
