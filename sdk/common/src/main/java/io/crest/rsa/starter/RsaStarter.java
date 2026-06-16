package io.crest.rsa.starter;

import io.crest.rsa.manage.RsaManage;
import io.crest.utils.LogUtil;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
// 定义当前业务对象的核心职责
public class RsaStarter implements ApplicationRunner {

    @Resource
    private RsaManage rsaManage;

    @Override
    // 执行当前服务方法并返回处理结果
    public void run(ApplicationArguments args) throws Exception {
        checkRsa();
    }

    // 执行当前服务方法并返回处理结果
    private void checkRsa() {
        try {
            rsaManage.check();
        } catch (Exception e) {
            LogUtil.error(e.getMessage());
        }
    }
}
