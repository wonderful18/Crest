package io.crest.listener;

import io.crest.job.schedule.DataFillingTaskExecutor;
import io.crest.job.schedule.ScheduledTaskExecutor;
import io.crest.job.schedule.DataSyncTaskExecutor;
import io.crest.utils.LogUtil;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = 4)
// 定义当前业务对象的核心职责
public class DataSyncTaskStarter implements ApplicationRunner {

    @Resource
    private ScheduledTaskExecutor taskExecutor;

    @Resource
    private DataFillingTaskExecutor dataFillingTaskExecutor;

    @Resource
    private DataSyncTaskExecutor dataSyncTaskExecutor;

    @Override
    // 执行当前服务方法并返回处理结果
    public void run(ApplicationArguments args) {
        try {
            taskExecutor.init();
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e.getCause());
        }
        try {
            dataFillingTaskExecutor.init();
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e.getCause());
        }
        try {
            dataSyncTaskExecutor.init();
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e.getCause());
        }
    }
}
