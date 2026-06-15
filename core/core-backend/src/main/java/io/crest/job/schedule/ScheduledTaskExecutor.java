package io.crest.job.schedule;

import io.crest.commons.utils.CronUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component("scheduledTaskExecutor")
// 封装报表任务、临时任务、重试任务和阈值任务调度操作。
public class ScheduledTaskExecutor {

    protected static final String IS_TEMP_TASK = "isTempTask";
    protected static final String IS_RETRY_TASK = "isRetryTask";

    private static final String JOB_GROUP = "REPORT_TASK";
    private static final String RETRY_JOB_GROUP = "RETRY_REPORT_TASK";
    private static final String TEMP_JOB_GROUP = "TEMP_REPORT_TASK";

    private static final String THRESHOLD_JOB_GROUP = "THRESHOLD_TASK";

    @Resource
    private ScheduleManager scheduleManager;

    // 执行调度任务占位入口。
    public boolean execute(Map<String, Object> taskData) {
        return false;
    }

    // 初始化调度执行器。
    public void init() {
    }

    // 添加阈值检测调度任务。
    public void addThresholdTask(Long taskId, String cron) {
        String key = taskId.toString();
        JobKey jobKey = new JobKey(key, THRESHOLD_JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(key, THRESHOLD_JOB_GROUP);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("taskId", taskId);
        jobDataMap.put("threshold", taskId);
        Date startDate = new Date();
        scheduleManager.addOrUpdateCronJob(jobKey, triggerKey, ScheduledTaskJob.class, cron, startDate, null, jobDataMap);
    }

    // 新增或更新常规报表任务。
    public void addOrUpdateTask(Long taskId, String cron, Long startTime, Long endTime) {
        if (CronUtils.taskExpire(endTime)) {
            return;
        }
        String key = taskId.toString();
        JobKey jobKey = new JobKey(key, JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(key, JOB_GROUP);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("taskId", taskId);
        jobDataMap.put(IS_TEMP_TASK, false);
        Date end = null;
        if (ObjectUtils.isNotEmpty(endTime)) end = new Date(endTime);
        scheduleManager.addOrUpdateCronJob(jobKey, triggerKey, ScheduledTaskJob.class, cron, new Date(startTime), end, jobDataMap);
    }

    // 添加失败重试调度任务。
    public void addRetryTask(Long taskId, Integer retryLimit, Integer retryInterval, Object retryParam) {
        long saltTime = 3000L;
        long interval = retryInterval == null ? 5L : retryInterval;
        long intervalMill = interval * 60000L;
        long now = System.currentTimeMillis();
        String cron = "0 */" + interval + " * * * ?";
        long endTime = (retryLimit + 1) * intervalMill + now - saltTime;
        String key = taskId.toString();
        if (CronUtils.taskExpire(endTime)) {
            return;
        }
        JobKey jobKey = new JobKey(key, RETRY_JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(key, RETRY_JOB_GROUP);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("taskId", taskId);
        jobDataMap.put(IS_RETRY_TASK, true);
        jobDataMap.put("retryParam", retryParam);
        Date end = null;
        if (ObjectUtils.isNotEmpty(endTime)) end = new Date(endTime);
        scheduleManager.addOrUpdateCronJob(jobKey, triggerKey, ScheduledTaskJob.class, cron, new Date(now), end, jobDataMap);
    }

    // 立即触发常规报表任务。
    public boolean fireNow(Long taskId) throws Exception {
        String key = taskId.toString();
        JobKey jobKey = new JobKey(key, JOB_GROUP);
        if (scheduleManager.exist(jobKey)) {
            scheduleManager.fireNow(jobKey);
            return true;
        }
        return false;
    }

    // 添加一次性临时调度任务。
    public void addTempTask(Long taskId, Long startTime) {
        String key = taskId.toString();
        JobKey jobKey = new JobKey(key, TEMP_JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(key, TEMP_JOB_GROUP);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(IS_TEMP_TASK, true);
        String cron = CronUtils.tempCron();
        jobDataMap.put("taskId", taskId);
        scheduleManager.addOrUpdateCronJob(jobKey, triggerKey, ScheduledTaskJob.class, cron, new Date(startTime), null, jobDataMap);
    }

    // 移除常规或临时调度任务。
    public void removeTask(Long taskId, boolean isTemp) {
        String key = taskId.toString();
        JobKey jobKey = new JobKey(key, isTemp ? TEMP_JOB_GROUP : JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(key, isTemp ? TEMP_JOB_GROUP : JOB_GROUP);
        scheduleManager.removeJob(jobKey, triggerKey);
    }

    // 移除重试调度任务。
    public void removeRetryTask(Long taskId) {
        String key = taskId.toString();
        JobKey jobKey = new JobKey(key, RETRY_JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(key, RETRY_JOB_GROUP);
        scheduleManager.removeJob(jobKey, triggerKey);
    }

    // 移除阈值检测调度任务。
    public void removeThresholdTask(Long taskId) {
        String key = taskId.toString();
        JobKey jobKey = new JobKey(key, THRESHOLD_JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(key, THRESHOLD_JOB_GROUP);
        scheduleManager.removeJob(jobKey, triggerKey);
    }

    // 清空全部重试调度任务。
    public void clearRetryTask() throws Exception {
        scheduleManager.clearByGroup(RETRY_JOB_GROUP);
    }
}
