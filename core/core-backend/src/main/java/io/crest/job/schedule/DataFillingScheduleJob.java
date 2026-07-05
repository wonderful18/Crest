package io.crest.job.schedule;

import io.crest.utils.CommonBeanFactory;
import io.crest.utils.LogUtil;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DataFillingScheduleJob implements Job {


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Trigger trigger = jobExecutionContext.getTrigger();
        JobKey jobKey = trigger.getJobKey();
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        DataFillingTaskExecutor taskExecutor = CommonBeanFactory.getBean(DataFillingTaskExecutor.class);
        assert taskExecutor != null;
        try {
            boolean taskLoaded = taskExecutor.execute(jobDataMap);
            if (!taskLoaded) {
                Objects.requireNonNull(CommonBeanFactory.getBean(ScheduleManager.class)).removeJob(jobKey, trigger.getKey());
            }
        } catch (RuntimeException e) {
            LogUtil.error(e.getMessage(), e.getCause());
        }
    }
}
