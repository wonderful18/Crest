package io.crest.dataset.server;

import io.crest.api.dataset.DatasetDataApi;
import io.crest.api.dataset.dto.*;
import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.dataset.manage.DatasetDataManage;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.utils.LogUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("dataset-data")
public class DatasetDataServer implements DatasetDataApi {
    @Resource
    private DatasetDataManage datasetDataManage;

    @Override
    public Map<String, Object> previewData(@RequestBody DatasetGroupInfoDTO datasetGroupInfoDTO) throws Exception {
        return datasetDataManage.previewDataWithLimit(datasetGroupInfoDTO, 0, 100, false, true);
    }

    @Override
    public List<DatasetTableFieldDTO> tableField(@RequestBody DatasetTableDTO datasetTableDTO) throws Exception {
        return datasetDataManage.getTableFields(datasetTableDTO);
    }

    @Override
    public Map<String, Object> previewSql(@RequestBody PreviewSqlDTO dto) throws Exception {
        return datasetDataManage.previewSqlWithLog(dto);
    }

    @Override
    public Map<String, Object> previewSqlCheck(@RequestBody PreviewSqlDTO dto) throws Exception {
        return datasetDataManage.previewSql(dto);
    }

    @Override
    public List<String> getFieldEnumDs(@RequestBody EnumObj map) throws Exception {
        try {
            return datasetDataManage.getFieldEnumDs(map);
        } catch (Exception e) {
            LogUtil.info(e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> getFieldEnum(@RequestBody MultFieldValuesRequest multFieldValuesRequest) {
        try {
            return datasetDataManage.getFieldEnum(multFieldValuesRequest);
        } catch (Exception e) {
            LogUtil.info(e.getMessage());
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> getFieldEnumObj(@RequestBody EnumValueRequest request) throws Exception {
        try {
            return datasetDataManage.getFieldEnumObj(request);
        } catch (Exception e) {
            LogUtil.info(e.getMessage());
            return null;
        }
    }

    @Override
    public Long datasetCount(@RequestBody DatasetGroupInfoDTO datasetGroupInfoDTO) throws Exception {
        return datasetDataManage.datasetTotal(datasetGroupInfoDTO.getId());
    }

    @Override
    public Long datasetTotal(@RequestBody DatasetGroupInfoDTO datasetGroupInfoDTO) throws Exception {
        return datasetDataManage.datasetCountWithWhere(datasetGroupInfoDTO.getId());
    }

    @Override
    public List<BaseTreeNodeDTO> getFieldValueTree(@RequestBody MultFieldValuesRequest multFieldValuesRequest) throws Exception {
        try {
            return datasetDataManage.getFieldValueTree(multFieldValuesRequest);
        } catch (Exception e) {
            LogUtil.info(e.getMessage());
            return null;
        }
    }
}
