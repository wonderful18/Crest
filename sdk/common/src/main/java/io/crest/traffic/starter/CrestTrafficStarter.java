package io.crest.traffic.starter;

import io.crest.traffic.dao.mapper.CoreApiTrafficMapper;
import io.crest.utils.LogUtil;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
// 定义当前业务对象的核心职责
public class CrestTrafficStarter implements ApplicationRunner {

    @Resource
    private CoreApiTrafficMapper coreApiTrafficMapper;
    @Override
    // 执行当前服务方法并返回处理结果
    public void run(ApplicationArguments args) throws Exception {
        try {
            coreApiTrafficMapper.cleanTraffic();
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), new Throwable(e));
        }
    }
}
