package io.crest.chart;

import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.chart.charts.impl.table.TableInfoHandler;
import io.crest.chart.charts.impl.table.TableNormalHandler;
import io.crest.constant.FieldTypeConstants;
import io.crest.engine.constant.ExtFieldConstant;
import io.crest.extensions.datasource.dto.ConnectionObj;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.dto.DatasourceDTO;
import io.crest.extensions.datasource.dto.DatasourceRequest;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.dto.TableField;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.model.SQLObj;
import io.crest.extensions.datasource.provider.Provider;
import io.crest.extensions.view.dto.AxisFormatResult;
import io.crest.extensions.view.dto.ChartAxis;
import io.crest.extensions.view.dto.ChartCalcDataResult;
import io.crest.extensions.view.dto.ChartExtFilterDTO;
import io.crest.extensions.view.dto.ChartExtRequest;
import io.crest.extensions.view.dto.ChartViewDTO;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import io.crest.extensions.view.dto.CustomFilterResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableSummaryHandlerTest {

    @Test
    void tableInfoHandlerShouldReturnSummaryColumnResult() {
        ChartCalcDataResult result = new TableInfoHandler().calcChartResult(
                view("table-info"),
                formatResult(),
                filterResult("pull"),
                sqlMap(),
                sqlMeta(),
                new SummaryProvider());

        Map<String, BigDecimal> summary = summaryColumnResult(result);

        assertEquals(new BigDecimal("42.5"), summary.get("summary_amount"));
    }

    @Test
    void tableNormalHandlerShouldReturnSummaryColumnResult() {
        SummaryProvider provider = new SummaryProvider();

        ChartCalcDataResult result = new TableNormalHandler().calcChartResult(
                view("table-normal"),
                formatResult(),
                filterResult("pull"),
                sqlMap(),
                sqlMeta(),
                provider);

        Map<String, BigDecimal> summary = summaryColumnResult(result);

        assertEquals(new BigDecimal("42.5"), summary.get("summary_amount"));
        assertEquals(2, provider.fetchCount);
    }

    @Test
    void excelBuildChartShouldExposeSummaryColumnResultForExport() {
        Map<String, BigDecimal> summaryColumnResult = Map.of("summary_amount", new BigDecimal("42.5"));
        ChartCalcDataResult calcResult = new ChartCalcDataResult();
        calcResult.setData(Map.of("summaryColumnResult", summaryColumnResult));
        calcResult.setOriginData(List.<String[]>of(new String[]{"项目A"}));

        ChartViewDTO view = view("table-info");
        view.setIsExcelExport(true);
        AxisFormatResult formatResult = formatResult();
        formatResult.getAxisMap().put(ChartAxis.extStack, new ArrayList<>());
        CustomFilterResult filterResult = filterResult("pull");
        filterResult.getContext().put("desensitizationList", Map.of());

        new TableInfoHandler().buildChart(view, calcResult, formatResult, filterResult);

        assertEquals(summaryColumnResult, view.getData().get("summaryColumnResult"));
    }

    private ChartViewDTO view(String type) {
        ChartViewDTO view = new ChartViewDTO();
        view.setType(type);
        view.setChartExtRequest(new ChartExtRequest());
        view.setXAxis(List.of(projectField()));
        view.setYAxis(List.of());
        view.setCustomAttr(Map.of("basicStyle", Map.of(
                "showSummary", true,
                "summaryLabel", "总计",
                "seriesSummary", List.of(),
                "summaryColumns", List.of(Map.of(
                        "id", "summary_amount",
                        "name", "总金额",
                        "show", true,
                        "summary", "sum",
                        "field", "f_amount")))));
        return view;
    }

    private AxisFormatResult formatResult() {
        DatasetGroupInfoDTO dataset = new DatasetGroupInfoDTO();
        dataset.setIsCross(false);
        Map<ChartAxis, List<ChartViewFieldDTO>> axisMap = new HashMap<>();
        axisMap.put(ChartAxis.xAxis, new ArrayList<>(List.of(projectField())));
        axisMap.put(ChartAxis.yAxis, new ArrayList<>());
        Map<String, Object> context = new HashMap<>();
        context.put("dataset", dataset);
        return new AxisFormatResult(axisMap, context);
    }

    private CustomFilterResult filterResult(String tablePageMode) {
        Map<String, Object> context = new HashMap<>();
        context.put("allFields", List.of(projectField(), amountField()));
        context.put("tablePageMode", tablePageMode);
        return new CustomFilterResult(List.<ChartExtFilterDTO>of(), context);
    }

    private Map<String, Object> sqlMap() {
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setType("mysql");
        return Map.of("dsMap", Map.of(1L, datasource));
    }

    private SQLMeta sqlMeta() {
        SQLMeta meta = new SQLMeta();
        meta.setTable(SQLObj.builder().tableName("orders").tableAlias("t_a_0").build());
        return meta;
    }

    private ChartViewFieldDTO projectField() {
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setId(1L);
        field.setField("f_project");
        field.setEngineFieldName("f_project");
        field.setOriginName("project");
        field.setName("项目");
        field.setFieldType(FieldTypeConstants.STRING);
        field.setExtField(ExtFieldConstant.EXT_NORMAL);
        field.setGroupType("d");
        return field;
    }

    private ChartViewFieldDTO amountField() {
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setId(2L);
        field.setField("f_amount");
        field.setEngineFieldName("f_amount");
        field.setOriginName("amount");
        field.setName("金额");
        field.setFieldType(FieldTypeConstants.FLOAT);
        field.setExtField(ExtFieldConstant.EXT_NORMAL);
        field.setGroupType("q");
        return field;
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> summaryColumnResult(ChartCalcDataResult result) {
        return (Map<String, BigDecimal>) result.getData().get("summaryColumnResult");
    }

    private static class SummaryProvider extends Provider {
        int fetchCount;

        @Override
        public Map<String, Object> fetchResultField(DatasourceRequest datasourceRequest) {
            fetchCount++;
            List<String[]> data = fetchCount == 1
                    ? List.<String[]>of(new String[]{"项目A"})
                    : List.<String[]>of(new String[]{"42.5"});
            return Map.of("data", data);
        }

        @Override
        public String rebuildSQL(String sql, SQLMeta sqlMeta, boolean crossDs, Map<Long, DatasourceSchemaDTO> dsMap) {
            return sql;
        }

        @Override
        public List<String> getSchema(DatasourceRequest datasourceRequest) {
            return List.of();
        }

        @Override
        public List<DatasetTableDTO> tables(DatasourceRequest datasourceRequest) {
            return List.of();
        }

        @Override
        public ConnectionObj getConnection(DatasourceDTO coreDatasource) {
            return new ConnectionObj();
        }

        @Override
        public String checkStatus(DatasourceRequest datasourceRequest) {
            return null;
        }

        @Override
        public List<TableField> fetchTableField(DatasourceRequest datasourceRequest) {
            return List.of();
        }

        @Override
        public void hidePW(DatasourceDTO datasourceDTO) {
        }
    }
}
