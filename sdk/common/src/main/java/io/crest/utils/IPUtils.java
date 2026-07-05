package io.crest.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class IPUtils {

    private static final String HEAD_KEYS = "x-forwarded-for, Proxy-Client-IP, WL-Proxy-Client-IP";

    private static final String UNKNOWN = "unknown";

    private static final String LOCAL_IP_KEY = "0:0:0:0:0:0:0:1";
    private static final String LOCAL_IP_VAL = "127.0.0.1";
    private static final int MAX_IP_LENGTH = 64;
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-fA-F:]+$");

    public static String get() {

        String ipStr = null;
        boolean isProxy = false;

        HttpServletRequest request = null;
        try {
            request = ServletUtils.request();
        } catch (RuntimeException e) {
            LogUtil.error(e.getMessage(), e);
            return null;
        }
        if (ObjectUtils.isEmpty(request)) return null;
        String[] keyArr = HEAD_KEYS.split(",");
        for (String key : keyArr) {
            String header = request.getHeader(key.trim());
            String proxyIp = firstValidIp(header);
            if (StringUtils.isNotBlank(proxyIp)) {
                ipStr = proxyIp;
                isProxy = true;
                break;
            }
        }

        if (!isProxy) {
            ipStr = firstValidIp(request.getRemoteAddr());
        }
        if (StringUtils.isBlank(ipStr)) {
            return LOCAL_IP_VAL;
        }
        return Strings.CS.equals(LOCAL_IP_KEY, ipStr) ? LOCAL_IP_VAL : ipStr;
    }

    public static String domain() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return LOCAL_IP_VAL;
        }
    }

    private static String firstValidIp(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return Arrays.stream(value.split(","))
                .map(IPUtils::normalizeIpCandidate)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private static String normalizeIpCandidate(String value) {
        String candidate = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(candidate) || Strings.CI.equals(UNKNOWN, candidate) || candidate.length() > MAX_IP_LENGTH) {
            return null;
        }
        if (candidate.startsWith("[") && candidate.contains("]")) {
            candidate = candidate.substring(1, candidate.indexOf(']'));
        } else if (candidate.indexOf(':') == candidate.lastIndexOf(':')) {
            int portIndex = candidate.indexOf(':');
            if (portIndex > 0 && IPV4_PATTERN.matcher(candidate.substring(0, portIndex)).matches()) {
                candidate = candidate.substring(0, portIndex);
            }
        }
        if (IPV4_PATTERN.matcher(candidate).matches() || isIpv6Literal(candidate)) {
            return candidate;
        }
        return null;
    }

    private static boolean isIpv6Literal(String candidate) {
        if (!candidate.contains(":") || !IPV6_CHARS.matcher(candidate).matches()) {
            return false;
        }
        try {
            return InetAddress.getByName(candidate).getHostAddress().contains(":");
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
