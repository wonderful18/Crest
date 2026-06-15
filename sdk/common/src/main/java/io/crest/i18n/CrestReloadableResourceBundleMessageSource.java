package io.crest.i18n;

import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Set;
import java.util.stream.Collectors;

// 定义当前业务对象的核心职责
public class CrestReloadableResourceBundleMessageSource extends ReloadableResourceBundleMessageSource {

    @Override
    // 执行当前服务方法并返回处理结果
    public Set<String> getBasenameSet() {
        return super.getBasenameSet().stream().sorted(this::compare).collect(Collectors.toSet());
    }

    // 执行当前服务方法并返回处理结果
    private int compare(String o1, String o2) {
        return o1.substring(0, 1).compareTo(o2.substring(0, 1));
    }

}
