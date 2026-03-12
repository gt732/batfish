parser grammar MikrotikParser;

options {
   superClass = 'MikrotikBaseParser';
   tokenVocab = MikrotikLexer;
}

import Mikrotik_common, Mikrotik_interface;

mikrotik_configuration
:
  NEWLINE*
  (
    statement NEWLINE*
  )*
  EOF
;

statement
:
  interface_command
  | ip_command
  | routing_command
  | system_command
  | snmp_command
  | tool_command
  | generic_command
  | line_command
;

interface_command
:
  SLASH INTERFACE interface_type_command
;

interface_type_command
:
  interface_bridge_add
  | interface_bridge_port_add
  | interface_ethernet_set
  | WIRELESS SECURITY_PROFILES command_tail?
  | command_tail?
;

ip_command
:
  SLASH IP (ip_address_command | ip_other_command)
;

ip_address_command
:
  ip_address_add
  | ADDRESS command_tail?
;

ip_other_command
:
  ip_subpath command_tail?
;

ip_subpath
:
  ROUTE
  | DNS
  | FIREWALL FILTER
  | FIREWALL ADDRESS_LIST
  | DHCP_CLIENT
;

routing_command
:
  SLASH ROUTING routing_subpath command_tail?
;

routing_subpath
:
  BGP INSTANCE
  | BGP PEER
  | word word?
;

system_command
:
  SLASH SYSTEM system_subpath command_tail?
;

system_subpath
:
  IDENTITY
  | CLOCK
  | LOGGING
;

snmp_command
:
  SLASH SNMP COMMUNITY command_tail?
;

tool_command
:
  SLASH TOOL GRAPHING word? command_tail?
;

generic_command
:
  command_path command_tail?
;

command_path
:
  SLASH word (SLASH word)*
;

command_tail
:
  line_command
;

line_command
:
  command_verb command_argument*
;

command_verb
:
  word
;

command_argument
:
  bracket_expression
  | key_value_parameter
  | value_atom
;

bracket_expression
:
  LBRACK line_command RBRACK
;
