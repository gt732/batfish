parser grammar Mikrotik_interface;

options {
  tokenVocab = MikrotikLexer;
}

interface_bridge_add
:
  BRIDGE ADD interface_bridge_add_prop*
;

interface_bridge_add_prop
:
  if_prop_name
  | if_prop_comment
  | key_value_parameter
;

interface_ethernet_set
:
  ETHERNET SET bracket_expression? interface_ethernet_set_prop*
;

interface_ethernet_set_prop
:
  if_prop_name
  | if_prop_disabled
  | if_prop_comment
  | if_prop_mtu
  | if_prop_disable_running_check
  | key_value_parameter
;

interface_bridge_port_add
:
  BRIDGE PORT ADD interface_bridge_port_add_prop*
;

interface_bridge_port_add_prop
:
  if_prop_bridge
  | if_prop_interface
  | if_bridge_port_prop_pvid
  | key_value_parameter
;

if_bridge_port_prop_pvid
:
  PVID EQUALS parameter_value
;

interface_bridge_vlan_add
:
  BRIDGE VLAN ADD interface_bridge_vlan_add_prop*
;

interface_bridge_vlan_add_prop
:
  if_prop_bridge
  | if_bridge_vlan_prop_vlan_ids
  | if_bridge_vlan_prop_tagged
  | if_bridge_vlan_prop_untagged
  | key_value_parameter
;

if_bridge_vlan_prop_vlan_ids
:
  VLAN_IDS EQUALS parameter_value
;

if_bridge_vlan_prop_tagged
:
  TAGGED EQUALS parameter_value
;

if_bridge_vlan_prop_untagged
:
  UNTAGGED EQUALS parameter_value
;

interface_vlan_add
:
  VLAN ADD interface_vlan_add_prop*
;

interface_vlan_add_prop
:
  if_prop_name
  | if_vlan_prop_vlan_id
  | if_prop_interface
  | if_prop_disabled
  | if_prop_mtu
  | if_prop_comment
  | key_value_parameter
;

if_prop_name
:
  NAME EQUALS parameter_value
;

if_vlan_prop_vlan_id
:
  VLAN_ID EQUALS parameter_value
;

if_prop_disabled
:
  DISABLED EQUALS parameter_value
;

if_prop_comment
:
  COMMENT EQUALS parameter_value
;

if_prop_mtu
:
  MTU EQUALS parameter_value
;

if_prop_disable_running_check
:
  DISABLE_RUNNING_CHECK EQUALS parameter_value
;

if_prop_bridge
:
  BRIDGE EQUALS parameter_value
;

if_prop_interface
:
  INTERFACE EQUALS parameter_value
;

ip_address_add
:
  ADDRESS ADD ip_address_add_prop*
;

ip_address_add_prop
:
  ip_addr_prop_address
  | ip_addr_prop_interface
  | ip_addr_prop_network
  | ip_addr_prop_comment
  | key_value_parameter
;

ip_addr_prop_address
:
  ADDRESS EQUALS parameter_value
;

ip_addr_prop_interface
:
  INTERFACE EQUALS parameter_value
;

ip_addr_prop_network
:
  NETWORK EQUALS parameter_value
;

ip_addr_prop_comment
:
  COMMENT EQUALS parameter_value
;
