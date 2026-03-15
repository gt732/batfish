parser grammar Mikrotik_common;

options {
  tokenVocab = MikrotikLexer;
}

word
:
  INTERFACE
  | BRIDGE
  | BONDING
  | PORT
  | PROTOCOL
  | PRIORITY
  | ETHERNET
  | WIRELESS
  | VLAN
  | GRE
  | VLAN_ID
  | SLAVES
  | MODE
  | LACP_RATE
  | LIST
  | SECURITY_PROFILES
  | IP
  | ADDRESS
  | ACTION
  | ADDRESS_LIST
  | ROUTE
  | DST_ADDRESS
  | DST_ADDRESS_LIST
  | DST_PORT
  | GATEWAY
  | DISTANCE
  | DNS
  | FIREWALL
  | FILTER
  | DHCP_CLIENT
  | IDENTITY
  | CLOCK
  | LOGGING
  | ROUTING
  | BGP
  | CHAIN
  | INSTANCE
  | INTERVAL
  | PEER
  | PREEMPTION_MODE
  | SNMP
  | SRC_ADDRESS_LIST
  | SRC_PORT
  | SYSTEM
  | COMMUNITY
  | TOOL
  | GRAPHING
  | HOST
  | NETWATCH
  | FIND
  | DEFAULT_NAME
  | ADD
  | SET
  | NAME
  | DISABLED
  | COMMENT
  | DOWN_SCRIPT
  | MTU
  | NAT
  | PVID
  | TAGGED
  | TIMEOUT
  | UNTAGGED
  | UP_SCRIPT
  | V3_PROTOCOL
  | VERSION
  | VIRTUAL_ADDRESS
  | VLAN_IDS
  | VRID
  | VRRP
  | DISABLE_RUNNING_CHECK
  | NETWORK
  | YES
  | NO
  | IDENTIFIER
  | NUMBER
  | WORD
;

ip_address
:
  IP_ADDRESS
;

ip_prefix
:
  IP_PREFIX
;

quoted_string
:
  QUOTED_STRING
;

value_atom
:
  quoted_string
  | ip_prefix
  | ip_address
  | word
;

value_list
:
  value_atom (COMMA value_atom)+
;

parameter_value
:
  value_list
  | value_atom
;

key_value_parameter
:
  word EQUALS parameter_value
;
