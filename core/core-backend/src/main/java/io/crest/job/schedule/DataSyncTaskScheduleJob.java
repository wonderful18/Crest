package io.crest.job.schedule;

import io.crest.utils.CommonBeanFactory;
import io.crest.utils.LogUtil;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
// 定义当前业务对象的核心职责
public class DataSyncTaskScheduleJob implements Job {


    @Override
    // 执行当前业务请求并写入处理结果
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        DataSyncTaskExecutor taskExecutor = CommonBeanFactory.getBean(DataSyncTaskExecutor.class);
        assert taskExecutor != null;
        try {
            taskExecutor.execute(jobDataMap);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e.getCause());
        }
    }
}
