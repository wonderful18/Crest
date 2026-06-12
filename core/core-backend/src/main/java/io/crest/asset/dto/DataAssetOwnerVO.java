package io.crest.asset.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DataAssetOwnerVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 6329388511482686922L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private String account;
}
