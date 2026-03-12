package org.batfish.vendor.mikrotik.grammar;

import org.antlr.v4.runtime.TokenStream;
import org.batfish.grammar.BatfishParser;

/** MikroTik parser base class providing additional functionality on top of {@link BatfishParser}. */
public abstract class MikrotikBaseParser extends BatfishParser {

  public MikrotikBaseParser(TokenStream input) {
    super(input);
  }
}
