-- P1-1: 为 pv_inverter_model 补 Modbus 寄存器模板列
ALTER TABLE pv_inverter_model
  ADD COLUMN register_profile text DEFAULT NULL COMMENT 'Modbus 寄存器地址模板' AFTER mqtt_protocol;
