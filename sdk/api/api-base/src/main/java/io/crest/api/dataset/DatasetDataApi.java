package io.crest.api.dataset;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.crest.api.dataset.dto.*;
import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@Tag(name = "数据集管理:数据")
@ApiSupport(order = 978)
public interface DatasetDataApi {
    @Operation(summary = "预览数据")
    @PostMapping("preview-data")
    Map<String, Object> previewData(@RequestBody DatasetGroupInfoDTO datasetGroupInfoDTO);

    @Operation(summary = "获取数据集节点字段")
    @PostMapping("table-fields")
    List<DatasetTableFieldDTO> tableField(@RequestBody DatasetTableDTO datasetTableDTO);

    @Operation(summary = "SQL预览")
    @PostMapping("preview-sql")
    Map<String, Object> previewSql(@RequestBody PreviewSqlDTO dto);

    @Operation(summary = "sql片段校验", hidden = true)
    @PostMapping("preview-sql/check")
    Map<String, Object> previewSqlCheck(@RequestBody PreviewSqlDTO dto);

    @Operation(summary = "数据集获取字段枚举值")
    @PostMapping("enum-values/dataset")
    List<String> getFieldEnumDs(@RequestBody EnumObj map);

    @Operation(summary = "获取字段枚举值")
    @PostMapping("enum-values")
    List<String> getFieldEnum(@RequestBody MultFieldValuesRequest multFieldValuesRequest);

    @Operation(summary = "获取字段枚举值(多字段)")
    @PostMapping("enum-values/object")
    List<Map<String, Object>> getFieldEnumObj(@RequestBody EnumValueRequest request);

    @Operation(summary = "获取数据集总数据量", hidden = true)
    @PostMapping("dataset-count")
    Long datasetCount(@RequestBody DatasetGroupInfoDTO datasetGroupInfoDTO);

    @Operation(summary = "获取数据集数据量", hidden = true)
    @PostMapping("dataset-total")
    Long datasetTotal(@RequestBody DatasetGroupInfoDTO datasetGroupInfoDTO);

    @Operation(summary = "获取下拉树数据", hidden = true)
    @PostMapping("field-tree")
    List<BaseTreeNodeDTO> getFieldValueTree(@RequestBody MultFieldValuesRequest multFieldValuesRequest);
}
