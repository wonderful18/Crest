package io.crest.traffic.starter;

import io.crest.traffic.dao.mapper.CoreApiTrafficMapper;
import io.crest.utils.LogUtil;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CrestTrafficStarter implements ApplicationRunner {

    @Resource
    private CoreApiTrafficMapper coreApiTrafficMapper;
    @Override
    public void run(ApplicationArguments args) {
        try {
            coreApiTrafficMapper.cleanTraffic();
        } catch (RuntimeException e) {
            LogUtil.error(e.getMessage(), new Throwable(e));
        }
    }
}
