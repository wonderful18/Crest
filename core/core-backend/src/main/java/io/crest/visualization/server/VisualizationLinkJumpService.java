package io.crest.visualization.server;

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
import io.crest.utils.AuthUtils;
import io.crest.utils.BeanUtils;
import io.crest.utils.IDUtils;
import io.crest.utils.ModelUtils;
import io.crest.visualization.dao.auto.entity.*;
import io.crest.visualization.dao.auto.mapper.*;
import io.crest.visualization.dao.ext.mapper.ExtVisualizationLinkJumpMapper;
import io.crest.visualization.dao.ext.mapper.ExtVisualizationLinkageMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
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

    /**
     * 联动扩展 Mapper，用于查询图表字段。
     */
    @Resource
    private ExtVisualizationLinkageMapper extVisualizationLinkageMapper;

    /**
     * 跳转扩展 Mapper，封装编辑态与快照态复杂查询。
     */
    @Resource
    private ExtVisualizationLinkJumpMapper extVisualizationLinkJumpMapper;

    /**
     * 编辑态跳转主表 Mapper。
     */
    @Resource
    private VisualizationLinkJumpMapper visualizationLinkJumpMapper;

    /**
     * 编辑态跳转明细 Mapper。
     */
    @Resource
    private VisualizationLinkJumpInfoMapper visualizationLinkJumpInfoMapper;

    /**
     * 编辑态跳转目标图表 Mapper。
     */
    @Resource
    private VisualizationLinkJumpTargetViewInfoMapper visualizationLinkJumpTargetViewInfoMapper;

    /**
     * 快照态跳转主表 Mapper。
     */
    @Resource
    private SnapshotVisualizationLinkJumpMapper snapshotVisualizationLinkJumpMapper;

    /**
     * 快照态跳转明细 Mapper。
     */
    @Resource
    private SnapshotVisualizationLinkJumpInfoMapper snapshotVisualizationLinkJumpInfoMapper;

    /**
     * 快照态跳转目标图表 Mapper。
     */
    @Resource
    private SnapshotVisualizationLinkJumpTargetViewInfoMapper snapshotVisualizationLinkJumpTargetViewInfoMapper;

    /**
     * 编辑态图表 Mapper。
     */
    @Resource
    private CoreChartViewMapper coreChartViewMapper;

    /**
     * 快照态图表 Mapper。
     */
    @Resource
    private SnapshotCoreChartViewMapper snapshotCoreChartViewMapper;


    /**
     * 可视化主信息 Mapper。
     */
    @Resource
    private DataVisualizationInfoMapper dataVisualizationInfoMapper;

    /**
     * 查询指定图表可用于跳转映射的数据集字段。
     */
    @Override
    public List<DatasetTableFieldDTO> getTableFieldWithViewId(Long viewId) {
        return extVisualizationLinkageMapper.queryTableFieldWithViewId(viewId);
    }

    /**
     * 查询仪表板或大屏的组件跳转配置。
     */
    @CrestLinkPermit
    @Override
    public VisualizationLinkJumpBaseResponse queryVisualizationJumpInfo(Long dvId, String resourceTable) {
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

    /**
     * 查询指定图表的跳转配置。
     */
    @Override
    public VisualizationLinkJumpDTO queryWithViewId(Long dvId, Long viewId) {
        return extVisualizationLinkJumpMapper.queryWithViewId(dvId, viewId, AuthUtils.getUser().getUserId(), ModelUtils.isDesktop());
    }

    /**
     * 更新图表跳转设置，并重建快照态跳转关系。
     */
    @Transactional
    @Override
    public void updateJumpSet(VisualizationLinkJumpDTO jumpDTO) {
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

    /**
     * 查询目标可视化接收到的跳转配置。
     */
    @CrestLinkPermit("#p0.targetDvId")
    @Override
    public VisualizationLinkJumpBaseResponse queryTargetVisualizationJumpInfo(VisualizationLinkJumpBaseRequest request) {
        List<VisualizationLinkJumpDTO> result = null;
        if (CommonConstants.RESOURCE_TABLE.SNAPSHOT.equals(request.getResourceTable())) {
            result = extVisualizationLinkJumpMapper.getTargetVisualizationJumpInfoSnapshot(request);
        } else {
            result = extVisualizationLinkJumpMapper.getTargetVisualizationJumpInfo(request);
        }
        return new VisualizationLinkJumpBaseResponse(null, Optional.ofNullable(result).orElse(new ArrayList<>()).stream().filter(item -> StringUtils.isNotEmpty(item.getSourceInfo())).collect(Collectors.toMap(VisualizationLinkJumpDTO::getSourceInfo, VisualizationLinkJumpDTO::getTargetInfoList)));
    }

    /**
     * 查询大屏内图表明细和外部参数跳转信息。
     */
    @Override
    public VisualizationComponentDTO viewTableDetailList(Long dvId) {
        DataVisualizationInfo dvInfo = dataVisualizationInfoMapper.selectById(dvId);
        List<VisualizationViewTableVO> result;
        List<VisualizationOutParamsJumpVO> outParamsJumpInfo;
        String componentData;
        if (dvInfo != null) {
            result = extVisualizationLinkJumpMapper.getViewTableDetails(dvId).stream().filter(viewTableInfo -> dvInfo.getComponentData().indexOf(viewTableInfo.getId().toString()) > -1).collect(Collectors.toList());
            componentData = dvInfo.getComponentData();
            outParamsJumpInfo = extVisualizationLinkJumpMapper.queryOutParamsTargetWithDvId(dvId);
        } else {
            result = new ArrayList<>();
            outParamsJumpInfo = new ArrayList<>();
            componentData = "[]";
        }
        return new VisualizationComponentDTO(componentData, result, outParamsJumpInfo);

    }

    /**
     * 更新图表跳转启用状态并返回最新配置。
     */
    @Override
    public VisualizationLinkJumpBaseResponse updateJumpSetActive(VisualizationLinkJumpBaseRequest request) {
        SnapshotCoreChartView coreChartView = new SnapshotCoreChartView();
        coreChartView.setId(Long.valueOf(request.getSourceViewId()));
        coreChartView.setJumpActive(request.getActiveStatus());
        snapshotCoreChartViewMapper.updateById(coreChartView);
        return queryVisualizationJumpInfo(request.getSourceDvId(), CommonConstants.RESOURCE_TABLE.SNAPSHOT);
    }

    /**
     * 删除指定图表的跳转设置。
     */
    @Override
    public void deleteJumpSet(VisualizationLinkJumpDTO jumpDTO) {
        //清理原有数据
        extVisualizationLinkJumpMapper.deleteJumpTargetViewInfoSnapshot(jumpDTO.getSourceDvId(), jumpDTO.getSourceViewId());
        extVisualizationLinkJumpMapper.deleteJumpInfoSnapshot(jumpDTO.getSourceDvId(), jumpDTO.getSourceViewId());
        extVisualizationLinkJumpMapper.deleteJumpSnapshot(jumpDTO.getSourceDvId(), jumpDTO.getSourceViewId());
    }

}
