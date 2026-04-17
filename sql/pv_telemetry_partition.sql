-- Pv telemetry monthly partition migration script
-- Preconditions:
-- 1. MySQL 8.0+
-- 2. Execute in a maintenance window after stopping writes to pv_telemetry
-- 3. Validate row counts before and after the table swap
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
  key idx_pv_telemetry_inverter_id (inverter_id),
  key idx_pv_telemetry_collect_time (collect_time),
  key idx_pv_telemetry_inverter_time (inverter_id, collect_time)
) engine=innodb comment='光伏遥测数据（分区版）'
partition by range (to_days(collect_time)) (
  partition p202604 values less than (to_days('2026-05-01')),
  partition p202605 values less than (to_days('2026-06-01')),
  partition p202606 values less than (to_days('2026-07-01')),
  partition p202607 values less than (to_days('2026-08-01')),
  partition p202608 values less than (to_days('2026-09-01')),
  partition p202609 values less than (to_days('2026-10-01')),
  partition p202610 values less than (to_days('2026-11-01')),
  partition p202611 values less than (to_days('2026-12-01')),
  partition p202612 values less than (to_days('2027-01-01')),
  partition p202701 values less than (to_days('2027-02-01')),
  partition p202702 values less than (to_days('2027-03-01')),
  partition p202703 values less than (to_days('2027-04-01')),
  partition pmax values less than maxvalue
);

insert into pv_telemetry_part (
  telemetry_id,
  inverter_id,
  active_power,
  daily_yield,
  total_yield,
  voltage,
  current,
  collect_time,
  legacy_firebase_id
)
select telemetry_id,
       inverter_id,
       active_power,
       daily_yield,
       total_yield,
       voltage,
       current,
       collect_time,
       legacy_firebase_id
from pv_telemetry
order by collect_time asc, telemetry_id asc;

-- Validation before swap:
-- select count(1) as source_rows from pv_telemetry;
-- select count(1) as target_rows from pv_telemetry_part;
-- select min(collect_time), max(collect_time) from pv_telemetry_part;
-- select partition_name, table_rows
--   from information_schema.partitions
--  where table_schema = database()
--    and table_name = 'pv_telemetry_part'
--  order by partition_ordinal_position;

-- Recommended swap:
-- rename table pv_telemetry to pv_telemetry_legacy,
--              pv_telemetry_part to pv_telemetry;

-- Post-swap cleanup after acceptance:
-- drop table if exists pv_telemetry_legacy;
