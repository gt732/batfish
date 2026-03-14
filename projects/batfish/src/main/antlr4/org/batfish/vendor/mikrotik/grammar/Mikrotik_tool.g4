parser grammar Mikrotik_tool;

options {
  tokenVocab = MikrotikLexer;
}

tool_netwatch_add
:
  NETWATCH ADD tool_netwatch_add_prop*
;

tool_netwatch_add_prop
:
  tool_netwatch_prop_host
  | tool_netwatch_prop_comment
  | tool_netwatch_prop_disabled
  | tool_netwatch_prop_interval
  | tool_netwatch_prop_timeout
  | tool_netwatch_prop_up_script
  | tool_netwatch_prop_down_script
  | key_value_parameter
;

tool_netwatch_prop_host
:
  HOST EQUALS parameter_value
;

tool_netwatch_prop_comment
:
  COMMENT EQUALS parameter_value
;

tool_netwatch_prop_disabled
:
  DISABLED EQUALS parameter_value
;

tool_netwatch_prop_interval
:
  INTERVAL EQUALS parameter_value
;

tool_netwatch_prop_timeout
:
  TIMEOUT EQUALS parameter_value
;

tool_netwatch_prop_up_script
:
  UP_SCRIPT EQUALS parameter_value
;

tool_netwatch_prop_down_script
:
  DOWN_SCRIPT EQUALS parameter_value
;
