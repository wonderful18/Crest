package io.crest.portal;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class DataPortalPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 8093644632121377141L;

    private long total;
    private int page;
    private int pageSize;
    private List<DataPortalResourceVO> records = new ArrayList<>();
}
