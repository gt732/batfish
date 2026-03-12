package org.batfish.vendor.mikrotik.grammar;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.UnrecognizedLineToken;
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

  @Override
  public void enterGeneric_command(MikrotikParser.Generic_commandContext ctx) {
    _configuration.setUnrecognized(true);
    _w.addWarning(
        ctx,
        getFullText(ctx),
        _parser,
        String.format("Unsupported Mikrotik command path: %s", ctx.command_path().getText()));
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    int line = token.getLine();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    _configuration.setUnrecognized(true);

    if (token instanceof UnrecognizedLineToken) {
      UnrecognizedLineToken unrecognizedLineToken = (UnrecognizedLineToken) token;
      _w.getParseWarnings()
          .add(
              new ParseWarning(
                  line, lineText, unrecognizedLineToken.getParserContext(), "This syntax is unrecognized"));
      return;
    }
    _w.redFlagf(
        "Unrecognized Line: %d: %s SUBSEQUENT LINES MAY NOT BE PROCESSED CORRECTLY", line, lineText);
  }

  private @Nonnull String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    return _text.substring(start, end + 1);
  }

  private final @Nonnull MikrotikConfiguration _configuration;
  private final @Nonnull MikrotikCombinedParser _parser;
  private final @Nonnull SilentSyntaxCollection _silentSyntax;
  private final @Nonnull String _text;
  private final @Nonnull Warnings _w;
}
