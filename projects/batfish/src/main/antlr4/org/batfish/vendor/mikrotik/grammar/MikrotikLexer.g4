lexer grammar MikrotikLexer;

options {
   superClass = 'MikrotikBaseLexer';
}

// Newline tokens
NEWLINE
:
  [\r]? [\n]
;

// Whitespace (hidden)
WS
:
  [ \t]+ -> channel(HIDDEN)
;

// RouterOS comment lines (lines starting with #)
COMMENT_LINE
:
  '#' ~[\r\n]* -> channel(HIDDEN)
;

// Slash-prefixed command paths (e.g. /ip address, /interface ethernet)
SLASH
:
  '/'
;

// Key=value separator
EQUALS
:
  '='
;

// General word token (identifiers, keywords, values)
WORD
:
  ~[ \t\r\n#/=]+
;
