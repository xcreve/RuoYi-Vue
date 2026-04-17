package com.ruoyi.quartz.task.pv;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PvTelemetryPartitionMaintainTaskTest
{
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void maintainMonthlyPartitionsShouldAddNextMonthAndDropExpiredPartition()
    {
        PvTelemetryPartitionMaintainTask task = new PvTelemetryPartitionMaintainTask(
                jdbcTemplate,
                fixedClock(2026, 5, 1, 2, 0));
        when(jdbcTemplate.queryForList(PvTelemetryPartitionMaintainTask.LOAD_PARTITIONS_SQL, String.class))
                .thenReturn(partitions("202604", "202703"));

        task.maintainMonthlyPartitions(12);

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).queryForList(PvTelemetryPartitionMaintainTask.LOAD_PARTITIONS_SQL, String.class);
        inOrder.verify(jdbcTemplate).execute(
                "alter table pv_telemetry reorganize partition pmax into (partition p202704 values less than (to_days('2027-05-01')), partition pmax values less than maxvalue)");
        inOrder.verify(jdbcTemplate).execute("alter table pv_telemetry drop partition p202604");
    }

    @Test
    void maintainMonthlyPartitionsShouldFallbackToDefaultWindowWhenArgumentInvalid()
    {
        PvTelemetryPartitionMaintainTask task = new PvTelemetryPartitionMaintainTask(
                jdbcTemplate,
                fixedClock(2026, 4, 15, 9, 0));
        when(jdbcTemplate.queryForList(PvTelemetryPartitionMaintainTask.LOAD_PARTITIONS_SQL, String.class))
                .thenReturn(partitions("202604", "202703"));

        task.maintainMonthlyPartitions(0);

        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void maintainMonthlyPartitionsShouldFailWhenPmaxMissing()
    {
        PvTelemetryPartitionMaintainTask task = new PvTelemetryPartitionMaintainTask(
                jdbcTemplate,
                fixedClock(2026, 4, 15, 9, 0));
        when(jdbcTemplate.queryForList(PvTelemetryPartitionMaintainTask.LOAD_PARTITIONS_SQL, String.class))
                .thenReturn(List.of("p202604"));

        assertThrows(IllegalStateException.class, () -> task.maintainMonthlyPartitions(12));

        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }

    private Clock fixedClock(int year, int month, int day, int hour, int minute)
    {
        return Clock.fixed(LocalDateTime.of(year, month, day, hour, minute).atZone(ZONE_ID).toInstant(), ZONE_ID);
    }

    private List<String> partitions(String startInclusive, String endInclusive)
    {
        List<String> partitions = new ArrayList<>();
        int current = Integer.parseInt(startInclusive);
        int end = Integer.parseInt(endInclusive);
        while (current <= end)
        {
            partitions.add("p" + current);
            int year = current / 100;
            int month = current % 100;
            if (month == 12)
            {
                current = (year + 1) * 100 + 1;
            }
            else
            {
                current = year * 100 + month + 1;
            }
        }
        partitions.add("pmax");
        return partitions;
    }
}
