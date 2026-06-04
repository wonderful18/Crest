package io.crest.api.permissions.dataset.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.crest.api.permissions.dataset.dto.DataSetColumnPermissionsDTO;
import io.crest.auth.CrestApiPath;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static io.crest.constant.AuthResourceEnum.DATASET;

@Hidden
@Tag(name = "列权限")
@CrestApiPath(value = "/dataset/column-permissions", rt = DATASET)
public interface ColumnPermissionsApi {

    @Operation(summary = "查询列权限列表")
    @GetMapping("/page/{datasetId}/{goPage}/{pageSize}")
    public IPage<DataSetColumnPermissionsDTO> columnPermissions(@PathVariable("datasetId") Long datasetId, @PathVariable("goPage") int goPage, @PathVariable("pageSize") int pageSize);

    @Operation(summary = "保存")
    @PostMapping("save")
    public void save(@RequestBody DataSetColumnPermissionsDTO dataSetColumnPermissionsDTO);

    @Operation(summary = "删除")
    @DeleteMapping
    public void delete(@RequestBody DataSetColumnPermissionsDTO dataSetColumnPermissionsDTO);

    @Operation(summary = "获取详细信息")
    @PostMapping("/info")
    public DataSetColumnPermissionsDTO DataSetColumnPermissionInfo(@RequestBody DataSetColumnPermissionsDTO request);

    public List<DataSetColumnPermissionsDTO> list(@RequestBody DataSetColumnPermissionsDTO request);

}
