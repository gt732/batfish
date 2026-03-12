parser grammar Mikrotik_static;

options {
  tokenVocab = MikrotikLexer;
}

ip_route_add
:
  ROUTE ADD ip_route_add_prop*
;

ip_route_add_prop
:
  ip_route_prop_dst_address
  | ip_route_prop_gateway
  | ip_route_prop_distance
  | ip_route_prop_comment
  | key_value_parameter
;

ip_route_prop_dst_address
:
  DST_ADDRESS EQUALS parameter_value
;

ip_route_prop_gateway
:
  GATEWAY EQUALS parameter_value
;

ip_route_prop_distance
:
  DISTANCE EQUALS parameter_value
;

ip_route_prop_comment
:
  COMMENT EQUALS parameter_value
;
