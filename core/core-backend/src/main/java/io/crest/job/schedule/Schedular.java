package io.crest.job.schedule;

import io.crest.datasource.server.DatasourceServer;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
// 定义当前业务对象的核心职责
public class Schedular {

    @Resource
    private DatasourceServer datasourceServer;

    @Scheduled(cron = "0 0/3 * * * ?")
    // 格式化日期时间并返回统一展示值
    public void updateStopJobStatus() {
        datasourceServer.updateStopJobStatus();
    }

}
