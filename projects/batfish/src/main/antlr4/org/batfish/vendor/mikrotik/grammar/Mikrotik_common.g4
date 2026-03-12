parser grammar Mikrotik_common;

options {
  tokenVocab = MikrotikLexer;
}

word
:
  INTERFACE
  | BRIDGE
  | PORT
  | ETHERNET
  | WIRELESS
  | SECURITY_PROFILES
  | IP
  | ADDRESS
  | ROUTE
  | DNS
  | FIREWALL
  | FILTER
  | ADDRESS_LIST
  | DHCP_CLIENT
  | SYSTEM
  | IDENTITY
  | CLOCK
  | LOGGING
  | ROUTING
  | BGP
  | INSTANCE
  | PEER
  | SNMP
  | COMMUNITY
  | TOOL
  | GRAPHING
  | FIND
  | ADD
  | SET
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

