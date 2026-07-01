package io.crest.visualization.server;

import io.crest.api.commons.BaseRspModel;
import io.crest.api.visualization.VisualizationLinkageApi;
import io.crest.api.visualization.dto.LinkageInfoDTO;
import io.crest.api.visualization.dto.VisualizationLinkageDTO;
import io.crest.api.visualization.request.VisualizationLinkageRequest;
import io.crest.api.visualization.vo.VisualizationLinkageFieldVO;
import io.crest.auth.CrestLinkPermit;
import io.crest.chart.dao.auto.entity.CoreChartView;
import io.crest.chart.dao.auto.mapper.CoreChartViewMapper;
import io.crest.constant.CommonConstants;
import io.crest.portal.DataPortalPermissionManage;
import io.crest.utils.BeanUtils;
import io.crest.utils.IDUtils;
import io.crest.visualization.dao.auto.entity.*;
import io.crest.visualization.dao.auto.mapper.*;
import io.crest.visualization.dao.ext.mapper.ExtVisualizationLinkageMapper;
import io.crest.visualization.manage.CoreVisualizationManage;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理可视化组件联动配置，负责联动关系的查询、保存和快照同步。
 */
@RestController
@RequestMapping("linkage")
public class VisualizationLinkageService implements VisualizationLinkageApi {

    @Resource
    private ExtVisualizationLinkageMapper extVisualizationLinkageMapper;

    @Resource
    private VisualizationLinkageFieldMapper visualizationLinkageFieldMapper;

    @Resource
    private SnapshotVisualizationLinkageFieldMapper snapshotVisualizationLinkageFieldMapper;

    @Resource
    private VisualizationLinkageMapper visualizationLinkageMapper;

    @Resource
    private SnapshotVisualizationLinkageMapper snapshotVisualizationLinkageMapper;

    @Resource
    private DataVisualizationInfoMapper dataVisualizationInfoMapper;

    @Resource
    private CoreVisualizationManage coreVisualizationManage;

    @Resource
    private DataPortalPermissionManage dataPortalPermissionManage;

    @Resource
    private CoreChartViewMapper coreChartViewMapper;

    @Resource
    private SnapshotCoreChartViewMapper snapshotCoreChartViewMapper;

    @Override
    @CrestLinkPermit("#p0.dvId")
    public Map<String, VisualizationLinkageDTO> viewLinkageGather(@RequestBody VisualizationLinkageRequest request) {
        requireVisualizationRead(request.getDvId());
        if (CollectionUtils.isNotEmpty(request.getTargetViewIds())) {
            List<VisualizationLinkageDTO> linkageDTOList = null;
            if (CommonConstants.RESOURCE_TABLE.SNAPSHOT.equals(request.getResourceTable())) {
                linkageDTOList = extVisualizationLinkageMapper.viewLinkageGatherSnapshot(request.getDvId(), request.getSourceViewId(), request.getTargetViewIds());
            } else {
                linkageDTOList =  extVisualizationLinkageMapper.viewLinkageGather(request.getDvId(), request.getSourceViewId(), request.getTargetViewIds());
            }
            return linkageDTOList.stream().collect(Collectors.toMap(targetViewId -> String.valueOf(targetViewId), PanelViewLinkageDTO -> PanelViewLinkageDTO));
        }
        return new HashMap<>();
    }

    @Override
    @CrestLinkPermit("#p0.dvId")
    public List<VisualizationLinkageDTO> viewLinkageGatherArray(@RequestBody VisualizationLinkageRequest request) {
        requireVisualizationRead(request.getDvId());
        if (CommonConstants.RESOURCE_TABLE.SNAPSHOT.equals(request.getResourceTable())) {
            return extVisualizationLinkageMapper.viewLinkageGatherSnapshot(request.getDvId(), request.getSourceViewId(), request.getTargetViewIds());
        } else {
            return extVisualizationLinkageMapper.viewLinkageGather(request.getDvId(), request.getSourceViewId(), request.getTargetViewIds());
        }
    }

    @Override
    @Transactional
    public BaseRspModel saveLinkage(@RequestBody VisualizationLinkageRequest request) {
        requireVisualizationManage(request.getDvId());
        // 向镜像中保存
        Long updateTime = System.currentTimeMillis();
        List<VisualizationLinkageDTO> linkageInfo = request.getLinkageInfo();
        Long sourceViewId = request.getSourceViewId();
        Long dvId = request.getDvId();

        Assert.notNull(sourceViewId, "source View ID can not be null");
        Assert.notNull(dvId, "dvId can not be null");

        // 清理原有关系
        extVisualizationLinkageMapper.deleteViewLinkageFieldSnapshot(dvId, sourceViewId);
        extVisualizationLinkageMapper.deleteViewLinkageSnapshot(dvId, sourceViewId);

        //重新建立关系
        for (VisualizationLinkageDTO linkageDTO : linkageInfo) {
            //去掉source view 的信息
            if (sourceViewId.equals(linkageDTO.getTargetViewId())) {
                continue;
            }
            List<VisualizationLinkageFieldVO> linkageFields = linkageDTO.getLinkageFields();
            Long linkageId = IDUtils.snowID();
            SnapshotVisualizationLinkage linkage = new SnapshotVisualizationLinkage();
            linkage.setId(linkageId);
            linkage.setDvId(dvId);
            linkage.setSourceViewId(sourceViewId);
            linkage.setTargetViewId(linkageDTO.getTargetViewId());
            linkage.setUpdatePeople("");
            linkage.setUpdateTime(updateTime);
            linkage.setLinkageActive(linkageDTO.getLinkageActive());
            snapshotVisualizationLinkageMapper.insert(linkage);
            if (CollectionUtils.isNotEmpty(linkageFields) && linkageDTO.getLinkageActive()) {
                linkageFields.forEach(linkageField -> {
                    linkageField.setId(IDUtils.snowID());
                    linkageField.setLinkageId(linkageId);
                    linkageField.setUpdateTime(updateTime);
                    SnapshotVisualizationLinkageField fieldInsert = new SnapshotVisualizationLinkageField();
                    snapshotVisualizationLinkageFieldMapper.insert(BeanUtils.copyBean(fieldInsert, linkageField));
                });
            }
        }
        return new BaseRspModel();
    }

    @CrestLinkPermit
    @Override
    public Map<String, List<String>> getVisualizationAllLinkageInfo(Long dvId, String resourceTable) {
        requireVisualizationRead(dvId);
        List<LinkageInfoDTO> info = null;
        if (CommonConstants.RESOURCE_TABLE.SNAPSHOT.equals(resourceTable)) {
            info = extVisualizationLinkageMapper.getPanelAllLinkageInfoSnapshot(dvId);
        }else{
            info = extVisualizationLinkageMapper.getPanelAllLinkageInfo(dvId);
        }
        return Optional.ofNullable(info).orElse(new ArrayList<>()).stream().collect(Collectors.toMap(LinkageInfoDTO::getSourceInfo, LinkageInfoDTO::getTargetInfoList));
    }

    @Override
    public Map updateLinkageActive(@RequestBody VisualizationLinkageRequest request) {
        requireVisualizationManage(request.getDvId());
        SnapshotCoreChartView coreChartView = new SnapshotCoreChartView();
        coreChartView.setId(request.getSourceViewId());
        coreChartView.setLinkageActive(request.getActiveStatus());
        snapshotCoreChartViewMapper.updateById(coreChartView);
        return getVisualizationAllLinkageInfo(request.getDvId(),CommonConstants.RESOURCE_TABLE.SNAPSHOT);
    }

    @Override
    public void deleteLinkage(@RequestBody VisualizationLinkageRequest request) {
        requireVisualizationManage(request.getDvId());
        // 清理原有关系
        extVisualizationLinkageMapper.deleteViewLinkageFieldSnapshot(request.getDvId(), request.getSourceViewId());
        extVisualizationLinkageMapper.deleteViewLinkageSnapshot(request.getDvId(), request.getSourceViewId());
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
