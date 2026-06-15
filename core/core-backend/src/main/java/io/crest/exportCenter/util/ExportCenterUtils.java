package io.crest.exportCenter.util;

import io.crest.exportCenter.manage.ExportCenterLimitManage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
// 定义当前业务对象的核心职责
public class ExportCenterUtils {


    private static ExportCenterLimitManage exportCenterLimitManage;

    @Resource(name = "exportCenterLimitManage")
    // 执行当前服务方法并返回处理结果
    public void setExportCenterLimitManage(ExportCenterLimitManage exportCenterLimitManage) {
        ExportCenterUtils.exportCenterLimitManage = exportCenterLimitManage;
    }

    // 执行当前服务方法并返回处理结果
    public static long getExportLimit(String type) {
        return exportCenterLimitManage.getExportLimit(type);
    }
}
