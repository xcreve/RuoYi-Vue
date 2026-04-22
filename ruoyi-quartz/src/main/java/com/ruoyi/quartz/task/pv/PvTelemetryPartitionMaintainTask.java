package com.ruoyi.quartz.task.pv;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.ruoyi.system.metrics.pv.PvMetricsRecorder;

/**
 * 光伏遥测分区月度维护任务。
 */
@Component("pvTelemetryPartitionMaintainTask")
public class PvTelemetryPartitionMaintainTask
{
    static final String LOAD_PARTITIONS_SQL = "select partition_name "
            + "from information_schema.partitions "
            + "where table_schema = database() "
            + "and table_name = 'pv_telemetry' "
            + "and partition_name is not null "
            + "order by partition_ordinal_position";

    private static final Logger log = LoggerFactory.getLogger(PvTelemetryPartitionMaintainTask.class);
    private static final String TABLE_NAME = "pv_telemetry";
    private static final String PMAX = "pmax";
    private static final DateTimeFormatter PARTITION_NAME_FORMATTER = DateTimeFormatter.ofPattern("'p'yyyyMM");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final PvMetricsRecorder metricsRecorder;

    @Autowired
    public PvTelemetryPartitionMaintainTask(JdbcTemplate jdbcTemplate,
            ObjectProvider<PvMetricsRecorder> metricsRecorderProvider)
    {
        this(jdbcTemplate, Clock.systemDefaultZone(), metricsRecorderProvider.getIfAvailable());
    }

    PvTelemetryPartitionMaintainTask(JdbcTemplate jdbcTemplate, Clock clock)
    {
        this(jdbcTemplate, clock, null);
    }

    PvTelemetryPartitionMaintainTask(JdbcTemplate jdbcTemplate, Clock clock, PvMetricsRecorder metricsRecorder)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.metricsRecorder = metricsRecorder;
    }

    public void maintainMonthlyPartitions()
    {
        maintainMonthlyPartitions(12);
    }

    public void maintainMonthlyPartitions(Integer retentionMonths)
    {
        int months = retentionMonths == null || retentionMonths < 1 ? 12 : retentionMonths;
        YearMonth baseMonth = YearMonth.from(LocalDate.now(clock));
        List<String> partitionNames = loadPartitionNames();
        updatePartitionCount(partitionNames.size());
        if (partitionNames.isEmpty() || !partitionNames.contains(PMAX))
        {
            throw new IllegalStateException(
                    "pv_telemetry partition metadata missing pmax; run sql/pv_telemetry_partition.sql first");
        }

        Set<YearMonth> existingMonths = partitionNames.stream()
                .filter(this::isMonthlyPartition)
                .map(this::toYearMonth)
                .collect(Collectors.toCollection(TreeSet::new));

        List<YearMonth> missingMonths = new ArrayList<>();
        for (int i = 0; i < months; i++)
        {
            YearMonth month = baseMonth.plusMonths(i);
            if (!existingMonths.contains(month))
            {
                missingMonths.add(month);
            }
        }

        for (YearMonth missingMonth : missingMonths)
        {
            jdbcTemplate.execute(buildAddPartitionSql(missingMonth));
        }

        List<String> obsoletePartitions = partitionNames.stream()
                .filter(this::isMonthlyPartition)
                .filter(name -> toYearMonth(name).isBefore(baseMonth))
                .sorted()
                .collect(Collectors.toList());
        if (!obsoletePartitions.isEmpty())
        {
            jdbcTemplate.execute("alter table " + TABLE_NAME + " drop partition " + String.join(", ", obsoletePartitions));
        }

        log.info("PV telemetry partition maintenance finished. baseMonth={}, retentionMonths={}, addedPartitions={}, droppedPartitions={}",
                baseMonth, months, missingMonths.size(), obsoletePartitions.size());
        recordPartitionMaintainSuccess();
    }

    private List<String> loadPartitionNames()
    {
        return jdbcTemplate.queryForList(LOAD_PARTITIONS_SQL, String.class);
    }

    private boolean isMonthlyPartition(String partitionName)
    {
        return partitionName != null && partitionName.matches("p\\d{6}");
    }

    private YearMonth toYearMonth(String partitionName)
    {
        return YearMonth.parse(partitionName.substring(1), YEAR_MONTH_FORMATTER);
    }

    private String buildAddPartitionSql(YearMonth month)
    {
        LocalDate nextBoundary = month.plusMonths(1).atDay(1);
        return String.format(Locale.ROOT,
                "alter table %s reorganize partition %s into (partition %s values less than (to_days('%s')), partition %s values less than maxvalue)",
                TABLE_NAME,
                PMAX,
                PARTITION_NAME_FORMATTER.format(month.atDay(1)),
                nextBoundary,
                PMAX);
    }

    private void updatePartitionCount(int partitionCount)
    {
        if (metricsRecorder != null)
        {
            metricsRecorder.updateTelemetryPartitionCount(partitionCount);
        }
    }

    private void recordPartitionMaintainSuccess()
    {
        if (metricsRecorder != null)
        {
            metricsRecorder.recordPartitionMaintainSuccess(clock.instant());
        }
    }
}
