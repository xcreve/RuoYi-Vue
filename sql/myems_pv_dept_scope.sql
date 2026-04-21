-- P2-1: pv_station 补 dept_id 数据权限列
ALTER TABLE pv_station
  ADD COLUMN dept_id bigint(20) DEFAULT NULL COMMENT '数据权限部门ID' AFTER tag_id;
