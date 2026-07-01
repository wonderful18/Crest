package io.crest.dataset.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.chart.dao.auto.entity.CoreChartView;
import io.crest.chart.dao.auto.mapper.CoreChartViewMapper;
import io.crest.dataset.dao.auto.entity.CoreDatasetTableField;
import io.crest.dataset.dao.auto.mapper.CoreDatasetTableFieldMapper;
import io.crest.dataset.utils.DatasetUtils;
import io.crest.dataset.utils.TableUtils;
import io.crest.engine.constant.ExtFieldConstant;
import io.crest.engine.func.FunctionConstant;
import io.crest.engine.utils.Utils;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.api.PluginManageApi;
import io.crest.extensions.datasource.dto.CalParam;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.dto.FieldGroupDTO;
import io.crest.extensions.datasource.model.SQLObj;
import io.crest.extensions.view.dto.ColumnPermissionItem;
import io.crest.i18n.Translator;
import io.crest.utils.AuthUtils;
import io.crest.utils.BeanUtils;
import io.crest.utils.CrestPermissionUtils;
import io.crest.utils.IDUtils;
import io.crest.utils.JsonUtil;
import io.crest.visualization.dao.auto.entity.DataVisualizationInfo;
import io.crest.visualization.dao.auto.mapper.DataVisualizationInfoMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Transactional
@SuppressWarnings("unchecked")
public class DatasetTableFieldManage {
    @Resource
    private CoreDatasetTableFieldMapper coreDatasetTableFieldMapper;
    @Resource
    private PermissionManage permissionManage;
    @Resource
    private DatasetSQLManage datasetSQLManage;
    @Resource
    private DatasetGroupManage datasetGroupManage;
    @Resource
    private CoreChartViewMapper coreChartViewMapper;
    @Resource
    private DataVisualizationInfoMapper visualizationInfoMapper;
    @Autowired(required = false)
    private PluginManageApi pluginManage;

    public void save(CoreDatasetTableField coreDatasetTableField) {
        checkNameLength(coreDatasetTableField.getName());
        if (ObjectUtils.isEmpty(coreDatasetTableField.getId())) {
            coreDatasetTableField.setId(IDUtils.snowID());
            coreDatasetTableFieldMapper.insert(coreDatasetTableField);
        } else {
            coreDatasetTableFieldMapper.updateById(coreDatasetTableField);
        }
    }

    public DatasetTableFieldDTO chartFieldSave(DatasetTableFieldDTO datasetTableFieldDTO) {
        requireChartAccess(datasetTableFieldDTO == null ? null : datasetTableFieldDTO.getChartId());
        checkNameLength(datasetTableFieldDTO.getName());
        CoreDatasetTableField coreDatasetTableField = coreDatasetTableFieldMapper.selectById(datasetTableFieldDTO.getId());
        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.eq("name", datasetTableFieldDTO.getName());
        wrapper.eq("chart_id", datasetTableFieldDTO.getChartId());
        if (ObjectUtils.isNotEmpty(coreDatasetTableField)) {
            wrapper.ne("id", datasetTableFieldDTO.getId());
        }
        List<CoreDatasetTableField> fields = coreDatasetTableFieldMapper.selectList(wrapper);
        if (ObjectUtils.isNotEmpty(fields)) {
            CrestException.throwException(Translator.get("i18n_field_name_duplicated"));
        }
        datasetTableFieldDTO.setDatasetGroupId(null);
        return save(datasetTableFieldDTO);
    }

    /**
     * 数据集保存时使用
     *
     * @param datasetTableFieldDTO
     * @return
     */
    public DatasetTableFieldDTO save(DatasetTableFieldDTO datasetTableFieldDTO) {
        checkNameLength(datasetTableFieldDTO.getName());
        CoreDatasetTableField coreDatasetTableField = coreDatasetTableFieldMapper.selectById(datasetTableFieldDTO.getId());
        CoreDatasetTableField record = transDTO2Record(datasetTableFieldDTO);
        if (ObjectUtils.isEmpty(record.getEngineFieldName())) {
            String n = TableUtils.fieldNameShort(record.getId() + "");
            record.setFieldShortName(n);
            record.setEngineFieldName(n);
        }
        if (ObjectUtils.isEmpty(coreDatasetTableField)) {
            coreDatasetTableFieldMapper.insert(record);
        } else {
            coreDatasetTableFieldMapper.updateById(record);
        }
        return datasetTableFieldDTO;
    }

    public DatasetTableFieldDTO saveField(DatasetTableFieldDTO datasetTableFieldDTO) {
        CoreDatasetTableField record = new CoreDatasetTableField();
        if (ObjectUtils.isEmpty(datasetTableFieldDTO.getId())) {
            datasetTableFieldDTO.setId(IDUtils.snowID());
            BeanUtils.copyBean(record, datasetTableFieldDTO);
            coreDatasetTableFieldMapper.insert(record);
        } else {
            BeanUtils.copyBean(record, datasetTableFieldDTO);
            coreDatasetTableFieldMapper.updateById(record);
        }
        return datasetTableFieldDTO;
    }

    public List<DatasetTableFieldDTO> getChartCalcFields(Long chartId) {
        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.eq("chart_id", chartId);
        return transDTO(coreDatasetTableFieldMapper.selectList(wrapper));
    }

    public void deleteById(Long id) {
        CoreDatasetTableField field = coreDatasetTableFieldMapper.selectById(id);
        if (field != null) {
            requireFieldAccess(field);
        }
        coreDatasetTableFieldMapper.deleteById(id);
    }

    private void requireFieldAccess(CoreDatasetTableField field) {
        if (field.getChartId() != null) {
            requireChartAccess(field.getChartId());
            return;
        }
        if (field.getDatasetGroupId() != null) {
            datasetGroupManage.requireDatasetAccess(field.getDatasetGroupId());
            return;
        }
        CrestPermissionUtils.requireCreator(null);
    }

    private void requireChartAccess(Long chartId) {
        if (chartId == null) {
            CrestPermissionUtils.requireCreator(null);
            return;
        }
        CoreChartView chart = coreChartViewMapper.selectById(chartId);
        if (chart == null) {
            CrestPermissionUtils.requireCreator(null);
            return;
        }
        if (CrestPermissionUtils.canAccessCreator(chart.getCreateBy())) {
            return;
        }
        if (canAccessScene(chart.getSceneId())) {
            return;
        }
        if (chart.getTableId() != null) {
            datasetGroupManage.requireDatasetAccess(chart.getTableId());
            return;
        }
        CrestPermissionUtils.requireCreator(chart.getCreateBy());
    }

    private boolean canAccessScene(Long sceneId) {
        if (sceneId == null) {
            return false;
        }
        DataVisualizationInfo scene = visualizationInfoMapper.selectById(sceneId);
        return scene != null && CrestPermissionUtils.canAccessCreator(scene.getCreateBy());
    }

    public void deleteByDatasetTableUpdate(Long datasetTableId, List<Long> fieldIds) {
        if (!CollectionUtils.isEmpty(fieldIds)) {
            QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
            wrapper.eq("dataset_table_id", datasetTableId);
            wrapper.notIn("id", fieldIds);
            coreDatasetTableFieldMapper.delete(wrapper);
        }
    }

    public void deleteByDatasetGroupUpdate(Long datasetGroupId, List<Long> fieldIds) {
        if (!CollectionUtils.isEmpty(fieldIds)) {
            // chartCopyFields不删除
            List<Long> chartCopyFields = getChartCopyFieldsByDatasetGroupIdAndOriginIds(datasetGroupId, fieldIds);
            fieldIds.addAll(chartCopyFields);
            QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
            wrapper.eq("dataset_group_id", datasetGroupId);
            wrapper.notIn("id", fieldIds);
            coreDatasetTableFieldMapper.delete(wrapper);
        }
    }

    /**
     * 获取图表复制字段
     * 通过原始名是[field_id]格式，ext_field 为 2，并且chart_id不为空的字段判定为图表复制字段
     * @param datasetGroupId 数据集ID
     * @param fieldIds 原数据集字段ID列表
     */
    public List<Long> getChartCopyFieldsByDatasetGroupIdAndOriginIds(Long datasetGroupId, List<Long> fieldIds) {
        List<String> originNames = fieldIds.stream().map(id -> "[" + id + "]").collect(Collectors.toList());
        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_group_id", datasetGroupId);
        wrapper.eq("ext_field", 2);
        wrapper.in("origin_name", originNames);
        wrapper.isNotNull("chart_id");
        List<CoreDatasetTableField> list = coreDatasetTableFieldMapper.selectList(wrapper);
        return list.stream().map(CoreDatasetTableField::getId).collect(Collectors.toList());
    }

    public void deleteByDatasetGroupDelete(Long datasetGroupId) {
        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_group_id", datasetGroupId);
        coreDatasetTableFieldMapper.delete(wrapper);
    }

    public void deleteChartFields(Long chartId) {
        requireChartAccess(chartId);
        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.eq("chart_id", chartId);
        coreDatasetTableFieldMapper.delete(wrapper);
    }

    public List<DatasetTableFieldDTO> selectByDatasetTableId(Long id) {
        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_table_id", id);
        return transDTO(coreDatasetTableFieldMapper.selectList(wrapper));
    }

    public List<DatasetTableFieldDTO> selectByDatasetGroupId(Long id) {
        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_group_id", id);
        wrapper.eq("checked", true);
        wrapper.isNull("chart_id");
        return transDTO(coreDatasetTableFieldMapper.selectList(wrapper));
    }

    public List<DatasetTableFieldDTO> selectByDatasetGroupIdWithAccess(Long id) {
        datasetGroupManage.requireDatasetAccess(id);
        return selectByDatasetGroupId(id);
    }

    public Map<String, List<DatasetTableFieldDTO>> selectByDatasetGroupIds(List<Long> ids) {
        Map<String, List<DatasetTableFieldDTO>> map = new HashMap<>();
        for (Long id : ids) {
            QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
            wrapper.eq("dataset_group_id", id);
            wrapper.eq("checked", true);
            wrapper.isNull("chart_id");
            wrapper.eq("ext_field", 0);
            map.put(String.valueOf(id), transDTO(coreDatasetTableFieldMapper.selectList(wrapper)));
        }
        return map;
    }

    public Map<String, List<DatasetTableFieldDTO>> selectByDatasetGroupIdsWithAccess(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyMap();
        }
        ids.forEach(datasetGroupManage::requireDatasetAccess);
        return selectByDatasetGroupIds(ids);
    }

    public List<DatasetTableFieldDTO> selectByFieldIds(List<Long> ids) {
        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids);
        return transDTO(coreDatasetTableFieldMapper.selectList(wrapper));
    }

    public DatasetTableFieldDTO selectById(Long id) {
        CoreDatasetTableField coreDatasetTableField = coreDatasetTableFieldMapper.selectById(id);
        if (coreDatasetTableField == null) return null;
        return transObj(coreDatasetTableField);
    }

    public DatasetTableFieldDTO selectByIdWithAccess(Long id) {
        CoreDatasetTableField field = coreDatasetTableFieldMapper.selectById(id);
        if (field == null) return null;
        requireFieldAccess(field);
        return transObj(field);
    }

    /**
     * 返回维度、指标列表
     *
     * @return
     */
    public Map<String, List<DatasetTableFieldDTO>> listByDQ(Long id) {
        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_group_id", id);
        wrapper.eq("checked", true);
        List<DatasetTableFieldDTO> list = transDTO(coreDatasetTableFieldMapper.selectList(wrapper));
        List<DatasetTableFieldDTO> dimensionList = list.stream().filter(ele -> Strings.CI.equals(ele.getGroupType(), "d")).collect(Collectors.toList());
        List<DatasetTableFieldDTO> quotaList = list.stream().filter(ele -> Strings.CI.equals(ele.getGroupType(), "q")).collect(Collectors.toList());
        Map<String, List<DatasetTableFieldDTO>> map = new LinkedHashMap<>();
        map.put("dimensionList", dimensionList);
        map.put("quotaList", quotaList);
        return map;
    }

    public Map<String, List<DatasetTableFieldDTO>> listByDQWithAccess(Long id) {
        datasetGroupManage.requireDatasetAccess(id);
        return listByDQ(id);
    }

    public Map<String, List<DatasetTableFieldDTO>> copilotFields(Long id) throws Exception {
        DatasetGroupInfoDTO datasetGroupInfoDTO = datasetGroupManage.datasetGroupInfoDTO(id, null);
        Map<String, Object> sqlMap = datasetSQLManage.getUnionSQLForEdit(datasetGroupInfoDTO, null);
        Map<Long, DatasourceSchemaDTO> dsMap = (Map<Long, DatasourceSchemaDTO>) sqlMap.get("dsMap");
        boolean crossDs = Utils.isCrossDs(dsMap);
        if (crossDs) {
            CrestException.throwException(Translator.get("i18n_dataset_cross_error"));
        }
        if (!isCopilotSupport(dsMap)) {
            CrestException.throwException(Translator.get("i18n_copilot_ds"));
        }

        QueryWrapper<CoreDatasetTableField> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_group_id", id);
        wrapper.eq("checked", true);
        wrapper.eq("ext_field", 0);
        List<DatasetTableFieldDTO> list = transDTO(coreDatasetTableFieldMapper.selectList(wrapper));

        Map<String, ColumnPermissionItem> desensitizationList = new HashMap<>();
        list = permissionManage.filterColumnPermissions(list, desensitizationList, id, null);

        List<DatasetTableFieldDTO> dimensionList = list.stream().filter(ele -> Strings.CI.equals(ele.getGroupType(), "d")).collect(Collectors.toList());
        List<DatasetTableFieldDTO> quotaList = list.stream().filter(ele -> Strings.CI.equals(ele.getGroupType(), "q")).collect(Collectors.toList());
        Map<String, List<DatasetTableFieldDTO>> map = new LinkedHashMap<>();
        map.put("dimensionList", dimensionList);
        map.put("quotaList", quotaList);
        return map;
    }

    public List<DatasetTableFieldDTO> listFieldsWithPermissions(Long id) {
        datasetGroupManage.requireDatasetAccess(id);
        List<DatasetTableFieldDTO> fields = selectByDatasetGroupId(id);
        Map<String, ColumnPermissionItem> desensitizationList = new HashMap<>();
        Long userId = AuthUtils.getUser() == null ? null : AuthUtils.getUser().getUserId();
        List<DatasetTableFieldDTO> tmp = permissionManage
                .filterColumnPermissions(fields, desensitizationList, id, userId)
                .stream()
                .sorted(Comparator.comparing(DatasetTableFieldDTO::getGroupType))
                .toList();
        tmp.forEach(ele -> ele.setDesensitized(desensitizationList.containsKey(ele.getEngineFieldName())));
        return tmp;
    }

    public List<DatasetTableFieldDTO> listFieldsWithPermissionsRemoveAgg(Long id) {
        datasetGroupManage.requireDatasetAccess(id);
        List<DatasetTableFieldDTO> fields = selectByDatasetGroupId(id);
        Map<String, ColumnPermissionItem> desensitizationList = new HashMap<>();
        Long userId = AuthUtils.getUser() == null ? null : AuthUtils.getUser().getUserId();
        SQLObj tableObj = new SQLObj();
        tableObj.setTableAlias("");
        List<DatasetTableFieldDTO> tmp = permissionManage
                .filterColumnPermissions(fields, desensitizationList, id, userId)
                .stream()
                .filter(ele -> {
                    boolean flag = true;
                    if (Objects.equals(ele.getExtField(), ExtFieldConstant.EXT_CALC)) {
                        String originField = Utils.calcFieldRegex(ele, tableObj, fields, true, null, Utils.mergeParam(Utils.getParams(fields), null), pluginManage);
                        for (String func : FunctionConstant.AGG_FUNC) {
                            if (Utils.matchFunction(func, originField)) {
                                flag = false;
                                break;
                            }
                        }
                    }
                    return flag;
                })
                .sorted(Comparator.comparing(DatasetTableFieldDTO::getGroupType))
                .toList();
        tmp.forEach(ele -> ele.setDesensitized(desensitizationList.containsKey(ele.getEngineFieldName())));
        DatasetUtils.listEncode(tmp);
        return tmp;
    }

    public DatasetTableFieldDTO transObj(CoreDatasetTableField ele) {
        DatasetTableFieldDTO dto = new DatasetTableFieldDTO();
        if (ele == null) return null;
        BeanUtils.copyBean(dto, ele);
        if (StringUtils.isNotEmpty(ele.getParams())) {
            TypeReference<List<CalParam>> tokenType = new TypeReference<>() {
            };
            List<CalParam> calParams = JsonUtil.parseList(ele.getParams(), tokenType);
            dto.setParams(calParams);
        }
        if (StringUtils.isNotEmpty(ele.getGroupList())) {
            TypeReference<List<FieldGroupDTO>> groupTokenType = new TypeReference<>() {
            };
            List<FieldGroupDTO> fieldGroups = JsonUtil.parseList(ele.getGroupList(), groupTokenType);
            dto.setGroupList(fieldGroups);
        }
        return dto;
    }

    public List<DatasetTableFieldDTO> transDTO(List<CoreDatasetTableField> list) {
        if (!CollectionUtils.isEmpty(list)) {
            return list.stream().map(this::transObj).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    private CoreDatasetTableField transDTO2Record(DatasetTableFieldDTO dto) {
        CoreDatasetTableField record = new CoreDatasetTableField();
        BeanUtils.copyBean(record, dto);
        if (ObjectUtils.isNotEmpty(dto.getParams())) {
            record.setParams(JsonUtil.toJSONString(dto.getParams()).toString());
        }
        if (ObjectUtils.isNotEmpty(dto.getGroupList())) {
            record.setGroupList(JsonUtil.toJSONString(dto.getGroupList()).toString());
        }
        return record;
    }

    private void checkNameLength(String name) {
        if (name != null && name.length() > 100) {
            CrestException.throwException(Translator.get("i18n_field_name_limit_100"));
        }
    }

    public boolean isCopilotSupport(Map<Long, DatasourceSchemaDTO> dsMap) {
        DatasourceSchemaDTO value = dsMap.entrySet().iterator().next().getValue();
        return Strings.CI.equals(value.getType(), "mysql");
    }
}
