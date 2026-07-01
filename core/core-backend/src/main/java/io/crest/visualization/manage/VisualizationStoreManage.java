package io.crest.visualization.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.crest.api.visualization.request.VisualizationStoreRequest;
import io.crest.api.visualization.request.VisualizationWorkbranchQueryRequest;
import io.crest.api.visualization.vo.VisualizationStoreVO;
import io.crest.constant.BusiResourceEnum;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.system.manage.CoreUserManage;
import io.crest.utils.AuthUtils;
import io.crest.utils.CommonBeanFactory;
import io.crest.utils.CommunityUtils;
import io.crest.utils.IDUtils;
import io.crest.visualization.dao.auto.entity.CoreStore;
import io.crest.visualization.dao.auto.mapper.CoreStoreMapper;
import io.crest.visualization.dao.ext.mapper.CoreStoreExtMapper;
import io.crest.visualization.dao.ext.po.StorePO;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class VisualizationStoreManage {

    @Resource
    private CoreStoreMapper coreStoreMapper;

    @Resource
    private CoreStoreExtMapper coreStoreExtMapper;

    @Resource
    private CoreUserManage coreUserManage;
    @Resource
    private PlatformPermissionManage platformPermissionManage;
    @Resource
    private JdbcTemplate jdbcTemplate;

    public void execute(VisualizationStoreRequest request) {
        if (request == null) {
            CrestException.throwException("收藏资源不能为空");
        }
        Long resourceId = request.getId();
        BusiResourceEnum busiResourceEnum = busiResourceType(request.getType());
        requireResourceReadAccess(resourceId, busiResourceEnum);
        Long uid = AuthUtils.getUser().getUserId();
        if (favorited(resourceId)) {
            QueryWrapper<CoreStore> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("resource_id", resourceId);
            queryWrapper.eq("uid", uid);
            coreStoreMapper.delete(queryWrapper);
            return;
        }
        CoreStore coreStore = new CoreStore();
        coreStore.setId(IDUtils.snowID());
        coreStore.setTime(System.currentTimeMillis());
        coreStore.setUid(uid);
        coreStore.setResourceId(resourceId);
        coreStore.setResourceType(busiResourceEnum.getFlag());
        coreStoreMapper.insert(coreStore);
    }

    public Boolean favorited(Long resourceId) {
        QueryWrapper<CoreStore> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("resource_id", resourceId);
        queryWrapper.eq("uid", AuthUtils.getUser().getUserId());
        return coreStoreMapper.exists(queryWrapper);
    }
    public IPage<VisualizationStoreVO> query(int pageNum, int pageSize, VisualizationWorkbranchQueryRequest request) {
        IPage<StorePO> storePOIPage = proxy().queryStorePage(pageNum, pageSize, request);
        if (ObjectUtils.isEmpty(storePOIPage)) return null;
        List<VisualizationStoreVO> vos = proxy().formatResult(storePOIPage.getRecords());
        IPage<VisualizationStoreVO> ipage = new Page<>();
        ipage.setCurrent(storePOIPage.getCurrent());
        ipage.setPages(storePOIPage.getPages());
        ipage.setSize(storePOIPage.getSize());
        ipage.setTotal(storePOIPage.getTotal());
        ipage.setRecords(vos);
        return ipage;
    }

    public VisualizationStoreManage proxy() {
        return CommonBeanFactory.getBean(this.getClass());
    }

    public List<VisualizationStoreVO> formatResult(List<StorePO> pos) {
        if (CollectionUtils.isEmpty(pos)) return new ArrayList<>();
        return pos.stream().map(po ->
                new VisualizationStoreVO(
                        po.getStoreId(), po.getResourceId(), po.getName(),
                        po.getType(), coreUserManage.getUserName(po.getCreator()), coreUserManage.getUserName(po.getEditor()),
                        po.getEditTime(), 9, po.getExtFlag(), po.getExtFlag1())).toList();
    }

    public IPage<StorePO> queryStorePage(int goPage, int pageSize, VisualizationWorkbranchQueryRequest request) {
        VisualizationWorkbranchQueryRequest queryRequest = request == null ? new VisualizationWorkbranchQueryRequest() : request;
        Long uid = AuthUtils.getUser().getUserId();
        QueryWrapper<Object> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("s.uid", uid);
        queryWrapper.isNotNull("s.resource_id");
        if (StringUtils.isNotBlank(queryRequest.getType())) {
            BusiResourceEnum busiResourceEnum = busiResourceType(queryRequest.getType());
            queryWrapper.eq("s.resource_type", busiResourceEnum.getFlag());
        }
        if (StringUtils.isNotBlank(queryRequest.getKeyword())) {
            queryWrapper.apply("LOWER(v.name) LIKE LOWER(CONCAT('%', {0}, '%'))", queryRequest.getKeyword());
        }
        String scopeSql = visualizationPermissionScope();
        if (StringUtils.isNotBlank(scopeSql)) {
            queryWrapper.apply(scopeSql);
        }
        String info = CommunityUtils.getInfo();
        if (StringUtils.isNotBlank(info)) {
            queryWrapper.notExists(String.format(info, "s.resource_id"));
        }
        queryWrapper.orderBy(true, queryRequest.isAsc(), "v.update_time");
        Page<StorePO> page = new Page<>(goPage, pageSize);
        return coreStoreExtMapper.query(page, queryWrapper);
    }

    private BusiResourceEnum busiResourceType(String type) {
        if (StringUtils.isBlank(type)) {
            CrestException.throwException("type is invalid");
        }
        try {
            return BusiResourceEnum.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            CrestException.throwException("type is invalid");
            return null;
        }
    }

    private void requireResourceReadAccess(Long resourceId, BusiResourceEnum type) {
        if (resourceId == null) {
            CrestException.throwException("资源ID不能为空");
        }
        ResourceQuery query = resourceQuery(type);
        String scopeSql = platformPermissionManage.resourceScopeSql(query.resourceType(), query.idColumn(), query.creatorColumn(), query.orgColumn());
        String sql = query.sql();
        if (StringUtils.isNotBlank(scopeSql)) {
            sql += " AND " + scopeSql;
        }
        Long count = jdbcTemplate.queryForObject(sql, Long.class, resourceId);
        if (count == null || count == 0) {
            CrestException.throwException("资源不存在或无访问权限");
        }
    }

    private ResourceQuery resourceQuery(BusiResourceEnum type) {
        return switch (type) {
            case PANEL -> new ResourceQuery(
                    "panel",
                    "v.id",
                    "v.create_by",
                    "v.org_id",
                    "SELECT COUNT(1) FROM bi_visualization v WHERE v.id = ? AND v.delete_flag = 0 AND v.node_type = 'leaf' AND v.type = 'dashboard'"
            );
            case SCREEN -> new ResourceQuery(
                    "screen",
                    "v.id",
                    "v.create_by",
                    "v.org_id",
                    "SELECT COUNT(1) FROM bi_visualization v WHERE v.id = ? AND v.delete_flag = 0 AND v.node_type = 'leaf' AND v.type = 'dataV'"
            );
            case DATASET -> new ResourceQuery(
                    "dataset",
                    "dg.id",
                    "dg.create_by",
                    null,
                    "SELECT COUNT(1) FROM dataset_group dg WHERE dg.id = ? AND dg.node_type = 'dataset'"
            );
            case DATASOURCE -> new ResourceQuery(
                    "datasource",
                    "ds.id",
                    "ds.create_by",
                    null,
                    "SELECT COUNT(1) FROM data_source ds WHERE ds.id = ? AND ds.type <> 'folder'"
            );
            default -> {
                CrestException.throwException("type is invalid");
                yield null;
            }
        };
    }

    private String visualizationPermissionScope() {
        String screenScope = platformPermissionManage.resourceScopeSql("screen", "v.id", "v.create_by", "v.org_id");
        String panelScope = platformPermissionManage.resourceScopeSql("panel", "v.id", "v.create_by", "v.org_id");
        if (StringUtils.isBlank(screenScope) && StringUtils.isBlank(panelScope)) {
            return null;
        }
        String effectiveScreenScope = StringUtils.defaultIfBlank(screenScope, "1 = 1");
        String effectivePanelScope = StringUtils.defaultIfBlank(panelScope, "1 = 1");
        return "((v.type = 'dataV' AND " + effectiveScreenScope + ") OR (v.type <> 'dataV' AND " + effectivePanelScope + "))";
    }

    private record ResourceQuery(String resourceType, String idColumn, String creatorColumn, String orgColumn, String sql) {
    }
}
