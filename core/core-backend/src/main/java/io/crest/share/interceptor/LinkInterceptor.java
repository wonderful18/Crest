package io.crest.share.interceptor;

import io.crest.auth.CrestLinkPermit;
import io.crest.constant.AuthConstant;
import io.crest.exception.CrestException;
import io.crest.utils.ServletUtils;
import io.crest.utils.WhitelistUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;


@Component
public class LinkInterceptor implements HandlerInterceptor {

    private final static String whiteListText = "/user/ip-info, /dataset-data/enum-values, /dataset-data/enum-values/object, /dataset-data/field-tree, /public-key, /share/validate";

    private final static String whiteStartListText = "/data-visualization/type/";

    private boolean isWhiteStart(String url) {
        List<String> whiteStartList = Arrays.stream(StringUtils.split(whiteStartListText, ",")).map(String::trim).toList();
        return whiteStartList.stream().anyMatch(item -> Strings.CS.startsWith(url, item));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String linkToken = ServletUtils.getHead(AuthConstant.LINK_TOKEN_KEY);
        if (linkToken == null) {
            return true;
        }
        if (handler instanceof HandlerMethod handlerMethod) {
            CrestLinkPermit linkPermit = handlerMethod.getMethodAnnotation(CrestLinkPermit.class);
            if (linkPermit == null) {

                List<String> whiteList = Arrays.stream(StringUtils.split(whiteListText, ",")).map(String::trim).toList();

                String requestURI = ServletUtils.request().getRequestURI();
                requestURI = WhitelistUtils.removePrefix(requestURI, WhitelistUtils.getContextPath());
                requestURI = WhitelistUtils.removePrefix(requestURI, AuthConstant.CREST_API_PREFIX);
                boolean valid = whiteList.contains(requestURI) || isWhiteStart(requestURI) || WhitelistUtils.match(requestURI);
                if (!valid) {
                    CrestException.throwException("分享链接Token不支持访问当前url[" + requestURI + "]");
                }
                return true;
            }
        }
        return true;
    }


}
