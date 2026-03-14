package org.batfish.vendor.mikrotik.grammar;

import static org.batfish.vendor.mikrotik.representation.MikrotikStructureType.INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureType.STATIC_ROUTE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BONDING_SLAVE_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_PORT_BRIDGE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_PORT_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_VLAN_BRIDGE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_VLAN_TAGGED_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_VLAN_UNTAGGED_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.INTERFACE_SELF_REFERENCE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.IP_ADDRESS_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.STATIC_ROUTE_SELF_REFERENCE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.VLAN_INTERFACE_PARENT;

import java.util.ArrayList;
import java.util.List;
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
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.vendor.VendorConfiguration;
import org.batfish.vendor.mikrotik.representation.MikrotikBridgePort;
import org.batfish.vendor.mikrotik.representation.MikrotikBridgeVlan;
import org.batfish.vendor.mikrotik.representation.MikrotikConfiguration;
import org.batfish.vendor.mikrotik.representation.MikrotikInterface;
import org.batfish.vendor.mikrotik.representation.MikrotikStaticRoute;

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
    _pendingInterfaceReferences = new ArrayList<>();
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
  public void exitMikrotik_configuration(MikrotikParser.Mikrotik_configurationContext ctx) {
    _pendingInterfaceReferences.stream()
        .filter(
            reference ->
                !_configuration.getMainVrf().getInterfaces().containsKey(reference._referencedInterface))
        .forEach(
            reference ->
                _w.addWarning(
                    reference._ctx,
                    getFullText(reference._ctx),
                    _parser,
                    reference._warningMessage));
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
    for (MikrotikParser.Interface_bridge_add_propContext prop : ctx.interface_bridge_add_prop()) {
      if (prop.if_prop_mtu() != null) {
        String rawMtu = extractParameterValue(prop.if_prop_mtu().parameter_value());
        try {
          iface.setMtu(Integer.parseInt(rawMtu));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid mtu value '%s' for interface %s", rawMtu, name));
        }
      } else if (prop.if_prop_bandwidth() != null) {
        String rawBw = extractParameterValue(prop.if_prop_bandwidth().parameter_value());
        try {
          iface.setBandwidth(Double.parseDouble(rawBw));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid bandwidth value '%s' for interface %s", rawBw, name));
        }
      }
    }
    _configuration.defineStructure(INTERFACE, name, ctx);
    _configuration.referenceStructure(
        INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterInterface_bonding_add(MikrotikParser.Interface_bonding_addContext ctx) {
    Optional<String> maybeName =
        ctx.interface_bonding_add_prop().stream()
            .filter(prop -> prop.if_prop_name() != null)
            .map(prop -> extractParameterValue(prop.if_prop_name().parameter_value()))
            .findFirst();
    if (maybeName.isEmpty()) {
      return;
    }
    String name = maybeName.get();
    MikrotikInterface iface = getOrCreateInterface(name, "bonding");
    iface.setType("bonding");
    iface.getSlaves().clear();
    for (MikrotikParser.Interface_bonding_add_propContext prop : ctx.interface_bonding_add_prop()) {
      if (prop.if_bonding_prop_slaves() != null) {
        String rawSlaves = extractParameterValue(prop.if_bonding_prop_slaves().parameter_value());
        for (String slave : rawSlaves.split(",")) {
          String trimmed = slave.trim();
          if (trimmed.isEmpty()) {
            continue;
          }
          iface.addSlave(trimmed);
          _pendingInterfaceReferences.add(
              new PendingInterfaceReference(
                  ctx,
                  trimmed,
                  String.format(
                      "Bonding interface %s references undefined slave interface %s",
                      name, trimmed)));
          _configuration.referenceStructure(
              INTERFACE, trimmed, BONDING_SLAVE_INTERFACE, ctx.getStart().getLine());
        }
      } else if (prop.if_bonding_prop_mode() != null) {
        iface.setBondingMode(extractParameterValue(prop.if_bonding_prop_mode().parameter_value()));
      } else if (prop.if_bonding_prop_lacp_rate() != null) {
        iface.setLacpRate(extractParameterValue(prop.if_bonding_prop_lacp_rate().parameter_value()));
      } else if (prop.if_prop_disabled() != null) {
        iface.setDisabled(parseBoolean(extractParameterValue(prop.if_prop_disabled().parameter_value())));
      } else if (prop.if_prop_mtu() != null) {
        String rawMtu = extractParameterValue(prop.if_prop_mtu().parameter_value());
        try {
          iface.setMtu(Integer.parseInt(rawMtu));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid mtu value '%s' for interface %s", rawMtu, name));
        }
      } else if (prop.if_prop_bandwidth() != null) {
        String rawBw = extractParameterValue(prop.if_prop_bandwidth().parameter_value());
        try {
          iface.setBandwidth(Double.parseDouble(rawBw));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid bandwidth value '%s' for interface %s", rawBw, name));
        }
      }
    }
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
    Optional<String> maybeDefaultName =
        Optional.ofNullable(ctx.bracket_expression())
            .map(MikrotikParser.Bracket_expressionContext::line_command)
            .stream()
            .flatMap(lineCommand -> lineCommand.command_argument().stream())
            .map(MikrotikParser.Command_argumentContext::key_value_parameter)
            .filter(param -> param != null && param.word() != null && param.parameter_value() != null)
            .filter(param -> param.word().getText().equalsIgnoreCase("default-name"))
            .map(param -> extractParameterValue(param.parameter_value()))
            .findFirst();
    Optional<String> ifaceName = maybeName.isPresent() ? maybeName : maybeDefaultName;
    if (ifaceName.isEmpty()) {
      return;
    }
    String name = ifaceName.get();
    MikrotikInterface iface = getOrCreateInterface(name, "ethernet");
    iface.setType("ethernet");
    for (MikrotikParser.Interface_ethernet_set_propContext prop : ctx.interface_ethernet_set_prop()) {
      if (prop.if_prop_disabled() != null) {
        iface.setDisabled(parseBoolean(extractParameterValue(prop.if_prop_disabled().parameter_value())));
      } else if (prop.if_prop_mtu() != null) {
        String rawMtu = extractParameterValue(prop.if_prop_mtu().parameter_value());
        try {
          iface.setMtu(Integer.parseInt(rawMtu));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid mtu value '%s' for interface %s", rawMtu, name));
        }
      } else if (prop.if_prop_bandwidth() != null) {
        String rawBw = extractParameterValue(prop.if_prop_bandwidth().parameter_value());
        try {
          iface.setBandwidth(Double.parseDouble(rawBw));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid bandwidth value '%s' for interface %s", rawBw, name));
        }
      }
    }
    _configuration.defineStructure(INTERFACE, name, ctx);
    _configuration.referenceStructure(
        INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterInterface_vlan_add(MikrotikParser.Interface_vlan_addContext ctx) {
    Optional<String> maybeName =
        ctx.interface_vlan_add_prop().stream()
            .filter(prop -> prop.if_prop_name() != null)
            .map(prop -> extractParameterValue(prop.if_prop_name().parameter_value()))
            .findFirst();
    if (maybeName.isEmpty()) {
      return;
    }
    String name = maybeName.get();
    MikrotikInterface iface = getOrCreateInterface(name, "vlan");
    iface.setType("vlan");
    for (MikrotikParser.Interface_vlan_add_propContext prop : ctx.interface_vlan_add_prop()) {
      if (prop.if_vlan_prop_vlan_id() != null) {
        String rawVlanId = extractParameterValue(prop.if_vlan_prop_vlan_id().parameter_value());
        try {
          iface.setVlanId(Integer.parseInt(rawVlanId));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid vlan-id value '%s' for interface %s", rawVlanId, name));
        }
      } else if (prop.if_prop_interface() != null) {
        String parentInterface = extractParameterValue(prop.if_prop_interface().parameter_value());
        iface.setParentInterface(parentInterface);
        _pendingInterfaceReferences.add(
            new PendingInterfaceReference(
                ctx,
                parentInterface,
                String.format(
                    "VLAN interface %s references undefined parent interface %s",
                    name, parentInterface)));
      } else if (prop.if_prop_disabled() != null) {
        iface.setDisabled(parseBoolean(extractParameterValue(prop.if_prop_disabled().parameter_value())));
      } else if (prop.if_prop_mtu() != null) {
        String rawMtu = extractParameterValue(prop.if_prop_mtu().parameter_value());
        try {
          iface.setMtu(Integer.parseInt(rawMtu));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid mtu value '%s' for interface %s", rawMtu, name));
        }
      } else if (prop.if_prop_bandwidth() != null) {
        String rawBw = extractParameterValue(prop.if_prop_bandwidth().parameter_value());
        try {
          iface.setBandwidth(Double.parseDouble(rawBw));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid bandwidth value '%s' for interface %s", rawBw, name));
        }
      }
    }
    Optional.ofNullable(iface.getParentInterface())
        .ifPresent(
            parent ->
                _configuration.referenceStructure(
                    INTERFACE, parent, VLAN_INTERFACE_PARENT, ctx.getStart().getLine()));
    _configuration.defineStructure(INTERFACE, name, ctx);
    _configuration.referenceStructure(
        INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterInterface_gre_add(MikrotikParser.Interface_gre_addContext ctx) {
    Optional<String> maybeName =
        ctx.interface_gre_add_prop().stream()
            .filter(prop -> prop.if_prop_name() != null)
            .map(prop -> extractParameterValue(prop.if_prop_name().parameter_value()))
            .findFirst();
    if (maybeName.isEmpty()) {
      return;
    }
    String name = maybeName.get();
    MikrotikInterface iface = getOrCreateInterface(name, "gre");
    iface.setType("gre");
    for (MikrotikParser.Interface_gre_add_propContext prop : ctx.interface_gre_add_prop()) {
      if (prop.if_tunnel_prop_local_address() != null) {
        String rawLocalAddress =
            extractParameterValue(prop.if_tunnel_prop_local_address().parameter_value());
        try {
          iface.setLocalAddress(Ip.parse(rawLocalAddress));
        } catch (IllegalArgumentException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format(
                  "Invalid local-address '%s' for tunnel interface %s", rawLocalAddress, name));
        }
      } else if (prop.if_tunnel_prop_remote_address() != null) {
        String rawRemoteAddress =
            extractParameterValue(prop.if_tunnel_prop_remote_address().parameter_value());
        try {
          iface.setRemoteAddress(Ip.parse(rawRemoteAddress));
        } catch (IllegalArgumentException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format(
                  "Invalid remote-address '%s' for tunnel interface %s", rawRemoteAddress, name));
        }
      } else if (prop.if_prop_disabled() != null) {
        iface.setDisabled(parseBoolean(extractParameterValue(prop.if_prop_disabled().parameter_value())));
      } else if (prop.if_prop_mtu() != null) {
        String rawMtu = extractParameterValue(prop.if_prop_mtu().parameter_value());
        try {
          iface.setMtu(Integer.parseInt(rawMtu));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid mtu value '%s' for interface %s", rawMtu, name));
        }
      } else if (prop.if_prop_bandwidth() != null) {
        String rawBw = extractParameterValue(prop.if_prop_bandwidth().parameter_value());
        try {
          iface.setBandwidth(Double.parseDouble(rawBw));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid bandwidth value '%s' for interface %s", rawBw, name));
        }
      }
    }
    _configuration.defineStructure(INTERFACE, name, ctx);
    _configuration.referenceStructure(
        INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterInterface_ipip_add(MikrotikParser.Interface_ipip_addContext ctx) {
    Optional<String> maybeName =
        ctx.interface_ipip_add_prop().stream()
            .filter(prop -> prop.if_prop_name() != null)
            .map(prop -> extractParameterValue(prop.if_prop_name().parameter_value()))
            .findFirst();
    if (maybeName.isEmpty()) {
      return;
    }
    String name = maybeName.get();
    MikrotikInterface iface = getOrCreateInterface(name, "ipip");
    iface.setType("ipip");
    for (MikrotikParser.Interface_ipip_add_propContext prop : ctx.interface_ipip_add_prop()) {
      if (prop.if_tunnel_prop_local_address() != null) {
        String rawLocalAddress =
            extractParameterValue(prop.if_tunnel_prop_local_address().parameter_value());
        try {
          iface.setLocalAddress(Ip.parse(rawLocalAddress));
        } catch (IllegalArgumentException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format(
                  "Invalid local-address '%s' for tunnel interface %s", rawLocalAddress, name));
        }
      } else if (prop.if_tunnel_prop_remote_address() != null) {
        String rawRemoteAddress =
            extractParameterValue(prop.if_tunnel_prop_remote_address().parameter_value());
        try {
          iface.setRemoteAddress(Ip.parse(rawRemoteAddress));
        } catch (IllegalArgumentException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format(
                  "Invalid remote-address '%s' for tunnel interface %s", rawRemoteAddress, name));
        }
      } else if (prop.if_prop_disabled() != null) {
        iface.setDisabled(parseBoolean(extractParameterValue(prop.if_prop_disabled().parameter_value())));
      } else if (prop.if_prop_mtu() != null) {
        String rawMtu = extractParameterValue(prop.if_prop_mtu().parameter_value());
        try {
          iface.setMtu(Integer.parseInt(rawMtu));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid mtu value '%s' for interface %s", rawMtu, name));
        }
      } else if (prop.if_prop_bandwidth() != null) {
        String rawBw = extractParameterValue(prop.if_prop_bandwidth().parameter_value());
        try {
          iface.setBandwidth(Double.parseDouble(rawBw));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid bandwidth value '%s' for interface %s", rawBw, name));
        }
      }
    }
    _configuration.defineStructure(INTERFACE, name, ctx);
    _configuration.referenceStructure(
        INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterInterface_eoip_add(MikrotikParser.Interface_eoip_addContext ctx) {
    Optional<String> maybeName =
        ctx.interface_eoip_add_prop().stream()
            .filter(prop -> prop.if_prop_name() != null)
            .map(prop -> extractParameterValue(prop.if_prop_name().parameter_value()))
            .findFirst();
    if (maybeName.isEmpty()) {
      return;
    }
    String name = maybeName.get();
    MikrotikInterface iface = getOrCreateInterface(name, "eoip");
    iface.setType("eoip");
    for (MikrotikParser.Interface_eoip_add_propContext prop : ctx.interface_eoip_add_prop()) {
      if (prop.if_tunnel_prop_local_address() != null) {
        String rawLocalAddress =
            extractParameterValue(prop.if_tunnel_prop_local_address().parameter_value());
        try {
          iface.setLocalAddress(Ip.parse(rawLocalAddress));
        } catch (IllegalArgumentException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format(
                  "Invalid local-address '%s' for tunnel interface %s", rawLocalAddress, name));
        }
      } else if (prop.if_tunnel_prop_remote_address() != null) {
        String rawRemoteAddress =
            extractParameterValue(prop.if_tunnel_prop_remote_address().parameter_value());
        try {
          iface.setRemoteAddress(Ip.parse(rawRemoteAddress));
        } catch (IllegalArgumentException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format(
                  "Invalid remote-address '%s' for tunnel interface %s", rawRemoteAddress, name));
        }
      } else if (prop.if_eoip_prop_tunnel_id() != null) {
        String rawTunnelId = extractParameterValue(prop.if_eoip_prop_tunnel_id().parameter_value());
        try {
          iface.setTunnelId(Integer.parseInt(rawTunnelId));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid tunnel-id '%s' for tunnel interface %s", rawTunnelId, name));
        }
      } else if (prop.if_prop_disabled() != null) {
        iface.setDisabled(parseBoolean(extractParameterValue(prop.if_prop_disabled().parameter_value())));
      } else if (prop.if_prop_mtu() != null) {
        String rawMtu = extractParameterValue(prop.if_prop_mtu().parameter_value());
        try {
          iface.setMtu(Integer.parseInt(rawMtu));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid mtu value '%s' for interface %s", rawMtu, name));
        }
      } else if (prop.if_prop_bandwidth() != null) {
        String rawBw = extractParameterValue(prop.if_prop_bandwidth().parameter_value());
        try {
          iface.setBandwidth(Double.parseDouble(rawBw));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid bandwidth value '%s' for interface %s", rawBw, name));
        }
      }
    }
    _configuration.defineStructure(INTERFACE, name, ctx);
    _configuration.referenceStructure(
        INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterInterface_wireguard_add(MikrotikParser.Interface_wireguard_addContext ctx) {
    Optional<String> maybeName =
        ctx.interface_wireguard_add_prop().stream()
            .filter(prop -> prop.if_prop_name() != null)
            .map(prop -> extractParameterValue(prop.if_prop_name().parameter_value()))
            .findFirst();
    if (maybeName.isEmpty()) {
      return;
    }
    String name = maybeName.get();
    MikrotikInterface iface = getOrCreateInterface(name, "wireguard");
    iface.setType("wireguard");
    for (MikrotikParser.Interface_wireguard_add_propContext prop : ctx.interface_wireguard_add_prop()) {
      if (prop.if_wireguard_prop_listen_port() != null) {
        String rawListenPort =
            extractParameterValue(prop.if_wireguard_prop_listen_port().parameter_value());
        try {
          iface.setListenPort(Integer.parseInt(rawListenPort));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid listen-port '%s' for tunnel interface %s", rawListenPort, name));
        }
      } else if (prop.if_prop_disabled() != null) {
        iface.setDisabled(parseBoolean(extractParameterValue(prop.if_prop_disabled().parameter_value())));
      } else if (prop.if_prop_mtu() != null) {
        String rawMtu = extractParameterValue(prop.if_prop_mtu().parameter_value());
        try {
          iface.setMtu(Integer.parseInt(rawMtu));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid mtu value '%s' for interface %s", rawMtu, name));
        }
      } else if (prop.if_prop_bandwidth() != null) {
        String rawBw = extractParameterValue(prop.if_prop_bandwidth().parameter_value());
        try {
          iface.setBandwidth(Double.parseDouble(rawBw));
        } catch (NumberFormatException e) {
          _w.addWarning(
              ctx,
              getFullText(ctx),
              _parser,
              String.format("Invalid bandwidth value '%s' for interface %s", rawBw, name));
        }
      }
    }
    _configuration.defineStructure(INTERFACE, name, ctx);
    _configuration.referenceStructure(
        INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterInterface_bridge_port_add(MikrotikParser.Interface_bridge_port_addContext ctx) {
    Optional<String> maybeBridge =
        ctx.interface_bridge_port_add_prop().stream()
            .filter(prop -> prop.if_prop_bridge() != null)
            .map(prop -> extractParameterValue(prop.if_prop_bridge().parameter_value()))
            .findFirst();
    Optional<String> maybeIface =
        ctx.interface_bridge_port_add_prop().stream()
            .filter(prop -> prop.if_prop_interface() != null)
            .map(prop -> extractParameterValue(prop.if_prop_interface().parameter_value()))
            .findFirst();
    if (maybeBridge.isEmpty() || maybeIface.isEmpty()) {
      return;
    }

    MikrotikBridgePort bridgePort = new MikrotikBridgePort(maybeBridge.get(), maybeIface.get());
    for (MikrotikParser.Interface_bridge_port_add_propContext prop :
        ctx.interface_bridge_port_add_prop()) {
      if (prop.if_bridge_port_prop_pvid() == null) {
        continue;
      }
      String rawPvid = extractParameterValue(prop.if_bridge_port_prop_pvid().parameter_value());
      try {
        bridgePort.setPvid(Integer.parseInt(rawPvid));
      } catch (NumberFormatException e) {
        _w.addWarning(
            ctx,
            getFullText(ctx),
            _parser,
            String.format("Invalid pvid value '%s' for interface %s", rawPvid, maybeIface.get()));
      }
    }

    _configuration.getBridgePorts().add(bridgePort);
    _configuration.referenceStructure(
        INTERFACE, maybeBridge.get(), BRIDGE_PORT_BRIDGE, ctx.getStart().getLine());
    _configuration.referenceStructure(
        INTERFACE, maybeIface.get(), BRIDGE_PORT_INTERFACE, ctx.getStart().getLine());
  }

  @Override
  public void enterInterface_bridge_vlan_add(MikrotikParser.Interface_bridge_vlan_addContext ctx) {
    Optional<String> maybeBridge =
        ctx.interface_bridge_vlan_add_prop().stream()
            .filter(prop -> prop.if_prop_bridge() != null)
            .map(prop -> extractParameterValue(prop.if_prop_bridge().parameter_value()))
            .findFirst();
    if (maybeBridge.isEmpty()) {
      return;
    }

    MikrotikBridgeVlan bridgeVlan = new MikrotikBridgeVlan(maybeBridge.get());
    for (MikrotikParser.Interface_bridge_vlan_add_propContext prop :
        ctx.interface_bridge_vlan_add_prop()) {
      if (prop.if_bridge_vlan_prop_vlan_ids() != null) {
        String raw = extractParameterValue(prop.if_bridge_vlan_prop_vlan_ids().parameter_value());
        for (String part : raw.split(",")) {
          String vlanId = part.trim();
          if (vlanId.isEmpty()) {
            continue;
          }
          try {
            bridgeVlan.addVlanId(Integer.parseInt(vlanId));
          } catch (NumberFormatException e) {
            _w.addWarning(
                ctx,
                getFullText(ctx),
                _parser,
                String.format("Invalid vlan-id '%s' in bridge vlan entry", vlanId));
          }
        }
      } else if (prop.if_bridge_vlan_prop_tagged() != null) {
        String raw = extractParameterValue(prop.if_bridge_vlan_prop_tagged().parameter_value());
        for (String part : raw.split(",")) {
          String port = part.trim();
          if (!port.isEmpty()) {
            bridgeVlan.addTagged(port);
          }
        }
      } else if (prop.if_bridge_vlan_prop_untagged() != null) {
        String raw = extractParameterValue(prop.if_bridge_vlan_prop_untagged().parameter_value());
        for (String part : raw.split(",")) {
          String port = part.trim();
          if (!port.isEmpty()) {
            bridgeVlan.addUntagged(port);
          }
        }
      }
    }

    _configuration.getBridgeVlans().add(bridgeVlan);
    _configuration.referenceStructure(
        INTERFACE, maybeBridge.get(), BRIDGE_VLAN_BRIDGE, ctx.getStart().getLine());
    bridgeVlan
        .getTagged()
        .forEach(
            port ->
                _configuration.referenceStructure(
                    INTERFACE, port, BRIDGE_VLAN_TAGGED_INTERFACE, ctx.getStart().getLine()));
    bridgeVlan
        .getUntagged()
        .forEach(
            port ->
                _configuration.referenceStructure(
                    INTERFACE, port, BRIDGE_VLAN_UNTAGGED_INTERFACE, ctx.getStart().getLine()));
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
    MikrotikInterface iface = _configuration.getMainVrf().getInterfaces().get(ifaceName);
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

  @Override
  public void enterIp_route_add(MikrotikParser.Ip_route_addContext ctx) {
    String gatewayStr = null;
    String dstStr = null;
    int adminDistance = 1;

    for (MikrotikParser.Ip_route_add_propContext prop : ctx.ip_route_add_prop()) {
      if (prop.ip_route_prop_gateway() != null) {
        gatewayStr = extractParameterValue(prop.ip_route_prop_gateway().parameter_value());
      } else if (prop.ip_route_prop_dst_address() != null) {
        dstStr = extractParameterValue(prop.ip_route_prop_dst_address().parameter_value());
      } else if (prop.ip_route_prop_distance() != null) {
        try {
          adminDistance =
              Integer.parseInt(extractParameterValue(prop.ip_route_prop_distance().parameter_value()));
        } catch (NumberFormatException e) {
          // Use default.
        }
      }
    }

    if (gatewayStr == null) {
      _w.addWarning(ctx, getFullText(ctx), _parser, "Static route missing required gateway");
      return;
    }

    Ip nextHopIp;
    try {
      nextHopIp = Ip.parse(gatewayStr);
    } catch (IllegalArgumentException e) {
      _w.addWarning(ctx, getFullText(ctx), _parser, String.format("Invalid gateway IP: %s", gatewayStr));
      return;
    }

    Prefix network = Prefix.ZERO;
    if (dstStr != null) {
      try {
        network = Prefix.parse(dstStr);
      } catch (IllegalArgumentException e) {
        _w.addWarning(
            ctx, getFullText(ctx), _parser, String.format("Invalid dst-address prefix: %s", dstStr));
        return;
      }
    }

    _configuration
        .getMainVrf()
        .getStaticRoutes()
        .add(new MikrotikStaticRoute(network, nextHopIp, adminDistance));
    _configuration.referenceStructure(
        STATIC_ROUTE, network.toString(), STATIC_ROUTE_SELF_REFERENCE, ctx.getStart().getLine());
  }

  @Override
  public void enterSystem_command(MikrotikParser.System_commandContext ctx) {
    if (ctx.system_subpath() == null
        || ctx.system_subpath().IDENTITY() == null
        || ctx.command_tail() == null
        || ctx.command_tail().line_command() == null) {
      return;
    }
    MikrotikParser.Line_commandContext lineCommand = ctx.command_tail().line_command();
    if (lineCommand.command_verb() == null
        || !lineCommand.command_verb().getText().equalsIgnoreCase("set")) {
      return;
    }
    lineCommand.command_argument().stream()
        .map(MikrotikParser.Command_argumentContext::key_value_parameter)
        .filter(param -> param != null && param.word() != null && param.parameter_value() != null)
        .filter(param -> param.word().getText().equalsIgnoreCase("name"))
        .map(param -> extractParameterValue(param.parameter_value()))
        .findFirst()
        .ifPresent(_configuration::setHostname);
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
    return _configuration
        .getMainVrf()
        .getInterfaces()
        .computeIfAbsent(name, key -> new MikrotikInterface(key, type));
  }

  private static final class PendingInterfaceReference {
    private final @Nonnull ParserRuleContext _ctx;
    private final @Nonnull String _referencedInterface;
    private final @Nonnull String _warningMessage;

    private PendingInterfaceReference(
        ParserRuleContext ctx, String referencedInterface, String warningMessage) {
      _ctx = ctx;
      _referencedInterface = referencedInterface;
      _warningMessage = warningMessage;
    }
  }

  private final @Nonnull MikrotikConfiguration _configuration;
  private final @Nonnull List<PendingInterfaceReference> _pendingInterfaceReferences;
  private final @Nonnull MikrotikCombinedParser _parser;
  private final @Nonnull SilentSyntaxCollection _silentSyntax;
  private final @Nonnull String _text;
  private final @Nonnull Warnings _w;
}
