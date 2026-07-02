package io.crest.log;

import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.utils.AuthUtils;
import io.crest.utils.CommonBeanFactory;
import io.crest.utils.LogUtil;
import io.crest.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * CrestAudit 注解 AOP 切面
 *
 * 拦截 @CrestAudit 注解的方法，自动记录审计日志
 */
@Aspect
@Component
public class CrestAuditAspect {

    @Autowired
    private AuditLogService auditLogService;

    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
    private static final Pattern AUDIT_EXPRESSION_PATTERN = Pattern.compile("#[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*");

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint point, CrestAudit audit) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        HttpServletRequest request = ServletUtils.request();
        String requestMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestUrl = request != null ? request.getRequestURI() : "UNKNOWN";
        String clientIp = getClientIp(request);

        // 获取操作人信息
        Long operatorId = AuthUtils.getUser() != null ? AuthUtils.getUser().getUserId() : null;
        String operatorName = null;
        String operatorAccount = null;
        if (operatorId != null) {
            try {
                // 从数据库查询用户信息
                io.crest.substitute.permissions.user.model.CrestUser user =
                    CommonBeanFactory.getBean(io.crest.substitute.permissions.user.CrestUserManage.class).queryById(operatorId);
                if (user != null) {
                    operatorName = user.getName();
                    operatorAccount = user.getAccount();
                }
            } catch (Exception e) {
                LogUtil.debug("获取用户信息失败: " + e.getMessage());
            }
        }

        // 解析资源ID
        String resourceId = null;
        try {
            resourceId = parseExpression(audit.id(), point);
        } catch (Exception e) {
            LogUtil.debug("解析资源ID失败: " + e.getMessage());
        }

        // 获取资源类型
        LogST sourceType = resolveResourceType(audit, point);
        String resourceType = sourceType.name();

        // 获取操作类型
        LogOT operationType = audit.ot();

        Object result = null;
        int responseCode = 200;
        String responseMsg = "success";

        try {
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            responseCode = 500;
            responseMsg = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            HttpServletResponse response = ServletUtils.response();
            if (response != null && response.getStatus() >= 400) {
                responseCode = response.getStatus();
                responseMsg = responseMsg == null || "success".equals(responseMsg) ? "failed" : responseMsg;
            }

            // 获取资源名称（从方法名推断）
            String resourceName = AuditLogText.description(operationType, resourceType, requestUrl);

            // 异步记录审计日志
            auditLogService.log(
                    operationType, resourceType, resourceId, resourceName,
                    requestMethod, requestUrl,
                    operatorId, operatorName, operatorAccount, clientIp,
                    duration, responseCode, responseMsg
            );
        }
    }

    /**
     * 仅支持审计注解需要的参数路径表达式，例如 #p0、#p0.id、#request.id。
     */
    private String parseExpression(String expression, ProceedingJoinPoint point) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        String normalized = expression.trim();
        if (!AUDIT_EXPRESSION_PATTERN.matcher(normalized).matches()) {
            LogUtil.debug("审计表达式不受支持: " + expression);
            return null;
        }

        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            String[] paramNames = discoverer.getParameterNames(method);
            Object[] args = point.getArgs();
            Object value = resolveAuditExpression(normalized, paramNames, args);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            LogUtil.debug("审计表达式解析失败: " + expression + ", " + e.getMessage());
            return null;
        }
    }

    private Object resolveAuditExpression(String expression, String[] paramNames, Object[] args) {
        String[] parts = expression.substring(1).split("\\.");
        Object value = resolveRootValue(parts[0], paramNames, args);
        for (int i = 1; i < parts.length && value != null; i++) {
            value = resolvePropertyValue(value, parts[i]);
        }
        return value;
    }

    private Object resolveRootValue(String name, String[] paramNames, Object[] args) {
        if (name.startsWith("p")) {
            try {
                int index = Integer.parseInt(name.substring(1));
                return index >= 0 && index < args.length ? args[index] : null;
            } catch (NumberFormatException ignored) {
                // 不是 #p0 形式时继续按参数名解析。
            }
        }
        if (paramNames == null) {
            return null;
        }
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            if (name.equals(paramNames[i])) {
                return args[i];
            }
        }
        return null;
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

    private LogST resolveResourceType(CrestAudit audit, ProceedingJoinPoint point) {
        String stExpression = audit.stExp();
        if (stExpression != null && !stExpression.isBlank()) {
            LogST resolved = toLogST(parseExpression(stExpression, point));
            if (resolved != null) {
                return resolved;
            }
        }
        return audit.st();
    }

    private LogST toLogST(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        switch (normalized.toLowerCase(Locale.ROOT)) {
            case "datav":
            case "screen":
                return LogST.SCREEN;
            case "dashboard":
            case "panel":
            case "panelmobile":
                return LogST.PANEL;
            case "dataset":
                return LogST.DATASET;
            case "datasource":
                return LogST.DATASOURCE;
            default:
                break;
        }
        try {
            return LogST.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP，取第一个
            int index = ip.indexOf(',');
            if (index > 0) {
                ip = ip.substring(0, index);
            }
            return ip.trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getRemoteAddr();
        // 将 IPv6 本地地址转换为 IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

}
