parser grammar MikrotikParser;

options {
   superClass = 'org.batfish.grammar.BatfishParser';
   tokenVocab = MikrotikLexer;
}

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
  command_path
  | command_statement
;

command_path
:
  SLASH WORD
  (
    SLASH? WORD
  )*
;

command_statement
:
  WORD
  (
    WORD
    | EQUALS WORD
    | SLASH WORD
  )*
;
