-- MyEMS-PV business schema and menu bootstrap for RuoYi-Vue3

delete from sys_role_menu where menu_id between 2000 and 2199;
delete from sys_menu where menu_id between 2000 and 2199;
delete from sys_job where job_id = 100;
delete from sys_job where job_id = 101;
delete from sys_job where job_id = 102;

drop table if exists pv_alert;
drop table if exists pv_alert_rule;
drop table if exists pv_alert_channel;
drop table if exists pv_telemetry;
drop table if exists pv_inverter;
drop table if exists pv_gateway;
drop table if exists pv_station;
drop table if exists pv_inverter_model;
drop table if exists pv_station_tag;

create table pv_station_tag (
  tag_id bigint(20) not null auto_increment comment '标签ID',
  tag_name varchar(64) not null comment '标签名称',
  description varchar(255) default null comment '标签描述',
  legacy_firebase_id varchar(128) default null comment '历史Firebase ID',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (tag_id),
  unique key uk_pv_station_tag_name (tag_name)
) engine=innodb comment='光伏电站标签';

create table pv_station (
  station_id bigint(20) not null auto_increment comment '电站ID',
  station_name varchar(128) not null comment '电站名称',
  location varchar(255) default null comment '地理位置',
  capacity_mw decimal(10,2) default 0.00 comment '装机容量(MW)',
  tag_id bigint(20) default null comment '标签ID',
  legacy_firebase_id varchar(128) default null comment '历史Firebase ID',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (station_id),
  key idx_pv_station_tag_id (tag_id)
) engine=innodb comment='光伏电站';

create table pv_inverter_model (
  model_id bigint(20) not null auto_increment comment '型号ID',
  brand varchar(64) not null comment '品牌',
  model_name varchar(128) not null comment '型号',
  mqtt_protocol text comment 'MQTT协议说明',
  legacy_firebase_id varchar(128) default null comment '历史Firebase ID',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (model_id),
  unique key uk_pv_model_brand_name (brand, model_name)
) engine=innodb comment='逆变器品牌型号';

create table pv_gateway (
  gateway_id bigint(20) not null auto_increment comment '网关ID',
  station_id bigint(20) not null comment '所属电站ID',
  gateway_name varchar(128) not null comment '网关名称',
  gateway_type varchar(32) not null comment '网关类型',
  serial_number varchar(128) not null comment '序列号',
  status varchar(16) not null default 'offline' comment '状态',
  communication_type varchar(32) not null comment '通讯方式',
  protocol varchar(32) default null comment '采集协议',
  broker_url varchar(255) default null comment 'Broker地址',
  topic varchar(255) default null comment '订阅主题',
  polling_interval_sec int(11) default 60 comment '轮询间隔(秒)',
  last_seen datetime default null comment '最后在线时间',
  legacy_firebase_id varchar(128) default null comment '历史Firebase ID',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (gateway_id),
  unique key uk_pv_gateway_serial_number (serial_number),
  key idx_pv_gateway_station_id (station_id)
) engine=innodb comment='光伏接入网关';

create table pv_inverter (
  inverter_id bigint(20) not null auto_increment comment '逆变器ID',
  gateway_id bigint(20) not null comment '所属网关ID',
  model_id bigint(20) not null comment '型号ID',
  inverter_number varchar(64) default null comment '设备编号',
  serial_number varchar(128) not null comment '序列号',
  status varchar(16) not null default 'offline' comment '状态',
  last_seen datetime default null comment '最后在线时间',
  current_power decimal(12,2) default 0.00 comment '当前功率(kW)',
  daily_yield decimal(12,2) default 0.00 comment '当日发电量(kWh)',
  legacy_firebase_id varchar(128) default null comment '历史Firebase ID',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (inverter_id),
  unique key uk_pv_inverter_serial_number (serial_number),
  key idx_pv_inverter_gateway_id (gateway_id),
  key idx_pv_inverter_model_id (model_id)
) engine=innodb comment='光伏逆变器';

create table pv_telemetry (
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
) engine=innodb comment='光伏遥测数据'
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

create table pv_alert_channel (
  channel_id bigint(20) not null auto_increment comment '通道ID',
  channel_type varchar(32) not null comment '通道类型',
  target varchar(255) not null comment '目标地址或接收人',
  enabled tinyint(1) not null default 0 comment '是否启用',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (channel_id)
) engine=innodb comment='光伏告警推送通道';

create table pv_alert_rule (
  rule_id bigint(20) not null auto_increment comment '规则ID',
  level varchar(16) not null comment '告警级别',
  channel_id bigint(20) not null comment '通道ID',
  throttle_sec int not null default 300 comment '节流秒数',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (rule_id),
  key idx_pv_alert_rule_level (level),
  key idx_pv_alert_rule_channel_id (channel_id)
) engine=innodb comment='光伏告警推送规则';

create table pv_alert (
  alert_id bigint(20) not null auto_increment comment '告警ID',
  level varchar(16) not null comment '告警级别',
  content varchar(255) not null comment '告警内容',
  source varchar(128) default null comment '告警来源',
  occur_time datetime not null comment '发生时间',
  status varchar(16) not null default 'active' comment '状态',
  resolved_at datetime default null comment '处理时间',
  resolved_by varchar(64) default null comment '处理人',
  legacy_firebase_id varchar(128) default null comment '历史Firebase ID',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (alert_id),
  key idx_pv_alert_status (status),
  key idx_pv_alert_occur_time (occur_time)
) engine=innodb comment='系统告警';

insert into pv_station_tag values
('1', '华东片区', '华东区域示范电站', 'legacy-tag-east', 'admin', sysdate(), '', null, '默认演示标签'),
('2', '工商业屋顶', '工商业屋顶项目标签', 'legacy-tag-rooftop', 'admin', sysdate(), '', null, '默认演示标签');

insert into pv_station values
('1', '苏州工业园区一号站', '江苏省苏州市工业园区', 12.50, 1, 'legacy-station-001', 'admin', sysdate(), '', null, '迁移演示电站'),
('2', '宁波厂房屋顶站', '浙江省宁波市北仑区', 6.80, 2, 'legacy-station-002', 'admin', sysdate(), '', null, '迁移演示电站');

insert into pv_inverter_model values
('1', '华为', 'SUN2000-50KTL', 'topic: ems/gateway/{sn}/telemetry', 'legacy-model-001', 'admin', sysdate(), '', null, '默认演示型号'),
('2', '阳光电源', 'SG110CX', 'topic: ems/gateway/{sn}/telemetry', 'legacy-model-002', 'admin', sysdate(), '', null, '默认演示型号');

insert into pv_gateway values
('1', '1', '东区DTU-01', 'DTU', 'GW-001-EMS', 'online', 'MQTT', 'ModbusTCP', 'mqtt://broker.myems.local', 'ems/gateway/001', 60, date_sub(sysdate(), interval 5 minute), 'legacy-gateway-001', 'admin', sysdate(), '', null, '演示接入网关'),
('2', '2', '宁波边缘网关', 'EdgeGateway', 'GW-002-EMS', 'offline', 'Polling', 'ModbusRTU', null, null, 120, date_sub(sysdate(), interval 2 hour), 'legacy-gateway-002', 'admin', sysdate(), '', null, '演示接入网关');

insert into pv_inverter values
('1', '1', '1', 'INV-01', 'INV-SZ-001', 'online', date_sub(sysdate(), interval 3 minute), 86.40, 238.30, 'legacy-inverter-001', 'admin', sysdate(), '', null, '演示逆变器'),
('2', '1', '1', 'INV-02', 'INV-SZ-002', 'online', date_sub(sysdate(), interval 4 minute), 74.20, 215.70, 'legacy-inverter-002', 'admin', sysdate(), '', null, '演示逆变器'),
('3', '2', '2', 'INV-03', 'INV-NB-003', 'offline', date_sub(sysdate(), interval 2 hour), 0.00, 64.50, 'legacy-inverter-003', 'admin', sysdate(), '', null, '演示逆变器');

insert into pv_telemetry (telemetry_id, inverter_id, active_power, daily_yield, total_yield, voltage, current, collect_time, legacy_firebase_id) values
('1', '1', 65.00, 180.50, 12650.80, 516.20, 44.10, date_sub(sysdate(), interval 5 hour), 'legacy-telemetry-001'),
('2', '2', 58.00, 164.20, 11890.30, 510.70, 39.50, date_sub(sysdate(), interval 5 hour), 'legacy-telemetry-002'),
('3', '1', 71.00, 196.10, 12666.40, 518.60, 47.30, date_sub(sysdate(), interval 4 hour), 'legacy-telemetry-003'),
('4', '2', 63.00, 178.60, 11904.70, 512.10, 41.70, date_sub(sysdate(), interval 4 hour), 'legacy-telemetry-004'),
('5', '1', 79.00, 214.90, 12685.20, 520.40, 50.90, date_sub(sysdate(), interval 3 hour), 'legacy-telemetry-005'),
('6', '2', 69.00, 195.40, 11921.50, 514.90, 44.20, date_sub(sysdate(), interval 3 hour), 'legacy-telemetry-006'),
('7', '1', 83.00, 226.60, 12697.10, 522.10, 53.80, date_sub(sysdate(), interval 2 hour), 'legacy-telemetry-007'),
('8', '2', 72.00, 206.80, 11933.00, 516.30, 46.10, date_sub(sysdate(), interval 2 hour), 'legacy-telemetry-008'),
('9', '1', 86.40, 238.30, 12708.80, 523.80, 55.20, date_sub(sysdate(), interval 1 hour), 'legacy-telemetry-009'),
('10', '2', 74.20, 215.70, 11941.90, 518.00, 47.60, date_sub(sysdate(), interval 1 hour), 'legacy-telemetry-010'),
('11', '3', 0.00, 60.20, 8450.10, 0.00, 0.00, date_sub(sysdate(), interval 5 hour), 'legacy-telemetry-011'),
('12', '3', 0.00, 64.50, 8454.40, 0.00, 0.00, date_sub(sysdate(), interval 2 hour), 'legacy-telemetry-012');

insert into pv_alert_channel values
('1', 'webhook', 'https://example.com/myems/webhook', 0, 'admin', sysdate(), '', null, '示例 Webhook，默认关闭避免误推送'),
('2', 'dingtalk', 'https://oapi.dingtalk.com/robot/send?access_token=replace-me', 0, 'admin', sysdate(), '', null, '示例钉钉机器人，默认关闭');

insert into pv_alert_rule values
('1', 'warning', '1', 300, 'admin', sysdate(), '', null, 'warning 级别告警示例规则'),
('2', 'critical', '2', 300, 'admin', sysdate(), '', null, 'critical 级别告警示例规则');

insert into pv_alert values
('1', 'warning', '宁波边缘网关通信中断', '宁波边缘网关 / INV-03', date_sub(sysdate(), interval 30 minute), 'active', null, null, 'legacy-alert-001', 'admin', sysdate(), '', null, '默认演示告警'),
('2', 'info', '苏州工业园区一号站今日发电量达到 200kWh', '苏州工业园区一号站', date_sub(sysdate(), interval 90 minute), 'resolved', date_sub(sysdate(), interval 60 minute), 'admin', 'legacy-alert-002', 'admin', sysdate(), 'admin', sysdate(), '默认演示告警');

insert into sys_menu values('2000', '光伏运维', '0', '1', 'pv', '', '', '', 1, 0, 'M', '0', '0', '', 'dashboard', 'admin', sysdate(), '', null, 'MyEMS 光伏业务根目录');
insert into sys_menu values('2001', '监控大屏', '2000', '1', 'dashboard', 'pv/dashboard/index', '', '', 1, 0, 'C', '0', '0', 'pv:dashboard:view', 'dashboard', 'admin', sysdate(), '', null, '监控大屏菜单');
insert into sys_menu values('2002', '电站管理', '2000', '2', 'station', 'pv/station/index', '', '', 1, 0, 'C', '0', '0', 'pv:station:list', 'tree', 'admin', sysdate(), '', null, '电站管理菜单');
insert into sys_menu values('2003', '数据分析', '2000', '3', 'analysis', 'pv/analysis/index', '', '', 1, 0, 'C', '0', '0', 'pv:analysis:list', 'chart', 'admin', sysdate(), '', null, '数据分析菜单');
insert into sys_menu values('2004', '系统告警', '2000', '4', 'alert', 'pv/alert/index', '', '', 1, 0, 'C', '0', '0', 'pv:alert:list', 'bell', 'admin', sysdate(), '', null, '系统告警菜单');
insert into sys_menu values('2005', '设备管理', '2000', '5', 'device', '', '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', sysdate(), '', null, '设备管理目录');
insert into sys_menu values('2006', '接入网关', '2005', '1', 'gateway', 'pv/gateway/index', '', '', 1, 0, 'C', '0', '0', 'pv:gateway:list', 'monitor', 'admin', sysdate(), '', null, '接入网关菜单');
insert into sys_menu values('2007', '接入设备', '2005', '2', 'inverter', 'pv/inverter/index', '', '', 1, 0, 'C', '0', '0', 'pv:inverter:list', 'job', 'admin', sysdate(), '', null, '接入设备菜单');
insert into sys_menu values('2008', '基础资料', '2000', '6', 'basic', '', '', '', 1, 0, 'M', '0', '0', '', 'build', 'admin', sysdate(), '', null, '基础资料目录');
insert into sys_menu values('2009', '品牌型号', '2008', '1', 'model', 'pv/model/index', '', '', 1, 0, 'C', '0', '0', 'pv:model:list', 'build', 'admin', sysdate(), '', null, '品牌型号菜单');
insert into sys_menu values('2010', '电站标签', '2008', '2', 'stationTag', 'pv/stationTag/index', '', '', 1, 0, 'C', '0', '0', 'pv:stationTag:list', 'tag', 'admin', sysdate(), '', null, '电站标签菜单');

insert into sys_menu values('2101', '大屏模拟', '2001', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:dashboard:simulate', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2102', '电站新增', '2002', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:station:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2103', '电站修改', '2002', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:station:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2104', '电站删除', '2002', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:station:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2105', '标签新增', '2010', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:stationTag:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2106', '标签修改', '2010', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:stationTag:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2107', '标签删除', '2010', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:stationTag:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2108', '型号新增', '2009', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:model:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2109', '型号修改', '2009', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:model:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2110', '型号删除', '2009', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:model:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2111', '网关查看', '2006', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:gateway:view', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2112', '网关新增', '2006', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:gateway:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2113', '网关修改', '2006', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:gateway:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2114', '网关删除', '2006', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:gateway:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2115', '逆变器查看', '2007', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:inverter:view', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2116', '逆变器新增', '2007', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:inverter:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2117', '逆变器修改', '2007', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:inverter:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2118', '逆变器删除', '2007', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:inverter:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2119', '分析导出', '2003', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:analysis:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2120', '告警处理', '2004', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:alert:resolve', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2121', '告警一键处理', '2004', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'pv:alert:resolveAll', '#', 'admin', sysdate(), '', null, '');

insert into sys_role_menu(role_id, menu_id) values
('2', '2000'),
('2', '2001'),
('2', '2002'),
('2', '2003'),
('2', '2004'),
('2', '2005'),
('2', '2006'),
('2', '2007'),
('2', '2008'),
('2', '2009'),
('2', '2010'),
('2', '2111'),
('2', '2115');

insert into sys_job values
('100', '光伏遥测清理', 'DEFAULT', 'pvTelemetryTask.cleanupTelemetry(90)', '0 0 1 1 * ?', '3', '1', '0', 'admin', sysdate(), '', null, '每月1日清理90天前遥测数据');

insert into sys_job values
('101', '光伏网关心跳巡检', 'DEFAULT', 'pvGatewayPollingTask.heartbeatCheck(3)', '0 * * * * ?', '3', '1', '0', 'admin', sysdate(), '', null, '每分钟检查网关心跳并下沉离线状态');

insert into sys_job values
('102', '光伏遥测分区维护', 'DEFAULT', 'pvTelemetryPartitionMaintainTask.maintainMonthlyPartitions(12)', '0 0 2 1 * ?', '3', '1', '0', 'admin', sysdate(), '', null, '每月1日维护 pv_telemetry 当前月起 12 个月分区窗口');
