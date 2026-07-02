package io.crest.dataset.server;

import io.crest.api.dataset.DatasetTableApi;
import io.crest.api.dataset.dto.MultFieldValuesRequest;
import io.crest.api.dataset.engine.SQLFunctionDTO;
import io.crest.api.dataset.engine.SQLFunctionsEnum;
import io.crest.dataset.manage.DatasetDataManage;
import io.crest.dataset.manage.DatasetTableFieldManage;
import io.crest.dataset.utils.DatasetUtils;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("dataset-field")
public class DatasetFieldServer implements DatasetTableApi {
    @Resource
    private DatasetTableFieldManage datasetTableFieldManage;
    @Resource
    private DatasetDataManage datasetDataManage;

    @Override
    public DatasetTableFieldDTO save(@RequestBody DatasetTableFieldDTO datasetTableFieldDTO) throws Exception {
        return datasetTableFieldManage.chartFieldSave(datasetTableFieldDTO);
    }

    @Override
    public DatasetTableFieldDTO get(Long id) {
        return datasetTableFieldManage.selectByIdWithAccess(id);
    }

    @Override
    public List<DatasetTableFieldDTO> listByDatasetGroup(Long id) {
        List<DatasetTableFieldDTO> datasetTableFieldDTOS = datasetTableFieldManage.selectByDatasetGroupIdWithAccess(id);
        DatasetUtils.listEncode(datasetTableFieldDTOS);
        return datasetTableFieldDTOS;
    }

    @Override
    public Map<String, List<DatasetTableFieldDTO>> listByDsIds(@RequestBody List<Long> ids) {
        return datasetTableFieldManage.selectByDatasetGroupIdsWithAccess(ids);
    }

    @Override
    public void delete(Long id) {
        datasetTableFieldManage.deleteById(id);
    }

    @Override
    public Map<String, List<DatasetTableFieldDTO>> listByDQ(Long id) {
        return datasetTableFieldManage.listByDQWithAccess(id);
    }

    @Override
    public Map<String, List<DatasetTableFieldDTO>> copilotFields(Long id) throws Exception {
        return datasetTableFieldManage.copilotFields(id);
    }

    @Override
    public List<DatasetTableFieldDTO> listFieldsWithPermissions(Long id) {
        return datasetTableFieldManage.listFieldsWithPermissionsRemoveAgg(id);
    }

    @Override
    public List<String> multFieldValuesForPermissions(@RequestBody MultFieldValuesRequest multFieldValuesRequest) throws Exception {
        return datasetDataManage.getFieldEnum(multFieldValuesRequest);
    }

    @Override
    public List<SQLFunctionDTO> functions() {
        SQLFunctionsEnum[] values = SQLFunctionsEnum.values();
        return Arrays.stream(values).map(ele -> {
            SQLFunctionDTO dto = new SQLFunctionDTO();
            dto.setName(ele.getName());
            dto.setFunc(ele.getFunc());
            dto.setType(ele.getType());
            dto.setDesc(ele.getDesc());
            dto.setCustom(ele.isCustom());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteChartFields(Long id) {
        datasetTableFieldManage.deleteChartFields(id);
    }
}
