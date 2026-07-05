package io.crest.job.schedule;

import io.crest.utils.CommonBeanFactory;
import io.crest.utils.LogUtil;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
public class DataSyncTaskScheduleJob implements Job {


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        DataSyncTaskExecutor taskExecutor = CommonBeanFactory.getBean(DataSyncTaskExecutor.class);
        assert taskExecutor != null;
        try {
            taskExecutor.execute(jobDataMap);
        } catch (RuntimeException e) {
            LogUtil.error(e.getMessage(), e.getCause());
        }
    }
}
