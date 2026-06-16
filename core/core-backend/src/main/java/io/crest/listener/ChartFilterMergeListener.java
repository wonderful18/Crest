package io.crest.listener;

import io.crest.chart.manage.ChartViewOldDataMergeService;
import io.crest.startup.dao.auto.entity.CoreSysStartupJob;
import io.crest.startup.dao.auto.mapper.CoreSysStartupJobMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
@Order(value = 4)
// 定义当前业务对象的核心职责
public class ChartFilterMergeListener implements ApplicationListener<ApplicationReadyEvent> {
    private final Logger logger = LoggerFactory.getLogger(ChartFilterMergeListener.class);
    public static final String JOB_ID = "chartFilterMerge";
    @Resource
    private CoreSysStartupJobMapper coreSysStartupJobMapper;
    @Resource
    private ChartViewOldDataMergeService chartViewOldDataMergeService;

    @Override
    // 执行当前服务方法并返回处理结果
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.info("====chart filter merge [start]====");

        CoreSysStartupJob sysStartupJob = coreSysStartupJobMapper.selectById(JOB_ID);
        if (ObjectUtils.isNotEmpty(sysStartupJob) && Strings.CI.equals(sysStartupJob.getStatus(), "ready")) {
            logger.info("====chart filter merge [doing]====");

            chartViewOldDataMergeService.mergeOldData();

            sysStartupJob.setStatus("done");
            coreSysStartupJobMapper.updateById(sysStartupJob);
        }
        logger.info("====chart filter merge [end]====");
    }
}
