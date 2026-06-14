package io.crest.extensions.datasource.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CalParam implements Serializable {
    private String id;
    private String name;
    private String value;
}
