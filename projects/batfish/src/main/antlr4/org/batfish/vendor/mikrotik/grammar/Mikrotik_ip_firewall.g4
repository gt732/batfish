parser grammar Mikrotik_ip_firewall;

options {
  tokenVocab = MikrotikLexer;
}

ip_firewall_address_list_add
:
  FIREWALL ADDRESS_LIST ADD ip_firewall_address_list_add_prop*
;

ip_firewall_address_list_add_prop
:
  ip_fw_al_prop_list
  | ip_fw_al_prop_address
  | ip_fw_al_prop_comment
  | ip_fw_al_prop_disabled
  | key_value_parameter
;

ip_fw_al_prop_list
:
  LIST EQUALS parameter_value
;

ip_fw_al_prop_address
:
  ADDRESS EQUALS parameter_value
;

ip_fw_al_prop_comment
:
  COMMENT EQUALS parameter_value
;

ip_fw_al_prop_disabled
:
  DISABLED EQUALS parameter_value
;

ip_firewall_filter_add
:
  FIREWALL FILTER ADD ip_firewall_filter_add_prop*
;

ip_firewall_filter_add_prop
:
  ip_fw_filter_prop_chain
  | ip_fw_filter_prop_action
  | ip_fw_filter_prop_src_address_list
  | ip_fw_filter_prop_dst_address_list
  | ip_fw_filter_prop_protocol
  | ip_fw_filter_prop_dst_port
  | ip_fw_filter_prop_src_port
  | key_value_parameter
;

ip_fw_filter_prop_chain
:
  CHAIN EQUALS parameter_value
;

ip_fw_filter_prop_action
:
  ACTION EQUALS parameter_value
;

ip_fw_filter_prop_src_address_list
:
  SRC_ADDRESS_LIST EQUALS parameter_value
;

ip_fw_filter_prop_dst_address_list
:
  DST_ADDRESS_LIST EQUALS parameter_value
;

ip_fw_filter_prop_protocol
:
  PROTOCOL EQUALS parameter_value
;

ip_fw_filter_prop_dst_port
:
  DST_PORT EQUALS parameter_value
;

ip_fw_filter_prop_src_port
:
  SRC_PORT EQUALS parameter_value
;

ip_firewall_nat_add
:
  FIREWALL NAT ADD ip_firewall_nat_add_prop*
;

ip_firewall_nat_add_prop
:
  ip_fw_nat_prop_src_address_list
  | ip_fw_nat_prop_dst_address_list
  | key_value_parameter
;

ip_fw_nat_prop_src_address_list
:
  SRC_ADDRESS_LIST EQUALS parameter_value
;

ip_fw_nat_prop_dst_address_list
:
  DST_ADDRESS_LIST EQUALS parameter_value
;
