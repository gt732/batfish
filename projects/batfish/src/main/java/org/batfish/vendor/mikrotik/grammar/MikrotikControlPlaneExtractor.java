package org.batfish.vendor.mikrotik.grammar;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.vendor.VendorConfiguration;
import org.batfish.vendor.mikrotik.representation.MikrotikConfiguration;

/** Extracts a {@link MikrotikConfiguration} from a MikroTik RouterOS parse tree. */
@ParametersAreNonnullByDefault
public class MikrotikControlPlaneExtractor extends MikrotikParserBaseListener
    implements ControlPlaneExtractor {

  public MikrotikControlPlaneExtractor(
      String text,
      MikrotikCombinedParser parser,
      Warnings warnings,
      SilentSyntaxCollection silentSyntax) {
    _text = text;
    _parser = parser;
    _w = warnings;
    _silentSyntax = silentSyntax;
    _configuration = new MikrotikConfiguration();
  }

  @Override
  public @Nonnull VendorConfiguration getVendorConfiguration() {
    return _configuration;
  }

  @Override
  public void processParseTree(NetworkSnapshot snapshot, ParserRuleContext tree) {
    ParseTreeWalker walker = new BatfishParseTreeWalker(_parser);
    walker.walk(this, tree);
  }

  private final @Nonnull MikrotikConfiguration _configuration;
  private final @Nonnull MikrotikCombinedParser _parser;
  private final @Nonnull SilentSyntaxCollection _silentSyntax;
  @SuppressWarnings("unused")
  private final @Nonnull String _text;
  @SuppressWarnings("unused")
  private final @Nonnull Warnings _w;
}
