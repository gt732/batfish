parser grammar MikrotikParser;

options {
   superClass = 'MikrotikBaseParser';
   tokenVocab = MikrotikLexer;
}

import Mikrotik_common;

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
  interface_path command_tail?
;

interface_path
:
  SLASH INTERFACE interface_subpath
;

interface_subpath
:
  BRIDGE (PORT)?
  | ETHERNET
  | WIRELESS SECURITY_PROFILES
;

ip_command
:
  SLASH IP ip_subpath command_tail?
;

ip_subpath
:
  ADDRESS
  | ROUTE
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
