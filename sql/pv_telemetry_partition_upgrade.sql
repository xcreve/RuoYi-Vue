-- Pv telemetry partition migration script
-- Preconditions:
-- 1. MySQL 8.0+
-- 2. Execute in a maintenance window
-- 3. Verify business writes are stopped before rename
--
-- Reason:
-- MySQL RANGE PARTITION requires every unique key to include the partition key.
-- The current pv_telemetry primary key is (telemetry_id), so partitioning by
-- collect_time requires rebuilding the table with a composite primary key.

drop table if exists pv_telemetry_part;

create table pv_telemetry_part (
  telemetry_id bigint(20) not null auto_increment comment '遥测ID',
  inverter_id bigint(20) not null comment '逆变器ID',
  active_power decimal(12,2) default 0.00 comment '有功功率(kW)',
  daily_yield decimal(12,2) default 0.00 comment '当日发电量(kWh)',
  total_yield decimal(14,2) default 0.00 comment '总发电量(kWh)',
  voltage decimal(10,2) default 0.00 comment '电压(V)',
  current decimal(10,2) default 0.00 comment '电流(A)',
  collect_time datetime not null comment '采集时间',
  legacy_firebase_id varchar(128) default null comment '历史Firebase ID',
  primary key (telemetry_id, collect_time),
  key idx_pv_telemetry_collect_time (collect_time),
  key idx_pv_telemetry_inverter_time (inverter_id, collect_time)
) engine=innodb comment='光伏遥测数据（分区版）'
partition by range (to_days(collect_time)) (
  partition p202604 values less than (to_days('2026-05-01')),
  partition p202605 values less than (to_days('2026-06-01')),
  partition p202606 values less than (to_days('2026-07-01')),
  partition pmax values less than maxvalue
);

insert into pv_telemetry_part (
  telemetry_id, inverter_id, active_power, daily_yield, total_yield, voltage, current, collect_time, legacy_firebase_id
)
select telemetry_id, inverter_id, active_power, daily_yield, total_yield, voltage, current, collect_time, legacy_firebase_id
from pv_telemetry
order by collect_time asc, telemetry_id asc;

-- Validate row count and sampling before swap.
-- select count(1) from pv_telemetry;
-- select count(1) from pv_telemetry_part;

-- Recommended swap:
-- rename table pv_telemetry to pv_telemetry_legacy,
--              pv_telemetry_part to pv_telemetry;

-- Example monthly maintenance:
-- alter table pv_telemetry reorganize partition pmax into (
--   partition p202607 values less than (to_days('2026-08-01')),
--   partition pmax values less than maxvalue
-- );
