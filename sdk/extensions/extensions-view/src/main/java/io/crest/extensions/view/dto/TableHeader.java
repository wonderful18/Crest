package io.crest.extensions.view.dto;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Data
public class TableHeader {
    private HeaderGroupConfig headerGroupConfig;
    private boolean headerGroup;
    private AuxiliaryHeader auxiliaryHeader;


    @Data
    static
    public class HeaderGroupConfig {
        private List<MetaInfo> meta = new ArrayList<>();
        private List<ColumnInfo> columns = new ArrayList<>();
    }

    @Data
    static
    public class ColumnInfo {
        @Getter
        private String key;
        private List<ColumnInfo> children = new ArrayList<>();
        private Integer width;
    }

    @Getter
    @Data
    static
    public class MetaInfo {
        private String field;
        private String name;

    }

    @Data
    static
    public class AuxiliaryHeader {
        private boolean enabled;
        private Integer rowHeight;
        private String backgroundColor;
        private String fontColor;
        private Integer fontSize;
        private String align;
        private List<DescriptionInfo> descriptions = new ArrayList<>();
    }

    @Data
    static
    public class DescriptionInfo {
        private String field;
        private String text;
    }
}
