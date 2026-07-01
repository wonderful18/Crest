package io.crest.visualization.server;

import com.fasterxml.jackson.core.type.TypeReference;
import io.crest.api.visualization.VisualizationLinkJumpApi;
import io.crest.api.visualization.dto.VisualizationComponentDTO;
import io.crest.api.visualization.dto.VisualizationLinkJumpDTO;
import io.crest.api.visualization.dto.VisualizationLinkJumpInfoDTO;
import io.crest.api.visualization.request.VisualizationLinkJumpBaseRequest;
import io.crest.api.visualization.response.VisualizationLinkJumpBaseResponse;
import io.crest.api.visualization.vo.VisualizationOutParamsJumpVO;
import io.crest.api.visualization.vo.VisualizationViewTableVO;
import io.crest.auth.CrestLinkPermit;
import io.crest.chart.dao.auto.entity.CoreChartView;
import io.crest.chart.dao.auto.mapper.CoreChartViewMapper;
import io.crest.constant.CommonConstants;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.portal.DataPortalPermissionManage;
import io.crest.utils.AuthUtils;
import io.crest.utils.BeanUtils;
import io.crest.utils.IDUtils;
import io.crest.utils.JsonUtil;
import io.crest.utils.ModelUtils;
import io.crest.visualization.dao.auto.entity.*;
import io.crest.visualization.dao.auto.mapper.*;
import io.crest.visualization.dao.ext.mapper.ExtVisualizationLinkJumpMapper;
import io.crest.visualization.dao.ext.mapper.ExtVisualizationLinkageMapper;
import io.crest.visualization.manage.CoreVisualizationManage;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 维护可视化组件跳转配置，兼容编辑态和分享快照的查询场景。
 */
@RestController
@RequestMapping("link-jump")
public class VisualizationLinkJumpService implements VisualizationLinkJumpApi {

    @Resource
    private ExtVisualizationLinkageMapper extVisualizationLinkageMapper;

    @Resource
    private ExtVisualizationLinkJumpMapper extVisualizationLinkJumpMapper;

    @Resource
    private VisualizationLinkJumpMapper visualizationLinkJumpMapper;

    @Resource
    private VisualizationLinkJumpInfoMapper visualizationLinkJumpInfoMapper;

    @Resource
    private VisualizationLinkJumpTargetViewInfoMapper visualizationLinkJumpTargetViewInfoMapper;

    @Resource
    private SnapshotVisualizationLinkJumpMapper snapshotVisualizationLinkJumpMapper;

    @Resource
    private SnapshotVisualizationLinkJumpInfoMapper snapshotVisualizationLinkJumpInfoMapper;

    @Resource
    private SnapshotVisualizationLinkJumpTargetViewInfoMapper snapshotVisualizationLinkJumpTargetViewInfoMapper;

    @Resource
    private CoreChartViewMapper coreChartViewMapper;

    @Resource
    private SnapshotCoreChartViewMapper snapshotCoreChartViewMapper;

    @Resource
    private CoreVisualizationManage coreVisualizationManage;

    @Resource
    private DataPortalPermissionManage dataPortalPermissionManage;


    @Resource
    private DataVisualizationInfoMapper dataVisualizationInfoMapper;

    @Resource
    private SnapshotDataVisualizationInfoMapper snapshotDataVisualizationInfoMapper;

    @Override
    public List<DatasetTableFieldDTO> getTableFieldWithViewId(Long viewId) {
        requireViewRead(viewId);
        return extVisualizationLinkageMapper.queryTableFieldWithViewId(viewId);
    }

    @CrestLinkPermit
    // 查询仪表板或大屏的组件跳转配置。
    @Override
    public VisualizationLinkJumpBaseResponse queryVisualizationJumpInfo(Long dvId, String resourceTable) {
        requireVisualizationRead(dvId);
        Map<String, VisualizationLinkJumpInfoDTO> resultBase = new HashMap<>();
        List<VisualizationLinkJumpDTO> resultLinkJumpList = null;
        if (CommonConstants.RESOURCE_TABLE.SNAPSHOT.equals(resourceTable)) {
            resultLinkJumpList = extVisualizationLinkJumpMapper.queryWithDvIdSnapshot(dvId, AuthUtils.getUser().getUserId(), ModelUtils.isDesktop());
        } else {
            resultLinkJumpList = extVisualizationLinkJumpMapper.queryWithDvId(dvId, AuthUtils.getUser().getUserId(), ModelUtils.isDesktop());
        }
        Optional.ofNullable(resultLinkJumpList).orElse(new ArrayList<>()).forEach(resultLinkJump -> {
            if (resultLinkJump.getChecked()) {
                Long sourceViewId = resultLinkJump.getSourceViewId();
                Optional.ofNullable(resultLinkJump.getLinkJumpInfoArray()).orElse(new ArrayList<>()).forEach(linkJumpInfo -> {
                    if (linkJumpInfo.getChecked()) {
                        String sourceJumpInfo = sourceViewId + "#" + linkJumpInfo.getSourceFieldId();
                        // 内部仪表板跳转 需要设置好仪表板ID
                        if ("inner".equals(linkJumpInfo.getLinkType())) {
                            if (linkJumpInfo.getTargetDvId() != null) {
                                resultBase.put(sourceJumpInfo, linkJumpInfo);
                            }
                        } else {
                            // 外部跳转
                            resultBase.put(sourceJumpInfo, linkJumpInfo);
                        }
                    }
                });
            }
        });
        return new VisualizationLinkJumpBaseResponse(resultBase, null);
    }

    @Override
    public VisualizationLinkJumpDTO queryWithViewId(Long dvId, Long viewId) {
        requireVisualizationRead(dvId);
        return extVisualizationLinkJumpMapper.queryWithViewId(dvId, viewId, AuthUtils.getUser().getUserId(), ModelUtils.isDesktop());
    }

    @Transactional
    @Override
    public void updateJumpSet(@RequestBody VisualizationLinkJumpDTO jumpDTO) {
        requireVisualizationManage(jumpDTO.getSourceDvId());
        Long dvId = jumpDTO.getSourceDvId();
        Long viewId = jumpDTO.getSourceViewId();
        Assert.notNull(dvId, "dvId cannot be null");
        Assert.notNull(viewId, "viewId cannot be null");
        //清理原有数据
        extVisualizationLinkJumpMapper.deleteJumpTargetViewInfoSnapshot(dvId, viewId);
        extVisualizationLinkJumpMapper.deleteJumpInfoSnapshot(dvId, viewId);
        extVisualizationLinkJumpMapper.deleteJumpSnapshot(dvId, viewId);

        // 插入新的数据
        Long linkJumpId = IDUtils.snowID();
        jumpDTO.setId(linkJumpId);
        SnapshotVisualizationLinkJump insertParam = new SnapshotVisualizationLinkJump();
        BeanUtils.copyBean(insertParam, jumpDTO);
        snapshotVisualizationLinkJumpMapper.insert(insertParam);
        Optional.ofNullable(jumpDTO.getLinkJumpInfoArray()).orElse(new ArrayList<>()).forEach(linkJumpInfo -> {
            Long linkJumpInfoId = IDUtils.snowID();
            linkJumpInfo.setId(linkJumpInfoId);
            linkJumpInfo.setLinkJumpId(linkJumpId);
            SnapshotVisualizationLinkJumpInfo insertJumpInfoParam = new SnapshotVisualizationLinkJumpInfo();
            BeanUtils.copyBean(insertJumpInfoParam, linkJumpInfo);
            snapshotVisualizationLinkJumpInfoMapper.insert(insertJumpInfoParam);
            Optional.ofNullable(linkJumpInfo.getTargetViewInfoList()).orElse(new ArrayList<>()).forEach(targetViewInfo -> {
                Long targetViewInfoId = IDUtils.snowID();
                targetViewInfo.setTargetId(targetViewInfoId);
                targetViewInfo.setLinkJumpInfoId(linkJumpInfoId);
                SnapshotVisualizationLinkJumpTargetViewInfo insertTargetViewInfoParam = new SnapshotVisualizationLinkJumpTargetViewInfo();
                BeanUtils.copyBean(insertTargetViewInfoParam, targetViewInfo);
                snapshotVisualizationLinkJumpTargetViewInfoMapper.insert(insertTargetViewInfoParam);
            });
        });
    }

    @CrestLinkPermit("#p0.targetDvId")
    @Override
    public VisualizationLinkJumpBaseResponse queryTargetVisualizationJumpInfo(@RequestBody VisualizationLinkJumpBaseRequest request) {
        requireVisualizationRead(request.getTargetDvId());
        List<VisualizationLinkJumpDTO> result = null;
        if (CommonConstants.RESOURCE_TABLE.SNAPSHOT.equals(request.getResourceTable())) {
            result = extVisualizationLinkJumpMapper.getTargetVisualizationJumpInfoSnapshot(request);
        } else {
            result = extVisualizationLinkJumpMapper.getTargetVisualizationJumpInfo(request);
        }
        return new VisualizationLinkJumpBaseResponse(null, Optional.ofNullable(result).orElse(new ArrayList<>()).stream().filter(item -> StringUtils.isNotEmpty(item.getSourceInfo())).collect(Collectors.toMap(VisualizationLinkJumpDTO::getSourceInfo, VisualizationLinkJumpDTO::getTargetInfoList)));
    }

    @Override
    public VisualizationComponentDTO viewTableDetailList(Long dvId, String resourceTable) {
        requireVisualizationRead(dvId);
        List<VisualizationViewTableVO> result = new ArrayList<>();
        List<VisualizationOutParamsJumpVO> outParamsJumpInfo = new ArrayList<>();
        String componentData = "[]";
        String resolvedResourceTable = StringUtils.defaultIfBlank(resourceTable, CommonConstants.RESOURCE_TABLE.SNAPSHOT);
        if (CommonConstants.RESOURCE_TABLE.SNAPSHOT.equals(resolvedResourceTable)) {
            SnapshotDataVisualizationInfo dvInfo = snapshotDataVisualizationInfoMapper.selectById(dvId);
            if (dvInfo != null) {
                componentData = StringUtils.defaultIfBlank(dvInfo.getComponentData(), "[]");
                result = filterViewTables(componentData, extVisualizationLinkJumpMapper.getViewTableDetailsSnapshot(dvId));
                outParamsJumpInfo = Optional.ofNullable(extVisualizationLinkJumpMapper.queryOutParamsTargetWithDvIdSnapshot(dvId)).orElse(new ArrayList<>());
            }
        } else {
            DataVisualizationInfo dvInfo = dataVisualizationInfoMapper.selectById(dvId);
            if (dvInfo != null) {
                componentData = StringUtils.defaultIfBlank(dvInfo.getComponentData(), "[]");
                result = filterViewTables(componentData, extVisualizationLinkJumpMapper.getViewTableDetails(dvId));
                outParamsJumpInfo = Optional.ofNullable(extVisualizationLinkJumpMapper.queryOutParamsTargetWithDvId(dvId)).orElse(new ArrayList<>());
            }
        }
        return new VisualizationComponentDTO(componentData, result, outParamsJumpInfo);

    }

    static List<VisualizationViewTableVO> filterViewTables(String componentData, List<VisualizationViewTableVO> tables) {
        Set<String> viewIds = componentViewIds(componentData);
        return Optional.ofNullable(tables).orElse(new ArrayList<>()).stream()
                .filter(viewTableInfo -> viewTableInfo.getId() != null)
                .filter(viewTableInfo -> viewIds.isEmpty()
                        ? componentData.contains(viewTableInfo.getId().toString())
                        : viewIds.contains(viewTableInfo.getId().toString()))
                .collect(Collectors.toList());
    }

    static Set<String> componentViewIds(String componentData) {
        List<Map<String, Object>> components = JsonUtil.parseObject(componentData, new TypeReference<List<Map<String, Object>>>() {});
        if (components == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        collectComponentViewIds(components, result);
        return result;
    }

    private static void collectComponentViewIds(Object value, Set<String> result) {
        if (value instanceof List<?> list) {
            list.forEach(item -> collectComponentViewIds(item, result));
            return;
        }
        if (!(value instanceof Map<?, ?> component)) {
            return;
        }
        Object componentType = component.get("component");
        Object id = component.get("id");
        if ("UserView".equals(componentType) && !"VQuery".equals(component.get("innerType")) && id != null) {
            result.add(String.valueOf(id));
        }
        if ("Group".equals(componentType)) {
            collectComponentViewIds(component.get("propValue"), result);
        } else if ("Tabs".equals(componentType) && component.get("propValue") instanceof List<?> tabs) {
            tabs.forEach(tab -> {
                if (tab instanceof Map<?, ?> tabItem) {
                    collectComponentViewIds(tabItem.get("componentData"), result);
                }
            });
        }
    }

    @Override
    public VisualizationLinkJumpBaseResponse updateJumpSetActive(@RequestBody VisualizationLinkJumpBaseRequest request) {
        requireVisualizationManage(request.getSourceDvId());
        SnapshotCoreChartView coreChartView = new SnapshotCoreChartView();
        coreChartView.setId(Long.valueOf(request.getSourceViewId()));
        coreChartView.setJumpActive(request.getActiveStatus());
        snapshotCoreChartViewMapper.updateById(coreChartView);
        return queryVisualizationJumpInfo(request.getSourceDvId(), CommonConstants.RESOURCE_TABLE.SNAPSHOT);
    }

    @Override
    public void deleteJumpSet(@RequestBody VisualizationLinkJumpDTO jumpDTO) {
        requireVisualizationManage(jumpDTO.getSourceDvId());
        //清理原有数据
        extVisualizationLinkJumpMapper.deleteJumpTargetViewInfoSnapshot(jumpDTO.getSourceDvId(), jumpDTO.getSourceViewId());
        extVisualizationLinkJumpMapper.deleteJumpInfoSnapshot(jumpDTO.getSourceDvId(), jumpDTO.getSourceViewId());
        extVisualizationLinkJumpMapper.deleteJumpSnapshot(jumpDTO.getSourceDvId(), jumpDTO.getSourceViewId());
    }

    private void requireViewRead(Long viewId) {
        if (viewId == null) {
            coreVisualizationManage.requireVisualizationManage(null);
            return;
        }
        CoreChartView view = coreChartViewMapper.selectById(viewId);
        if (view != null) {
            requireVisualizationRead(view.getSceneId());
            return;
        }
        SnapshotCoreChartView snapshotView = snapshotCoreChartViewMapper.selectById(viewId);
        if (snapshotView != null) {
            requireVisualizationRead(snapshotView.getSceneId());
            return;
        }
        coreVisualizationManage.requireVisualizationManage(null);
    }

    private void requireVisualizationRead(Long dvId) {
        if (dvId == null) {
            coreVisualizationManage.requireVisualizationManage(null);
            return;
        }
        DataVisualizationInfo info = dataVisualizationInfoMapper.selectById(dvId);
        if (info == null) {
            coreVisualizationManage.requireVisualizationManage(null);
            return;
        }
        if (!dataPortalPermissionManage.canReadPublishedVisualization(dvId, info.getType())) {
            coreVisualizationManage.requireVisualizationManage(info);
        }
    }

    private void requireVisualizationManage(Long dvId) {
        DataVisualizationInfo info = dvId == null ? null : dataVisualizationInfoMapper.selectById(dvId);
        coreVisualizationManage.requireVisualizationManage(info);
    }

}
