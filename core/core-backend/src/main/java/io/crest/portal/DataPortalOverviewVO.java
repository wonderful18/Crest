package io.crest.portal;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DataPortalOverviewVO implements Serializable {
    @Serial
    private static final long serialVersionUID = -5133467643668235940L;

    private long total;
    private long screenCount;
    private long dashboardCount;
    private Long latestUpdateTime;
    private Boolean backendAccess;
}
