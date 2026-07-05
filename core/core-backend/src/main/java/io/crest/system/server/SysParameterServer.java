package io.crest.system.server;

import io.crest.api.system.SysParameterApi;
import io.crest.api.system.request.SQLBotConfigCreator;
import io.crest.api.system.vo.SQLBotConfigVO;
import io.crest.api.system.vo.SettingItemVO;
import io.crest.api.system.vo.ShareBaseVO;
import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.constant.StaticResourceConstants;
import io.crest.constant.SystemSettingConstants;
import io.crest.exception.CrestException;
import io.crest.log.CrestAudit;
import io.crest.system.dao.auto.entity.CoreSysSetting;
import io.crest.system.manage.SysParameterManage;
import io.crest.utils.CrestPermissionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/sys-parameter")
public class SysParameterServer implements SysParameterApi {

    private static final Pattern CUSTOM_I18N_FILE_PATTERN = Pattern.compile("^custom_(.+)_front_(.+)\\.js$");

    @Resource
    private SysParameterManage sysParameterManage;

    @Value("${crest.internal-lite.enabled:false}")
    private boolean internalLiteEnabled;

    @Value("${crest.path.i18n.front:}")
    private String i18nFrontPath;

    @Override
    public String singleVal(String key) {
        CrestPermissionUtils.requireAdmin();
        return sysParameterManage.singleVal(key);
    }

    @Override
    public List<SettingItemVO> queryBasicSetting() {
        CrestPermissionUtils.requireAdmin();
        return basicSettingItems();
    }

    @Override
    @CrestAudit(ot = LogOT.MODIFY, st = LogST.DATA)
    public void saveBasicSetting(@RequestBody List<SettingItemVO> settingItemVOS) {
        CrestPermissionUtils.requireAdmin();
        sysParameterManage.saveBasic(settingItemVOS);
    }

    @Override
    public Integer RequestTimeOut() {
        int frontTimeOut = 60;
        List<SettingItemVO> settingItemVOS = basicSettingItems();
        for (SettingItemVO settingItemVO : settingItemVOS) {
            if (StringUtils.isNotBlank(settingItemVO.getPkey()) && settingItemVO.getPkey().equalsIgnoreCase(SystemSettingConstants.Front_Time_Out) && StringUtils.isNotBlank(settingItemVO.getPval())) {
                frontTimeOut = Integer.parseInt(settingItemVO.getPval());
            }
        }
        return frontTimeOut;
    }

    @Override
    public Map<String, Object> defaultSettings() {
        Map<String, Object> map = new HashMap<>();
        map.put(SystemSettingConstants.DEFAULT_SORT, "1");

        List<SettingItemVO> settingItemVOS = basicSettingItems();
        for (SettingItemVO settingItemVO : settingItemVOS) {
            if (StringUtils.isNotBlank(settingItemVO.getPkey()) && settingItemVO.getPkey().equalsIgnoreCase(SystemSettingConstants.DEFAULT_SORT) && StringUtils.isNotBlank(settingItemVO.getPval())) {
                map.put(SystemSettingConstants.DEFAULT_SORT, settingItemVO.getPval());
            }
            if (StringUtils.isNotBlank(settingItemVO.getPkey()) && settingItemVO.getPkey().equalsIgnoreCase(SystemSettingConstants.DEFAULT_OPEN) && StringUtils.isNotBlank(settingItemVO.getPval())) {
                map.put(SystemSettingConstants.DEFAULT_OPEN, settingItemVO.getPval());
            }
        }
        return map;
    }

    private List<SettingItemVO> basicSettingItems() {
        String key = "basic.";
        List<CoreSysSetting> coreSysSettings = sysParameterManage.groupList(key);
        return sysParameterManage.convert(coreSysSettings);
    }

    @Override
    public List<Object> ui() {
        return sysParameterManage.getUiList();
    }

    @Override
    public Integer defaultLogin() {
        return sysParameterManage.defaultLogin();
    }

    @Override
    public ShareBaseVO shareBase() {
        return sysParameterManage.shareBase();
    }

    @Override
    public Map<String, String> i18nOptions() {
        File dir = resolveI18nFrontDir();
        File[] files;
        if (dir == null || !dir.isDirectory() || ObjectUtils.isEmpty(files = dir.listFiles())) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String name = file.getName();
            Matcher matcher = CUSTOM_I18N_FILE_PATTERN.matcher(name);
            if (!matcher.matches()) {
                continue;
            }
            result.put(matcher.group(1).replace("_", "-"), matcher.group(2));
        }
        return result;
    }

    private File resolveI18nFrontDir() {
        String path = StringUtils.isBlank(i18nFrontPath) ? StaticResourceConstants.I18N_DIR : i18nFrontPath;
        if (path.startsWith(StaticResourceConstants.FILE_PROTOCOL)) {
            path = path.substring(StaticResourceConstants.FILE_PROTOCOL.length());
        }
        try {
            return Path.of(path).toAbsolutePath().normalize().toFile().getCanonicalFile();
        } catch (InvalidPathException | IOException e) {
            return null;
        }
    }

    @Override
    public SQLBotConfigVO sqlBotConfig() {
        CrestPermissionUtils.requireAdmin();
        assertFullModeFeature();
        String key = "sqlbot.";
        List<CoreSysSetting> coreSysSettings = sysParameterManage.groupList(key);
        if (CollectionUtils.isNotEmpty(coreSysSettings)) {
            SQLBotConfigVO vo = new SQLBotConfigVO();
            coreSysSettings.forEach(sysSetting -> {
                if (sysSetting.getPkey().equalsIgnoreCase(key + "domain")) {
                    vo.setDomain(sysSetting.getPval());
                } else if (sysSetting.getPkey().equalsIgnoreCase(key + "id")) {
                    vo.setId(sysSetting.getPval());
                } else if (sysSetting.getPkey().equalsIgnoreCase(key + "enabled")) {
                    vo.setEnabled(StringUtils.isNotBlank(sysSetting.getPval()) && Strings.CI.equals(sysSetting.getPval(), "true"));
                } else if (sysSetting.getPkey().equalsIgnoreCase(key + "valid")) {
                    vo.setValid(StringUtils.isNotBlank(sysSetting.getPval()) && Strings.CI.equals(sysSetting.getPval(), "true"));
                }
            });
            return vo;
        }
        return null;
    }

    @Override
    @CrestAudit(ot = LogOT.MODIFY, st = LogST.DATA)
    public void saveSqlBotConfig(@RequestBody SQLBotConfigCreator creator) {
        CrestPermissionUtils.requireAdmin();
        assertFullModeFeature();
        sysParameterManage.saveSqlBotConfig(creator);
    }

    private void assertFullModeFeature() {
        if (internalLiteEnabled) {
            CrestException.throwException("当前版本未启用该功能");
        }
    }
}
