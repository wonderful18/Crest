package io.crest.chart.dao.ext.mapper;

import io.crest.api.chart.vo.ViewSelectorVO;
import io.crest.api.dataset.vo.DataSQLBotDatasetVO;
import io.crest.chart.dao.auto.entity.CoreChartView;
import io.crest.chart.dao.ext.entity.ChartBasePO;
import io.crest.extensions.view.dto.ChartViewDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
// 定义当前业务对象的核心职责
public interface ExtChartViewMapper {

    @Select("""
            select id, scene_id as pid, title, type from bi_chart where type != 'VQuery' and scene_id = #{resourceId}
            """)
    List<ViewSelectorVO> queryViewOption(@Param("resourceId") Long resourceId);

    ChartBasePO queryChart(@Param("id") Long id, @Param("resourceTable")String resourceTable);

    List<CoreChartView> selectListCustom(@Param("sceneId") Long sceneId, @Param("resourceTable") String resourceTable);

    void deleteViewsBySceneId(@Param("sceneId") Long sceneId, @Param("resourceTable") String resourceTable);

    @Select("""
            <script>
            SELECT id, scene_id as pid, title, type FROM (
                SELECT id, scene_id, title, type FROM bi_chart
                WHERE id = #{viewId}
                UNION ALL
                SELECT id, scene_id, title, type FROM bi_chart_snapshot
                WHERE id = #{viewId}
            ) combined_views
            <choose>
                <when test="_databaseId == 'ob-oracle'">
                    FETCH FIRST 1 ROWS ONLY
                </when>
                <otherwise>
                    LIMIT 1
                </otherwise>
            </choose>
            </script>
            """)
    ChartViewDTO findChartViewAround(@Param("viewId") String viewId);


    @Select("""
            select DISTINCT table_id from bi_chart_snapshot where scene_id=#{dvId}
            """)
    List<Long> findDatasetGroupIdByDvId(@Param("dvId") String dvId);


    @Select("""
            SELECT
             DISTINCT
                sdg.id AS table_id,
                sdg.NAME AS table_name,
                cd.id AS ds_id,
                cd.NAME AS ds_name\s
            FROM
                dataset_table sdt
                INNER JOIN data_source cd ON sdt.datasource_id = cd.id
                INNER JOIN dataset_group sdg ON sdt.dataset_group_id = sdg.id
                INNER JOIN bi_chart_snapshot sccv on  sccv.table_id = sdt.dataset_group_id\s
            WHERE
                sccv.scene_id = #{dvId}
            """)
    List<DataSQLBotDatasetVO> findDataSQLBotDatasetDvId(@Param("dvId") String dvId);


}
