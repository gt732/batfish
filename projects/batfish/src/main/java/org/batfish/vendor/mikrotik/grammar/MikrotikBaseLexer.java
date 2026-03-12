package org.batfish.vendor.mikrotik.grammar;

import org.antlr.v4.runtime.CharStream;
import org.batfish.grammar.BatfishLexer;

/** MikroTik lexer base class providing additional functionality on top of {@link BatfishLexer}. */
public abstract class MikrotikBaseLexer extends BatfishLexer {

  public MikrotikBaseLexer(CharStream input) {
    super(input);
  }
}
