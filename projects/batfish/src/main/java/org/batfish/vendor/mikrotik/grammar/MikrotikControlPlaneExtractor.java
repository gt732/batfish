package org.batfish.vendor.mikrotik.grammar;

import static org.batfish.vendor.mikrotik.representation.MikrotikStructureType.INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.INTERFACE_SELF_REFERENCE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.IP_ADDRESS_INTERFACE;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.vendor.VendorConfiguration;
import org.batfish.vendor.mikrotik.representation.MikrotikConfiguration;
import org.batfish.vendor.mikrotik.representation.MikrotikInterface;

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

  @Override
  public void enterInterface_bridge_add(MikrotikParser.Interface_bridge_addContext ctx) {
    Optional<String> maybeName =
        ctx.interface_bridge_add_prop().stream()
            .filter(prop -> prop.if_prop_name() != null)
            .map(prop -> extractParameterValue(prop.if_prop_name().parameter_value()))
            .findFirst();
    if (maybeName.isEmpty()) {
      return;
    }
    String name = maybeName.get();
    MikrotikInterface iface = getOrCreateInterface(name, "bridge");
    iface.setType("bridge");
    _configuration.defineStructure(INTERFACE, name, ctx);
    _configuration.referenceStructure(
        INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterInterface_ethernet_set(MikrotikParser.Interface_ethernet_setContext ctx) {
    Optional<String> maybeName =
        ctx.interface_ethernet_set_prop().stream()
            .filter(prop -> prop.if_prop_name() != null)
            .map(prop -> extractParameterValue(prop.if_prop_name().parameter_value()))
            .findFirst();
    if (maybeName.isEmpty()) {
      return;
    }
    String name = maybeName.get();
    MikrotikInterface iface = getOrCreateInterface(name, "ethernet");
    iface.setType("ethernet");
    for (MikrotikParser.Interface_ethernet_set_propContext prop : ctx.interface_ethernet_set_prop()) {
      if (prop.if_prop_disabled() != null) {
        iface.setDisabled(parseBoolean(extractParameterValue(prop.if_prop_disabled().parameter_value())));
      } else if (prop.if_prop_mtu() != null) {
        iface.setMtu(Integer.parseInt(extractParameterValue(prop.if_prop_mtu().parameter_value())));
      }
    }
    _configuration.defineStructure(INTERFACE, name, ctx);
    _configuration.referenceStructure(
        INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterIp_address_add(MikrotikParser.Ip_address_addContext ctx) {
    Optional<String> maybeAddress =
        ctx.ip_address_add_prop().stream()
            .filter(prop -> prop.ip_addr_prop_address() != null)
            .map(prop -> extractParameterValue(prop.ip_addr_prop_address().parameter_value()))
            .findFirst();
    Optional<String> maybeIfaceName =
        ctx.ip_address_add_prop().stream()
            .filter(prop -> prop.ip_addr_prop_interface() != null)
            .map(prop -> extractParameterValue(prop.ip_addr_prop_interface().parameter_value()))
            .findFirst();
    if (maybeAddress.isEmpty() || maybeIfaceName.isEmpty()) {
      return;
    }
    String ifaceName = maybeIfaceName.get();
    _configuration.referenceStructure(
        INTERFACE, ifaceName, IP_ADDRESS_INTERFACE, ctx.getStart().getLine());
    MikrotikInterface iface = _configuration.getInterfaces().get(ifaceName);
    if (iface == null) {
      _w.addWarning(
          ctx,
          getFullText(ctx),
          _parser,
          String.format("ip address entry references undefined interface %s", ifaceName));
      return;
    }
    iface.addAddress(ConcreteInterfaceAddress.parse(maybeAddress.get()));
  }

  private @Nonnull String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    return _text.substring(start, end + 1);
  }

  private @Nonnull String extractParameterValue(MikrotikParser.Parameter_valueContext ctx) {
    String raw = ctx.getText();
    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
      return raw.substring(1, raw.length() - 1);
    }
    return raw;
  }

  private static boolean parseBoolean(String value) {
    return value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true");
  }

  private @Nonnull MikrotikInterface getOrCreateInterface(String name, String type) {
    return _configuration.getInterfaces().computeIfAbsent(name, key -> new MikrotikInterface(key, type));
  }

  private final @Nonnull MikrotikConfiguration _configuration;
  private final @Nonnull MikrotikCombinedParser _parser;
  private final @Nonnull SilentSyntaxCollection _silentSyntax;
  private final @Nonnull String _text;
  private final @Nonnull Warnings _w;
}
