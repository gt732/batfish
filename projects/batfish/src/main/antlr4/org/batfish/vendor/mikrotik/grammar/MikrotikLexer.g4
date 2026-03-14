lexer grammar MikrotikLexer;

options {
   superClass = 'MikrotikBaseLexer';
}

NEWLINE
:
  [\r]? [\n]
;

WS
:
  [ \t]+ -> channel(HIDDEN)
;

COMMENT_LINE
:
  '#' ~[\r\n]* -> channel(HIDDEN)
;

LBRACK
:
  '['
;

RBRACK
:
  ']'
;

SLASH
:
  '/'
;

EQUALS
:
  '='
;

COMMA
:
  ','
;

INTERFACE
:
  'interface'
;

BRIDGE
:
  'bridge'
;

BONDING
:
  'bonding'
;

PORT
:
  'port'
;

PRIORITY
:
  'priority'
;

ETHERNET
:
  'ethernet'
;

WIRELESS
:
  'wireless'
;

VLAN
:
  'vlan'
;

GRE
:
  'gre'
;

IPIP
:
  'ipip'
;

EOIP
:
  'eoip'
;

WIREGUARD
:
  'wireguard'
;

PEERS
:
  'peers'
;

PREEMPTION_MODE
:
  'preemption-mode'
;

VLAN_ID
:
  'vlan-id'
;

SLAVES
:
  'slaves'
;

MODE
:
  'mode'
;

LACP_RATE
:
  'lacp-rate'
;

LIST
:
  'list'
;

SECURITY_PROFILES
:
  'security-profiles'
;

IP
:
  'ip'
;

ADDRESS
:
  'address'
;

ROUTE
:
  'route'
;

DNS
:
  'dns'
;

FIREWALL
:
  'firewall'
;

FILTER
:
  'filter'
;

ADDRESS_LIST
:
  'address-list'
;

DHCP_CLIENT
:
  'dhcp-client'
;

SYSTEM
:
  'system'
;

IDENTITY
:
  'identity'
;

CLOCK
:
  'clock'
;

LOGGING
:
  'logging'
;

ROUTING
:
  'routing'
;

BGP
:
  'bgp'
;

INSTANCE
:
  'instance'
;

INTERVAL
:
  'interval'
;

PEER
:
  'peer'
;

SNMP
:
  'snmp'
;

SRC_ADDRESS_LIST
:
  'src-address-list'
;

COMMUNITY
:
  'community'
;

TOOL
:
  'tool'
;

GRAPHING
:
  'graphing'
;

HOST
:
  'host'
;

NETWATCH
:
  'netwatch'
;

FIND
:
  'find'
;

DEFAULT_NAME
:
  'default-name'
;

ADD
:
  'add'
;

SET
:
  'set'
;

NAME
:
  'name'
;

DISABLED
:
  'disabled'
;

COMMENT
:
  'comment'
;

DST_ADDRESS
:
  'dst-address'
;

DST_ADDRESS_LIST
:
  'dst-address-list'
;

GATEWAY
:
  'gateway'
;

DISTANCE
:
  'distance'
;

DOWN_SCRIPT
:
  'down-script'
;

BANDWIDTH
:
  'bandwidth'
;

MTU
:
  'mtu'
;

NAT
:
  'nat'
;

LOCAL_ADDRESS
:
  'local-address'
;

REMOTE_ADDRESS
:
  'remote-address'
;

TUNNEL_ID
:
  'tunnel-id'
;

LISTEN_PORT
:
  'listen-port'
;

PVID
:
  'pvid'
;

DISABLE_RUNNING_CHECK
:
  'disable-running-check'
;

NETWORK
:
  'network'
;

YES
:
  'yes'
;

NO
:
  'no'
;

TAGGED
:
  'tagged'
;

TIMEOUT
:
  'timeout'
;

UNTAGGED
:
  'untagged'
;

UP_SCRIPT
:
  'up-script'
;

V3_PROTOCOL
:
  'v3-protocol'
;

VERSION
:
  'version'
;

VIRTUAL_ADDRESS
:
  'virtual-address'
;

VLAN_IDS
:
  'vlan-ids'
;

VRID
:
  'vrid'
;

VRRP
:
  'vrrp'
;

IP_PREFIX
:
  DIGIT+ '.' DIGIT+ '.' DIGIT+ '.' DIGIT+ '/' DIGIT+
;

IP_ADDRESS
:
  DIGIT+ '.' DIGIT+ '.' DIGIT+ '.' DIGIT+
;

QUOTED_STRING
:
  '"' (ESC | ~["\\\r\n])* '"'
;

NUMBER
:
  DIGIT+
;

IDENTIFIER
:
  [A-Za-z_][A-Za-z0-9_.:+/-]*
;

WORD
:
  ~[ \t\r\n#/=,"]+
;

fragment DIGIT
:
  [0-9]
;

fragment ESC
:
  '\\' .
;
