package io.crest.msgCenter;

import io.crest.api.msgCenter.MsgCenterApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/msg-center")
// 定义当前业务对象的核心职责
public class MsgCenterServer implements MsgCenterApi {
    @Override
    // 执行当前服务方法并返回处理结果
    public long count() {
        return 0;
    }
}
