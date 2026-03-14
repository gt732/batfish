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
  | if_prop_mtu
  | if_prop_bandwidth
  | if_prop_comment
  | key_value_parameter
;

interface_bonding_add
:
  BONDING ADD interface_bonding_add_prop*
;

interface_bonding_add_prop
:
  if_prop_name
  | if_bonding_prop_slaves
  | if_bonding_prop_mode
  | if_bonding_prop_lacp_rate
  | if_prop_disabled
  | if_prop_mtu
  | if_prop_bandwidth
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
  | if_prop_bandwidth
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
  | if_prop_bandwidth
  | if_prop_comment
  | key_value_parameter
;

interface_gre_add
:
  GRE ADD interface_gre_add_prop*
;

interface_gre_add_prop
:
  if_prop_name
  | if_tunnel_prop_local_address
  | if_tunnel_prop_remote_address
  | if_prop_mtu
  | if_prop_bandwidth
  | if_prop_disabled
  | if_prop_comment
  | key_value_parameter
;

interface_ipip_add
:
  IPIP ADD interface_ipip_add_prop*
;

interface_ipip_add_prop
:
  if_prop_name
  | if_tunnel_prop_local_address
  | if_tunnel_prop_remote_address
  | if_prop_mtu
  | if_prop_bandwidth
  | if_prop_disabled
  | if_prop_comment
  | key_value_parameter
;

interface_eoip_add
:
  EOIP ADD interface_eoip_add_prop*
;

interface_eoip_add_prop
:
  if_prop_name
  | if_tunnel_prop_local_address
  | if_tunnel_prop_remote_address
  | if_eoip_prop_tunnel_id
  | if_prop_mtu
  | if_prop_bandwidth
  | if_prop_disabled
  | if_prop_comment
  | key_value_parameter
;

interface_wireguard_add
:
  WIREGUARD ADD interface_wireguard_add_prop*
;

interface_wireguard_add_prop
:
  if_prop_name
  | if_wireguard_prop_listen_port
  | if_prop_mtu
  | if_prop_bandwidth
  | if_prop_disabled
  | if_prop_comment
  | key_value_parameter
;

interface_vrrp_add
:
  VRRP ADD interface_vrrp_add_prop*
;

interface_vrrp_add_prop
:
  if_prop_name
  | if_prop_interface
  | if_vrrp_prop_vrid
  | if_vrrp_prop_priority
  | if_vrrp_prop_preemption_mode
  | if_vrrp_prop_version
  | if_vrrp_prop_v3_protocol
  | if_vrrp_prop_virtual_address
  | if_prop_disabled
  | if_prop_comment
  | if_prop_mtu
  | if_prop_bandwidth
  | key_value_parameter
;

if_vrrp_prop_vrid
:
  VRID EQUALS parameter_value
;

if_vrrp_prop_priority
:
  PRIORITY EQUALS parameter_value
;

if_vrrp_prop_preemption_mode
:
  PREEMPTION_MODE EQUALS parameter_value
;

if_vrrp_prop_version
:
  VERSION EQUALS parameter_value
;

if_vrrp_prop_v3_protocol
:
  V3_PROTOCOL EQUALS parameter_value
;

if_vrrp_prop_virtual_address
:
  VIRTUAL_ADDRESS EQUALS parameter_value
;

if_bonding_prop_slaves
:
  SLAVES EQUALS parameter_value
;

if_bonding_prop_mode
:
  MODE EQUALS parameter_value
;

if_bonding_prop_lacp_rate
:
  LACP_RATE EQUALS parameter_value
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

if_prop_bandwidth
:
  BANDWIDTH EQUALS parameter_value
;

if_tunnel_prop_local_address
:
  LOCAL_ADDRESS EQUALS parameter_value
;

if_tunnel_prop_remote_address
:
  REMOTE_ADDRESS EQUALS parameter_value
;

if_eoip_prop_tunnel_id
:
  TUNNEL_ID EQUALS parameter_value
;

if_wireguard_prop_listen_port
:
  LISTEN_PORT EQUALS parameter_value
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
