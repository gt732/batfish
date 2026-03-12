package org.batfish.vendor.mikrotik.grammar;

import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.vendor.mikrotik.grammar.MikrotikParser.Mikrotik_configurationContext;

/** Combined parser for MikroTik RouterOS configurations. */
public class MikrotikCombinedParser
    extends BatfishCombinedParser<MikrotikParser, MikrotikLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(
          MikrotikLexer.NEWLINE, "\n");

  public MikrotikCombinedParser(String input, GrammarSettings settings) {
    super(
        MikrotikParser.class,
        MikrotikLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public Mikrotik_configurationContext parse() {
    return _parser.mikrotik_configuration();
  }
}
