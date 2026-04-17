-- Pv telemetry monthly partition maintenance script
-- Default window: keep the current month plus the next 11 months.
-- Run at: 02:00 on the first day of each month.

set @retain_months := 12;

delimiter $$

drop procedure if exists sp_maintain_pv_telemetry_partitions $$

create procedure sp_maintain_pv_telemetry_partitions(in p_retain_months int)
begin
  declare v_retain_months int default ifnull(nullif(p_retain_months, 0), 12);
  declare v_base_month date default str_to_date(date_format(curdate(), '%Y-%m-01'), '%Y-%m-%d');
  declare v_partition_month date;
  declare v_next_boundary date;
  declare v_partition_name varchar(16);
  declare v_exists int default 0;
  declare v_idx int default 0;
  declare v_drop_list text default null;

  while v_idx < v_retain_months do
    set v_partition_month = date_add(v_base_month, interval v_idx month);
    set v_next_boundary = date_add(v_partition_month, interval 1 month);
    set v_partition_name = date_format(v_partition_month, 'p%Y%m');

    select count(*)
      into v_exists
      from information_schema.partitions
     where table_schema = database()
       and table_name = 'pv_telemetry'
       and partition_name = v_partition_name;

    if v_exists = 0 then
      set @ddl = concat(
        'alter table pv_telemetry reorganize partition pmax into (',
        'partition ', v_partition_name, ' values less than (to_days(''',
        date_format(v_next_boundary, '%Y-%m-%d'),
        ''')), partition pmax values less than maxvalue)'
      );
      prepare stmt from @ddl;
      execute stmt;
      deallocate prepare stmt;
    end if;

    set v_idx = v_idx + 1;
  end while;

  select group_concat(partition_name order by partition_name separator ', ')
    into v_drop_list
    from information_schema.partitions
   where table_schema = database()
     and table_name = 'pv_telemetry'
     and partition_name regexp '^p[0-9]{6}$'
     and str_to_date(concat(substring(partition_name, 2), '01'), '%Y%m%d') < v_base_month;

  if v_drop_list is not null and v_drop_list <> '' then
    set @ddl = concat('alter table pv_telemetry drop partition ', v_drop_list);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end $$

delimiter ;

call sp_maintain_pv_telemetry_partitions(@retain_months);

drop procedure if exists sp_maintain_pv_telemetry_partitions;
