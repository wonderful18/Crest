package io.crest.job.schedule;

import io.crest.utils.CommonBeanFactory;
import io.crest.utils.LogUtil;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
// 定义当前业务对象的核心职责
public class ScheduledTaskJob implements Job {

    @Override
    // 执行当前业务请求并写入处理结果
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Trigger trigger = jobExecutionContext.getTrigger();
        JobKey jobKey = trigger.getJobKey();
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        ScheduledTaskExecutor taskExecutor = CommonBeanFactory.getBean(ScheduledTaskExecutor.class);
        assert taskExecutor != null;
        try {
            boolean taskLoaded = taskExecutor.execute(jobDataMap);
            if (!taskLoaded) {
                Objects.requireNonNull(CommonBeanFactory.getBean(ScheduleManager.class)).removeJob(jobKey, trigger.getKey());
            }
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e.getCause());
        }
    }
}
