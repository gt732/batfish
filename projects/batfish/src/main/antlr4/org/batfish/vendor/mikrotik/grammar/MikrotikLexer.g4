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

PORT
:
  'port'
;

ETHERNET
:
  'ethernet'
;

WIRELESS
:
  'wireless'
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

PEER
:
  'peer'
;

SNMP
:
  'snmp'
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

FIND
:
  'find'
;

ADD
:
  'add'
;

SET
:
  'set'
;

YES
:
  'yes'
;

NO
:
  'no'
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
