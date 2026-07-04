package io.crest.visualization.manage;

import io.crest.auth.bo.TokenUserBO;
import io.crest.chart.manage.ChartViewManege;
import io.crest.operation.manage.CoreOptRecentManage;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.utils.AuthUtils;
import io.crest.visualization.dao.auto.entity.DataVisualizationInfo;
import io.crest.visualization.dao.auto.entity.SnapshotDataVisualizationInfo;
import io.crest.visualization.dao.auto.mapper.DataVisualizationInfoMapper;
import io.crest.visualization.dao.auto.mapper.SnapshotDataVisualizationInfoMapper;
import io.crest.visualization.dao.ext.mapper.ExtDataVisualizationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoreVisualizationManagePublishTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void innerEditShouldKeepCurrentVisualizationTypeWhenRequestOmitsType() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DataVisualizationInfoMapper mapper = mock(DataVisualizationInfoMapper.class);
        SnapshotDataVisualizationInfoMapper snapshotMapper = mock(SnapshotDataVisualizationInfoMapper.class);
        PlatformPermissionManage platformPermissionManage = mock(PlatformPermissionManage.class);
        CoreOptRecentManage coreOptRecentManage = mock(CoreOptRecentManage.class);
        CoreVisualizationManage manage = new CoreVisualizationManage();
        ReflectionTestUtils.setField(manage, "mapper", mapper);
        ReflectionTestUtils.setField(manage, "snapshotMapper", snapshotMapper);
        ReflectionTestUtils.setField(manage, "platformPermissionManage", platformPermissionManage);
        ReflectionTestUtils.setField(manage, "coreOptRecentManage", coreOptRecentManage);

        DataVisualizationInfo current = new DataVisualizationInfo();
        current.setId(20L);
        current.setType("dataV");
        current.setCreateBy("2");
        when(mapper.selectById(20L)).thenReturn(current);

        DataVisualizationInfo update = new DataVisualizationInfo();
        update.setId(20L);
        update.setName("经营大屏");
        update.setStatus(1);

        manage.innerEdit(update);

        verify(platformPermissionManage).upsertResource(eq("screen"), eq("20"), eq(1L), eq(2L),
                eq("经营大屏"), any(), any());
    }

    @Test
    void dvRestoreShouldAllowSnapshotRestoreAfterCoreResourceWasDeleted() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DataVisualizationInfoMapper mapper = mock(DataVisualizationInfoMapper.class);
        SnapshotDataVisualizationInfoMapper snapshotMapper = mock(SnapshotDataVisualizationInfoMapper.class);
        ExtDataVisualizationMapper extDataVisualizationMapper = mock(ExtDataVisualizationMapper.class);
        ChartViewManege chartViewManege = mock(ChartViewManege.class);
        CoreVisualizationManage manage = new CoreVisualizationManage();
        ReflectionTestUtils.setField(manage, "mapper", mapper);
        ReflectionTestUtils.setField(manage, "snapshotMapper", snapshotMapper);
        ReflectionTestUtils.setField(manage, "extDataVisualizationMapper", extDataVisualizationMapper);
        ReflectionTestUtils.setField(manage, "chartViewManege", chartViewManege);

        SnapshotDataVisualizationInfo snapshot = new SnapshotDataVisualizationInfo();
        snapshot.setId(20L);
        snapshot.setCreateBy("2");
        when(mapper.selectById(20L)).thenReturn(null);
        when(snapshotMapper.selectById(20L)).thenReturn(snapshot);

        manage.dvRestore(20L);

        verify(extDataVisualizationMapper).restoreDataV(20L);
        verify(extDataVisualizationMapper).restoreViews(20L);
        verify(chartViewManege).restoreThreshold(20L, "core");
    }
}
