package io.crest.api.permissions.dataset.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatasetRowPermissionsTreeRequest extends DataSetRowPermissionsTreeDTO {
    public String orderBy;
}
