package io.crest.operation.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.crest.commons.constants.OptConstants;
import io.crest.operation.dao.auto.entity.CoreOptRecent;
import io.crest.operation.dao.auto.mapper.CoreOptRecentMapper;
import io.crest.utils.AuthUtils;
import io.crest.utils.IDUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
/**
 * 最近操作记录管理器
 */
public class CoreOptRecentManage {

    /**
     * 最近操作记录数据访问对象
     */
    @Autowired
    private CoreOptRecentMapper coreStoreMapper;

    /**
     * 按资源 ID 保存最近操作
     */
    public void saveOpt(Long resourceId, int resourceType, int optType) {
        saveOpt(resourceId, null, resourceType, optType);
    }

    /**
     * 按资源名称保存最近操作
     */
    public void saveOpt(String resourceName, int resourceType, int optType) {
        saveOpt(null, resourceName, resourceType, optType);
    }

    /**
     * 保存或更新当前用户的最近操作记录
     */
    public void saveOpt(Long resourceId, String resourceName, int resourceType, int optType) {
        if (AuthUtils.getUser() == null) {
            return;
        }
        Long uid = AuthUtils.getUser().getUserId();
        QueryWrapper<CoreOptRecent> updateWrapper = new QueryWrapper<>();
        if (resourceId != null) {
            updateWrapper.eq("resource_id", resourceId);
        }
        if (StringUtils.isNotEmpty(resourceName)) {
            updateWrapper.eq("resource_name", resourceName);
        }
        updateWrapper.eq("resource_type", resourceType);
        updateWrapper.eq("uid", uid);
        CoreOptRecent updateParam = new CoreOptRecent();
        updateParam.setOptType(optType);
        updateParam.setTime(System.currentTimeMillis());
        if (coreStoreMapper.update(updateParam, updateWrapper) == 0) {
            CoreOptRecent optRecent = new CoreOptRecent();
            optRecent.setId(IDUtils.snowID());
            optRecent.setResourceId(resourceId);
            optRecent.setResourceName(resourceName);
            optRecent.setResourceType(resourceType);
            optRecent.setOptType(optType);
            optRecent.setTime(System.currentTimeMillis());
            optRecent.setUid(AuthUtils.getUser().getUserId());
            coreStoreMapper.insert(optRecent);
        }
    }

    /**
     * 查询当前用户最近使用模板的时间映射
     */
    public Map<String, Long> findTemplateRecentUseTime() {
        Long uid = AuthUtils.getUser().getUserId();
        QueryWrapper<CoreOptRecent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("resource_type", OptConstants.OPT_RESOURCE_TYPE.TEMPLATE);
        queryWrapper.eq("uid", uid);
        List<CoreOptRecent> result = coreStoreMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(result)) {
            return result.stream().collect(Collectors.toMap(CoreOptRecent::getResourceName, CoreOptRecent::getTime));
        } else {
            return new HashMap<>();
        }
    }

}
