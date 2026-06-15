package io.crest.system.dao.ext.mapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.crest.system.dao.auto.entity.CoreSysSetting;
import io.crest.system.dao.auto.mapper.CoreSysSettingMapper;
import org.springframework.stereotype.Component;

@Component("extCoreSysSettingMapper")
// 定义当前业务对象的核心职责
public class ExtCoreSysSettingMapper extends ServiceImpl<CoreSysSettingMapper, CoreSysSetting> {
}
