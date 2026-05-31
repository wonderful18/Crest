package io.crest.api.permissions.dataset.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.crest.api.permissions.dataset.dto.*;
import io.crest.api.permissions.user.vo.UserFormVO;
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
@Tag(name = "行权限")
@CrestApiPath(value = "/dataset/row-permissions", rt = DATASET)
public interface RowPermissionsApi {

    @Operation(summary = "查询行权限列表")
    @GetMapping("/page/{datasetId}/{goPage}/{pageSize}")
    public IPage<DataSetRowPermissionsTreeDTO> rowPermissions(@PathVariable("datasetId") Long datasetId, @PathVariable("goPage") int goPage, @PathVariable("pageSize") int pageSize);

    @Operation(summary = "保存")
    @PostMapping("record")
    public void save(@RequestBody DataSetRowPermissionsTreeDTO datasetRowPermissions);

    @Operation(summary = "删除")
    @DeleteMapping
    public void delete(@RequestBody DataSetRowPermissionsTreeDTO datasetRowPermissions);

    @Operation(summary = "授权对象")
    @GetMapping("/authorized-objects/{datasetId}/{type}")
    public List<Item> authObjs(@PathVariable("datasetId") Long datasetId, @PathVariable("type") String type);

    @Operation(summary = "获取详细信息")
    @PostMapping("/row-permission-info")
    public DataSetRowPermissionsTreeDTO dataSetRowPermissionInfo(@RequestBody DataSetRowPermissionsTreeDTO request);

    @Operation(summary = "白名单")
    @PostMapping("/allowlist-users")
    public List<UserFormVO> whiteListUsers(@RequestBody WhiteListUsersRequest request);

    public UserFormVO getUserById(Long id);

    public List<DataSetRowPermissionsTreeDTO> list(DatasetRowPermissionsTreeRequest dataSetRowPermissionsTreeDTO) ;
}
