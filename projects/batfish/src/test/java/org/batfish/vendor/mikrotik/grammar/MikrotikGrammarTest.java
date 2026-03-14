package org.batfish.vendor.mikrotik.grammar;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureType.FIREWALL_ADDRESS_LIST;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureType.INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BONDING_SLAVE_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_PORT_BRIDGE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_PORT_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_VLAN_BRIDGE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_VLAN_TAGGED_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_VLAN_UNTAGGED_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.FIREWALL_FILTER_DST_ADDRESS_LIST;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.FIREWALL_FILTER_SRC_ADDRESS_LIST;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.FIREWALL_NAT_DST_ADDRESS_LIST;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.FIREWALL_NAT_SRC_ADDRESS_LIST;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.VLAN_INTERFACE_PARENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.batfish.common.BatfishLogger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.vendor.mikrotik.representation.MikrotikBridgePort;
import org.batfish.vendor.mikrotik.representation.MikrotikBridgeVlan;
import org.batfish.vendor.mikrotik.representation.MikrotikConfiguration;
import org.batfish.vendor.mikrotik.representation.MikrotikInterface;
import org.batfish.vendor.mikrotik.representation.MikrotikStaticRoute;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MikrotikGrammarTest {

  private static final String TESTCONFIGS_PREFIX =
      "org/batfish/vendor/mikrotik/grammar/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Test
  public void testLexerTokenizesRouterOsSyntax() {
    String input =
        "/ip address add address=10.0.0.2/30 comment=\"L3 to Cisco core\" "
            + "servers=1.1.1.1,8.8.8.8\n";
    MikrotikLexer lexer = new MikrotikLexer(CharStreams.fromString(input));
    List<Integer> tokenTypes = lexer.getAllTokens().stream().map(Token::getType).toList();

    assertThat(tokenTypes, hasItem(MikrotikLexer.SLASH));
    assertThat(tokenTypes, hasItem(MikrotikLexer.EQUALS));
    assertThat(tokenTypes, hasItem(MikrotikLexer.IP_PREFIX));
    assertThat(tokenTypes, hasItem(MikrotikLexer.IP_ADDRESS));
    assertThat(tokenTypes, hasItem(MikrotikLexer.QUOTED_STRING));
  }

  @Test
  public void testLexerTokenizesTunnelKeywords() {
    String input =
        "/interface gre add name=gre0 local-address=1.2.3.4 remote-address=5.6.7.8 mtu=1476\n"
            + "/interface ipip add name=ipip0 local-address=1.2.3.4 remote-address=5.6.7.9 mtu=1480\n"
            + "/interface eoip add name=eoip0 local-address=1.2.3.4 remote-address=5.6.7.10"
            + " tunnel-id=99 mtu=1458\n"
            + "/interface wireguard add name=wg0 listen-port=51820 mtu=1420\n"
            + "/interface wireguard peers add interface=wg0 allowed-address=10.0.0.2/32"
            + " endpoint-address=198.51.100.2 endpoint-port=51820\n";
    MikrotikLexer lexer = new MikrotikLexer(CharStreams.fromString(input));
    List<Integer> tokenTypes = lexer.getAllTokens().stream().map(Token::getType).toList();

    assertThat(tokenTypes, hasItem(MikrotikLexer.GRE));
    assertThat(tokenTypes, hasItem(MikrotikLexer.IPIP));
    assertThat(tokenTypes, hasItem(MikrotikLexer.EOIP));
    assertThat(tokenTypes, hasItem(MikrotikLexer.WIREGUARD));
    assertThat(tokenTypes, hasItem(MikrotikLexer.PEERS));
    assertThat(tokenTypes, hasItem(MikrotikLexer.LOCAL_ADDRESS));
    assertThat(tokenTypes, hasItem(MikrotikLexer.REMOTE_ADDRESS));
    assertThat(tokenTypes, hasItem(MikrotikLexer.TUNNEL_ID));
    assertThat(tokenTypes, hasItem(MikrotikLexer.LISTEN_PORT));
  }

  @Test
  public void testLexerTokenizesBandwidthKeyword() {
    String input = "/interface ethernet set [ find default-name=ether1 ] bandwidth=1000000\n";
    MikrotikLexer lexer = new MikrotikLexer(CharStreams.fromString(input));
    List<Integer> tokenTypes = lexer.getAllTokens().stream().map(Token::getType).toList();

    assertThat(tokenTypes, hasItem(MikrotikLexer.BANDWIDTH));
  }

  @Test
  public void testLexerTokenizesNetwatchKeywords() {
    String input =
        "/tool netwatch add host=192.0.2.1 interval=00:00:10 timeout=3s "
            + "up-script=\":log info \\\"up\\\"\" down-script=\":log warning \\\"down\\\"\"\n";
    MikrotikLexer lexer = new MikrotikLexer(CharStreams.fromString(input));
    List<Integer> tokenTypes = lexer.getAllTokens().stream().map(Token::getType).toList();

    assertThat(tokenTypes, hasItem(MikrotikLexer.NETWATCH));
    assertThat(tokenTypes, hasItem(MikrotikLexer.HOST));
    assertThat(tokenTypes, hasItem(MikrotikLexer.INTERVAL));
    assertThat(tokenTypes, hasItem(MikrotikLexer.TIMEOUT));
    assertThat(tokenTypes, hasItem(MikrotikLexer.UP_SCRIPT));
    assertThat(tokenTypes, hasItem(MikrotikLexer.DOWN_SCRIPT));
  }

  @Test
  public void testLexerTokenizesVrrpKeywords() {
    String input = "/interface vrrp add vrid=10 virtual-address=10.10.10.254/24\n";
    MikrotikLexer lexer = new MikrotikLexer(CharStreams.fromString(input));
    List<Integer> tokenTypes = lexer.getAllTokens().stream().map(Token::getType).toList();

    assertThat(tokenTypes, hasItem(MikrotikLexer.VRRP));
    assertThat(tokenTypes, hasItem(MikrotikLexer.VRID));
    assertThat(tokenTypes, hasItem(MikrotikLexer.VIRTUAL_ADDRESS));
  }

  @Test
  public void testParserParsesReferenceFixture() {
    String src = readResource(TESTCONFIGS_PREFIX + "mikrotik_interfaces_and_routes.export", UTF_8);
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(parser, notNullValue());
    assertThat(tree, notNullValue());
  }

  @Test
  public void testParserParsesInlineBondingCommand() {
    String src =
        "/interface bonding add lacp-rate=1sec mode=802.3ad name=bond-core slaves=ether1,ether2\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(tree, notNullValue());
    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_bonding_add"));
    assertThat(tree.toStringTree(parser.getParser()), containsString("if_bonding_prop_mode"));
    assertThat(tree.toStringTree(parser.getParser()), containsString("if_bonding_prop_lacp_rate"));
    assertThat(tree.toStringTree(parser.getParser()), containsString("if_bonding_prop_slaves"));
  }

  @Test
  public void testParserParsesInlineTunnelCommands() {
    String src =
        "/interface gre add name=gre0 local-address=1.2.3.4 remote-address=5.6.7.8 mtu=1476\n"
            + "/interface ipip add name=ipip0 local-address=1.2.3.4 remote-address=5.6.7.9"
            + " mtu=1480\n"
            + "/interface eoip add name=eoip0 local-address=1.2.3.4 remote-address=5.6.7.10"
            + " tunnel-id=99 mtu=1458\n"
            + "/interface wireguard add name=wg0 listen-port=51820 mtu=1420\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(tree, notNullValue());
    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_gre_add"));
    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_ipip_add"));
    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_eoip_add"));
    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_wireguard_add"));
  }

  @Test
  public void testParserParsesInlineBandwidthCommand() {
    String src =
        "/interface ethernet set [ find default-name=ether1 ] bandwidth=1000000\n"
            + "/interface bridge add name=br0 bandwidth=2000000\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(tree, notNullValue());
    assertThat(tree.toStringTree(parser.getParser()), containsString("if_prop_bandwidth"));
  }

  @Test
  public void testParserParsesInlineNetwatchCommandWithoutGenericCommand() {
    String src = "/tool netwatch add host=192.0.2.1 comment=\"track-core-gw\"\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    String treeText = tree.toStringTree(parser.getParser());
    assertThat(treeText, containsString("tool_netwatch_add"));
    assertThat(treeText.contains("generic_command"), equalTo(false));
  }

  @Test
  public void testParserParsesWireguardPeersWithoutGenericCommand() {
    String src =
        "/interface wireguard peers add interface=wg0 allowed-address=10.0.0.2/32"
            + " endpoint-address=198.51.100.2 endpoint-port=51820\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    String treeText = tree.toStringTree(parser.getParser());
    assertThat(treeText, containsString("wireguard"));
    assertThat(treeText.contains("generic_command"), equalTo(false));
  }

  @Test
  public void testParserParsesInlineVrrpCommandWithoutGenericCommand() {
    String src = "/interface vrrp add interface=ether1 name=vrrp1 vrid=10 virtual-address=1.1.1.1/24\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    String treeText = tree.toStringTree(parser.getParser());
    assertThat(treeText, containsString("interface_vrrp_add"));
    assertThat(treeText.contains("generic_command"), equalTo(false));
  }

  @Test
  public void testParserParsesBondingVlanFixture() {
    String src = readResource(TESTCONFIGS_PREFIX + "mikrotik_bonding_vlan_vi", UTF_8);
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(tree, notNullValue());
    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_bonding_add"));
    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_vlan_add"));
  }

  @Test
  public void testMikrotikInterfaceBasic() {
    String src = readResource(TESTCONFIGS_PREFIX + "mikrotik_interface_basic", UTF_8);
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(tree, notNullValue());
    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_bridge_add"));
    assertThat(tree.toStringTree(parser.getParser()), containsString("ip_address_add"));
  }

  @Test
  public void testMikrotikStaticRouteBasic() {
    String src = readResource(TESTCONFIGS_PREFIX + "mikrotik_static_route_basic", UTF_8);
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(tree, notNullValue());
    assertThat(tree.toStringTree(parser.getParser()), containsString("ip_route_add"));
  }

  @Test
  public void testMikrotikInterfaceExtraction() {
    ExtractionResult result = parseAndExtract("mikrotik_interface_basic");

    MikrotikConfiguration vc = result._configuration;
    assertThat(
        vc.getMainVrf().getInterfaces().keySet(), containsInAnyOrder("bridge-lan", "customer-a", "spare"));

    MikrotikInterface bridgeLan = vc.getMainVrf().getInterfaces().get("bridge-lan");
    assertThat(bridgeLan, notNullValue());
    assertThat(bridgeLan.getType(), equalTo("bridge"));
    assertThat(bridgeLan.isDisabled(), equalTo(false));

    MikrotikInterface customerA = vc.getMainVrf().getInterfaces().get("customer-a");
    assertThat(customerA, notNullValue());
    assertThat(customerA.getType(), equalTo("ethernet"));

    MikrotikInterface spare = vc.getMainVrf().getInterfaces().get("spare");
    assertThat(spare, notNullValue());
    assertThat(spare.getType(), equalTo("ethernet"));
    assertThat(spare.isDisabled(), equalTo(true));

    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("references undefined interface uplink-core"));
  }

  @Test
  public void testMikrotikIpAddressExtraction() {
    String src =
        "# mar/12/2026 16:20:36 by RouterOS 6.49.17\n"
            + "/interface ethernet set [ find default-name=ether1 ] name=uplink-core\n"
            + "/ip address add address=10.0.0.2/30 interface=uplink-core network=10.0.0.0\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface interfaceWithAddress =
        result._configuration.getMainVrf().getInterfaces().get("uplink-core");
    assertThat(interfaceWithAddress, notNullValue());
    assertThat(interfaceWithAddress.getAddresses(), hasSize(1));
    assertThat(
        interfaceWithAddress.getAddresses(),
        contains(ConcreteInterfaceAddress.parse("10.0.0.2/30")));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testMikrotikVlanInterfaceExtraction() {
    String src =
        "/interface bridge add name=bridge-lan\n"
            + "/interface ethernet set [ find default-name=ether6 ] disable-running-check=no\n"
            + "/interface vlan add interface=bridge-lan name=vlan25 vlan-id=25\n"
            + "/interface vlan add interface=ether6 name=vlan30 vlan-id=30 disabled=yes\n"
            + "/ip address add address=192.168.25.1/24 interface=vlan25 network=192.168.25.0\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_vlan_add"));

    ExtractionResult result = parseAndExtractFromString(src);
    MikrotikInterface ether6 = result._configuration.getMainVrf().getInterfaces().get("ether6");
    MikrotikInterface vlan25 = result._configuration.getMainVrf().getInterfaces().get("vlan25");
    MikrotikInterface vlan30 = result._configuration.getMainVrf().getInterfaces().get("vlan30");
    assertThat(ether6, notNullValue());
    assertThat(ether6.getType(), equalTo("ethernet"));
    assertThat(vlan25, notNullValue());
    assertThat(vlan25.getType(), equalTo("vlan"));
    assertThat(vlan25.getVlanId(), equalTo(25));
    assertThat(vlan25.getParentInterface(), equalTo("bridge-lan"));
    assertThat(
        vlan25.getAddresses(), contains(ConcreteInterfaceAddress.parse("192.168.25.1/24")));
    assertThat(vlan30, notNullValue());
    assertThat(vlan30.getType(), equalTo("vlan"));
    assertThat(vlan30.getVlanId(), equalTo(30));
    assertThat(vlan30.getParentInterface(), equalTo("ether6"));
    assertThat(vlan30.isDisabled(), equalTo(true));
    assertThat(vlan30.getAddresses(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(INTERFACE);
    assertThat(references, hasKey("bridge-lan"));
    assertThat(references.get("bridge-lan"), hasKey(VLAN_INTERFACE_PARENT));
    assertThat(references.get("bridge-lan").get(VLAN_INTERFACE_PARENT), hasItem(3));
  }

  @Test
  public void testExtractorBondingFieldsInline() {
    String src =
        "/interface ethernet set [ find default-name=ether1 ] disable-running-check=no\n"
            + "/interface ethernet set [ find default-name=ether2 ] disable-running-check=no\n"
            + "/interface bonding add lacp-rate=1sec mode=802.3ad name=bond-core slaves=ether1,ether2\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface bondCore = result._configuration.getMainVrf().getInterfaces().get("bond-core");
    assertThat(bondCore, notNullValue());
    assertThat(bondCore.getType(), equalTo("bonding"));
    assertThat(bondCore.getSlaves(), contains("ether1", "ether2"));
    assertThat(bondCore.getBondingMode(), equalTo("802.3ad"));
    assertThat(bondCore.getLacpRate(), equalTo("1sec"));

    assertThat(result._warnings.getParseWarnings(), hasSize(0));

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(INTERFACE);
    assertThat(references, hasKey("ether1"));
    assertThat(references.get("ether1"), hasKey(BONDING_SLAVE_INTERFACE));
    assertThat(references, hasKey("ether2"));
    assertThat(references.get("ether2"), hasKey(BONDING_SLAVE_INTERFACE));
  }

  @Test
  public void testExtractorVlanParentOnBondInline() {
    String src =
        "/interface ethernet set [ find default-name=ether1 ] disable-running-check=no\n"
            + "/interface ethernet set [ find default-name=ether2 ] disable-running-check=no\n"
            + "/interface bonding add lacp-rate=1sec mode=802.3ad name=bond-core slaves=ether1,ether2\n"
            + "/interface vlan add interface=bond-core name=vlan200-bond-wan vlan-id=200\n"
            + "/ip address add address=172.16.200.2/30 interface=vlan200-bond-wan network=172.16.200.0\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface vlan200 =
        result._configuration.getMainVrf().getInterfaces().get("vlan200-bond-wan");
    assertThat(vlan200, notNullValue());
    assertThat(vlan200.getType(), equalTo("vlan"));
    assertThat(vlan200.getVlanId(), equalTo(200));
    assertThat(vlan200.getParentInterface(), equalTo("bond-core"));
    assertThat(vlan200.getAddresses(), contains(ConcreteInterfaceAddress.parse("172.16.200.2/30")));

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(INTERFACE);
    assertThat(references, hasKey("bond-core"));
    assertThat(references.get("bond-core"), hasKey(VLAN_INTERFACE_PARENT));
    assertThat(references.get("bond-core").get(VLAN_INTERFACE_PARENT), hasItem(4));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorTunnelGreInline() {
    String src =
        "/interface gre add name=gre0 local-address=1.2.3.4 remote-address=5.6.7.8 mtu=1476\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface gre0 = result._configuration.getMainVrf().getInterfaces().get("gre0");
    assertThat(gre0, notNullValue());
    assertThat(gre0.getType(), equalTo("gre"));
    assertThat(gre0.getLocalAddress(), equalTo(Ip.parse("1.2.3.4")));
    assertThat(gre0.getRemoteAddress(), equalTo(Ip.parse("5.6.7.8")));
    assertThat(gre0.getMtu(), equalTo(1476));
  }

  @Test
  public void testExtractorTunnelIpipInline() {
    String src =
        "/interface ipip add name=ipip0 local-address=1.2.3.4 remote-address=5.6.7.9 mtu=1480\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface ipip0 = result._configuration.getMainVrf().getInterfaces().get("ipip0");
    assertThat(ipip0, notNullValue());
    assertThat(ipip0.getType(), equalTo("ipip"));
    assertThat(ipip0.getLocalAddress(), equalTo(Ip.parse("1.2.3.4")));
    assertThat(ipip0.getRemoteAddress(), equalTo(Ip.parse("5.6.7.9")));
    assertThat(ipip0.getMtu(), equalTo(1480));
  }

  @Test
  public void testExtractorTunnelEoipInline() {
    String src =
        "/interface eoip add name=eoip0 local-address=1.2.3.4 remote-address=5.6.7.10"
            + " tunnel-id=99 mtu=1458\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface eoip0 = result._configuration.getMainVrf().getInterfaces().get("eoip0");
    assertThat(eoip0, notNullValue());
    assertThat(eoip0.getType(), equalTo("eoip"));
    assertThat(eoip0.getLocalAddress(), equalTo(Ip.parse("1.2.3.4")));
    assertThat(eoip0.getRemoteAddress(), equalTo(Ip.parse("5.6.7.10")));
    assertThat(eoip0.getTunnelId(), equalTo(99));
    assertThat(eoip0.getMtu(), equalTo(1458));
  }

  @Test
  public void testExtractorTunnelWireguardInline() {
    String src = "/interface wireguard add name=wg0 listen-port=51820 mtu=1420\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface wg0 = result._configuration.getMainVrf().getInterfaces().get("wg0");
    assertThat(wg0, notNullValue());
    assertThat(wg0.getType(), equalTo("wireguard"));
    assertThat(wg0.getListenPort(), equalTo(51820));
    assertThat(wg0.getMtu(), equalTo(1420));
    assertThat(wg0.getLocalAddress(), equalTo((Ip) null));
    assertThat(wg0.getRemoteAddress(), equalTo((Ip) null));
  }

  @Test
  public void testExtractorMtuEthernetInline() {
    String src = "/interface ethernet set [ find default-name=ether1 ] mtu=9000\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface ether1 = result._configuration.getMainVrf().getInterfaces().get("ether1");
    assertThat(ether1, notNullValue());
    assertThat(ether1.getMtu(), equalTo(9000));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorMtuBridgeInline() {
    String src = "/interface bridge add name=br0 mtu=1588\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface br0 = result._configuration.getMainVrf().getInterfaces().get("br0");
    assertThat(br0, notNullValue());
    assertThat(br0.getMtu(), equalTo(1588));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorMtuVlanInline() {
    String src = "/interface vlan add name=vlan10 vlan-id=10 interface=ether1 mtu=1496\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface vlan10 = result._configuration.getMainVrf().getInterfaces().get("vlan10");
    assertThat(vlan10, notNullValue());
    assertThat(vlan10.getMtu(), equalTo(1496));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("references undefined parent interface ether1"));
  }

  @Test
  public void testExtractorMalformedMtuWarningInline() {
    String src = "/interface ethernet set [ find default-name=ether1 ] mtu=notanumber\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface ether1 = result._configuration.getMainVrf().getInterfaces().get("ether1");
    assertThat(ether1, notNullValue());
    assertThat(ether1.getMtu(), equalTo((Integer) null));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Invalid mtu value 'notanumber' for interface ether1"));
  }

  @Test
  public void testExtractorBandwidthInline() {
    String src = "/interface ethernet set [ find default-name=ether1 ] bandwidth=1000000\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface ether1 = result._configuration.getMainVrf().getInterfaces().get("ether1");
    assertThat(ether1, notNullValue());
    assertThat(ether1.getBandwidth(), equalTo(1000000.0));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorMalformedBandwidthWarningInline() {
    String src = "/interface ethernet set [ find default-name=ether1 ] bandwidth=notanumber\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface ether1 = result._configuration.getMainVrf().getInterfaces().get("ether1");
    assertThat(ether1, notNullValue());
    assertThat(ether1.getBandwidth(), equalTo((Double) null));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Invalid bandwidth value 'notanumber' for interface ether1"));
  }

  @Test
  public void testExtractorNetwatchCommentBasedIdInline() {
    String src = "/tool netwatch add host=192.0.2.1 comment=\"track-core-gw\"\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getNetwatch(), hasKey("track-core-gw"));
    assertThat(
        result._configuration.getNetwatch().get("track-core-gw").getHost(), equalTo("192.0.2.1"));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorNetwatchHostFallbackIdInline() {
    String src = "/tool netwatch add host=10.0.0.1 interval=00:00:10\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getNetwatch(), hasKey("netwatch:10.0.0.1"));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorNetwatchDisabledInline() {
    String src = "/tool netwatch add host=10.0.0.2 disabled=yes comment=\"track-standby\"\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getNetwatch(), hasKey("track-standby"));
    assertThat(
        result._configuration.getNetwatch().get("track-standby").isDisabled(), equalTo(true));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorNetwatchDuplicateIdWarningInline() {
    String src =
        "/tool netwatch add host=192.0.2.1 comment=\"track-dup\"\n"
            + "/tool netwatch add host=192.0.2.2 comment=\"track-dup\"\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getNetwatch().keySet(), hasSize(1));
    assertThat(result._configuration.getNetwatch(), hasKey("track-dup"));
    assertThat(result._configuration.getNetwatch().get("track-dup").getHost(), equalTo("192.0.2.1"));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Duplicate Netwatch ID"));
  }

  @Test
  public void testExtractorNetwatchMissingHostWarningInline() {
    String src = "/tool netwatch add comment=\"no-host\"\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getNetwatch().keySet(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("missing required host"));
  }

  @Test
  public void testExtractorVrrpFullFieldsInline() {
    String src =
        "/interface vrrp add interface=ether1 name=vrrp1 vrid=10 priority=110 preemption-mode=yes"
            + " version=3 v3-protocol=ipv4 virtual-address=10.10.10.254/24\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getVrrpGroups(), hasKey("vrrp1"));
    assertThat(
        result._configuration.getVrrpGroups().get("vrrp1").getParentInterface(),
        equalTo("ether1"));
    assertThat(result._configuration.getVrrpGroups().get("vrrp1").getVrid(), equalTo(10));
    assertThat(result._configuration.getVrrpGroups().get("vrrp1").getPriority(), equalTo(110));
    assertThat(result._configuration.getVrrpGroups().get("vrrp1").isPreempt(), equalTo(true));
    assertThat(
        result._configuration.getVrrpGroups().get("vrrp1").getVirtualAddress(),
        equalTo(Ip.parse("10.10.10.254")));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorVrrpDisabledInline() {
    String src =
        "/interface vrrp add disabled=yes interface=ether1 name=vrrp-disabled vrid=30"
            + " virtual-address=10.10.10.253/24\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getVrrpGroups(), hasKey("vrrp-disabled"));
    assertThat(result._configuration.getVrrpGroups().get("vrrp-disabled").isDisabled(), equalTo(true));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorVrrpPreemptionModeNoInline() {
    String src =
        "/interface vrrp add interface=ether1 name=vrrp-no-preempt vrid=20 preemption-mode=no"
            + " virtual-address=198.51.100.254/24\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getVrrpGroups(), hasKey("vrrp-no-preempt"));
    assertThat(
        result._configuration.getVrrpGroups().get("vrrp-no-preempt").isPreempt(), equalTo(false));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorVrrpMissingVirtualAddressWarningInline() {
    String src = "/interface vrrp add interface=ether1 name=vrrp-no-vip vrid=10\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getVrrpGroups().keySet(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("missing required virtual-address"));
  }

  @Test
  public void testExtractorVrrpDuplicateNameWarningInline() {
    String src =
        "/interface vrrp add interface=ether1 name=vrrp-dup vrid=10 virtual-address=10.0.0.1/24\n"
            + "/interface vrrp add interface=ether1 name=vrrp-dup vrid=20"
            + " virtual-address=10.0.0.2/24\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getVrrpGroups().keySet(), hasSize(1));
    assertThat(result._configuration.getVrrpGroups(), hasKey("vrrp-dup"));
    assertThat(result._configuration.getVrrpGroups().get("vrrp-dup").getVrid(), equalTo(10));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Duplicate VRRP name"));
  }

  @Test
  public void testExtractorVrrpMissingRequiredFieldWarningInline() {
    String src = "/interface vrrp add name=vrrp-no-iface vrid=5 virtual-address=1.2.3.4\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getVrrpGroups().keySet(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("missing required name=, interface=, or vrid="));
  }

  @Test
  public void testExtractorTunnelInvalidAddressWarningInline() {
    String src =
        "/interface gre add name=gre0 local-address=not-an-ip remote-address=5.6.7.8 mtu=1476\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Invalid local-address 'not-an-ip' for tunnel interface gre0"));
  }

  @Test
  public void testExtractorTunnelInvalidTunnelIdWarningInline() {
    String src =
        "/interface eoip add name=eoip0 local-address=1.2.3.4 remote-address=5.6.7.10"
            + " tunnel-id=notanumber mtu=1458\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Invalid tunnel-id 'notanumber' for tunnel interface eoip0"));
  }

  @Test
  public void testExtractorTunnelUnsupportedParamsFallThroughInline() {
    String src =
        "/interface gre add name=gre-unsupported local-address=192.0.2.2 remote-address=192.0.2.10"
            + " dscp=inherit clamp-tcp-mss=yes ipsec-secret=\"x\"\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface iface =
        result._configuration.getMainVrf().getInterfaces().get("gre-unsupported");
    assertThat(iface, notNullValue());
    assertThat(iface.getType(), equalTo("gre"));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
    assertThat(result._warnings.getRedFlagWarnings(), hasSize(0));
  }

  @Test
  public void testWarningUndefinedBondingSlaveInline() {
    String src = "/interface bonding add name=bond-bad slaves=ghost-slave mode=802.3ad\n";
    ExtractionResult result = parseAndExtractFromString(src);

    List<String> warningComments =
        result._warnings.getParseWarnings().stream().map(w -> w.getComment()).toList();
    assertThat(
        warningComments,
        hasItem(
            containsString(
                "Bonding interface bond-bad references undefined slave interface ghost-slave")));

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(INTERFACE);
    assertThat(references, hasKey("ghost-slave"));
    assertThat(references.get("ghost-slave"), hasKey(BONDING_SLAVE_INTERFACE));
  }

  @Test
  public void testWarningUndefinedVlanParentInline() {
    String src = "/interface vlan add interface=ghost-parent name=vlan999-missing vlan-id=999\n";
    ExtractionResult result = parseAndExtractFromString(src);

    List<String> warningComments =
        result._warnings.getParseWarnings().stream().map(w -> w.getComment()).toList();
    assertThat(
        warningComments,
        hasItem(
            containsString(
                "VLAN interface vlan999-missing references undefined parent interface ghost-parent")));

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(INTERFACE);
    assertThat(references, hasKey("ghost-parent"));
    assertThat(references.get("ghost-parent"), hasKey(VLAN_INTERFACE_PARENT));
  }

  @Test
  public void testExtractorBondingVlanFixtureAssertsFullInterfaceSet() {
    ExtractionResult result = parseAndExtract("mikrotik_bonding_vlan_vi");
    Map<String, MikrotikInterface> interfaces = result._configuration.getMainVrf().getInterfaces();
    assertThat(
        interfaces.keySet(),
        containsInAnyOrder(
            "bridge-lan",
            "ether1",
            "ether2",
            "ether3",
            "ether4",
            "ether5",
            "ether6",
            "ether7",
            "ether8",
            "bond-core",
            "vlan10-users",
            "vlan20-servers",
            "vlan30-voice",
            "vlan40-guest",
            "vlan200-bond-wan"));

    MikrotikInterface bondCore = interfaces.get("bond-core");
    assertThat(bondCore, notNullValue());
    assertThat(bondCore.getType(), equalTo("bonding"));
    assertThat(bondCore.getSlaves(), contains("ether1", "ether2"));

    MikrotikInterface vlan200 = interfaces.get("vlan200-bond-wan");
    assertThat(vlan200, notNullValue());
    assertThat(vlan200.getType(), equalTo("vlan"));
    assertThat(vlan200.getVlanId(), equalTo(200));
    assertThat(vlan200.getParentInterface(), equalTo("bond-core"));
    assertThat(vlan200.getAddresses(), contains(ConcreteInterfaceAddress.parse("172.16.200.2/30")));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorTunnelFixtureAssertsFullInterfaceSet() {
    ExtractionResult result = parseAndExtract("mikrotik_tunnel_interfaces");
    Map<String, MikrotikInterface> interfaces = result._configuration.getMainVrf().getInterfaces();
    assertThat(
        interfaces.keySet(),
        containsInAnyOrder(
            "ether1",
            "ether2",
            "ether3",
            "ether4",
            "ether5",
            "ether6",
            "ether7",
            "ether8",
            "gre-core",
            "ipip-backhaul",
            "eoip-l2",
            "wg-overlay",
            "gre-unsupported",
            "ipip-unsupported",
            "eoip-unsupported",
            "wg-unsupported"));

    MikrotikInterface greCore = interfaces.get("gre-core");
    assertThat(greCore, notNullValue());
    assertThat(greCore.getType(), equalTo("gre"));
    assertThat(greCore.getLocalAddress(), equalTo(Ip.parse("192.0.2.2")));
    assertThat(greCore.getRemoteAddress(), equalTo(Ip.parse("192.0.2.1")));
    assertThat(greCore.getAddresses(), contains(ConcreteInterfaceAddress.parse("10.0.0.2/30")));

    MikrotikInterface ipipBackhaul = interfaces.get("ipip-backhaul");
    assertThat(ipipBackhaul, notNullValue());
    assertThat(ipipBackhaul.getType(), equalTo("ipip"));
    assertThat(ipipBackhaul.getLocalAddress(), equalTo(Ip.parse("192.0.2.2")));
    assertThat(ipipBackhaul.getRemoteAddress(), equalTo(Ip.parse("192.0.2.5")));
    assertThat(
        ipipBackhaul.getAddresses(), contains(ConcreteInterfaceAddress.parse("10.0.1.2/30")));

    MikrotikInterface eoipL2 = interfaces.get("eoip-l2");
    assertThat(eoipL2, notNullValue());
    assertThat(eoipL2.getType(), equalTo("eoip"));
    assertThat(eoipL2.getLocalAddress(), equalTo(Ip.parse("192.0.2.2")));
    assertThat(eoipL2.getRemoteAddress(), equalTo(Ip.parse("192.0.2.9")));
    assertThat(eoipL2.getTunnelId(), equalTo(200));

    MikrotikInterface wgOverlay = interfaces.get("wg-overlay");
    assertThat(wgOverlay, notNullValue());
    assertThat(wgOverlay.getType(), equalTo("wireguard"));
    assertThat(wgOverlay.getListenPort(), equalTo(51820));
    assertThat(
        wgOverlay.getAddresses(), contains(ConcreteInterfaceAddress.parse("10.255.255.1/24")));

    assertThat(result._warnings.getParseWarnings(), hasSize(0));
    assertThat(result._warnings.getRedFlagWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorMtuFixtureAssertsFullInterfaceSet() {
    ExtractionResult result = parseAndExtract("mikrotik_mtu_interfaces");
    Map<String, MikrotikInterface> interfaces = result._configuration.getMainVrf().getInterfaces();
    assertThat(
        interfaces.keySet(),
        containsInAnyOrder(
            "bridge-lan",
            "ether1",
            "ether2",
            "ether3",
            "ether4",
            "ether5",
            "ether6",
            "ether7",
            "ether8",
            "eoip-l2",
            "gre-core",
            "ipip-backhaul",
            "vlan110",
            "vlan120"));

    assertThat(interfaces.get("bridge-lan").getMtu(), equalTo(1588));
    assertThat(interfaces.get("ether1").getMtu(), equalTo(1501));
    assertThat(interfaces.get("ether2").getMtu(), equalTo(1598));
    assertThat(interfaces.get("eoip-l2").getMtu(), equalTo(1458));
    assertThat(interfaces.get("gre-core").getMtu(), equalTo(1476));
    assertThat(interfaces.get("ipip-backhaul").getMtu(), equalTo(1480));
    assertThat(interfaces.get("vlan110").getMtu(), equalTo(1496));
    assertThat(interfaces.get("vlan120").getMtu(), equalTo(1488));
    assertThat(interfaces.get("ether3").getMtu(), equalTo((Integer) null));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
    assertThat(result._warnings.getRedFlagWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorNetwatchFixture() {
    ExtractionResult result = parseAndExtract("mikrotik_netwatch_basic");

    assertThat(
        result._configuration.getNetwatch().keySet(),
        containsInAnyOrder("track-core-gw", "track-isp-gw", "track-dns", "track-disabled"));
    assertThat(
        result._configuration.getNetwatch().get("track-core-gw").getHost(), equalTo("192.0.2.1"));
    assertThat(
        result._configuration.getNetwatch().get("track-core-gw").getInterval(),
        equalTo("00:00:10"));
    assertThat(result._configuration.getNetwatch().get("track-core-gw").getTimeout(), equalTo("3s"));
    assertThat(
        result._configuration.getNetwatch().get("track-core-gw").isDisabled(), equalTo(false));
    assertThat(
        result._configuration.getNetwatch().get("track-isp-gw").getHost(),
        equalTo("198.51.100.1"));
    assertThat(
        result._configuration.getNetwatch().get("track-dns").getHost(), equalTo("203.0.113.53"));
    assertThat(
        result._configuration.getNetwatch().get("track-disabled").isDisabled(), equalTo(true));
    assertThat(
        result._configuration.getNetwatch().get("track-disabled").getHost(),
        equalTo("203.0.113.54"));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorVrrpFixture() {
    ExtractionResult result = parseAndExtract("mikrotik_vrrp_basic");

    assertThat(
        result._configuration.getVrrpGroups().keySet(),
        containsInAnyOrder("vrrp-lan", "vrrp-wan", "vrrp-disabled"));
    assertThat(
        result._configuration.getVrrpGroups().get("vrrp-lan").getParentInterface(),
        equalTo("vlan10-lan"));
    assertThat(result._configuration.getVrrpGroups().get("vrrp-lan").getVrid(), equalTo(10));
    assertThat(result._configuration.getVrrpGroups().get("vrrp-lan").getPriority(), equalTo(110));
    assertThat(result._configuration.getVrrpGroups().get("vrrp-lan").isPreempt(), equalTo(true));
    assertThat(
        result._configuration.getVrrpGroups().get("vrrp-lan").getVirtualAddress(),
        equalTo(Ip.parse("10.10.10.254")));
    assertThat(
        result._configuration.getVrrpGroups().get("vrrp-wan").getParentInterface(),
        equalTo("vlan20-wan"));
    assertThat(result._configuration.getVrrpGroups().get("vrrp-wan").getPriority(), equalTo(90));
    assertThat(result._configuration.getVrrpGroups().get("vrrp-wan").isPreempt(), equalTo(false));
    assertThat(
        result._configuration.getVrrpGroups().get("vrrp-disabled").isDisabled(), equalTo(true));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testViAndDataplaneBondingVlanFromFixture() throws Exception {
    ExtractionResult result = parseAndExtract("mikrotik_bonding_vlan_vi");
    Configuration viConfig = getOnlyElement(result._configuration.toVendorIndependentConfigurations());

    assertThat(viConfig.getAllInterfaces(), hasKey("bond-core"));
    assertThat(viConfig.getAllInterfaces(), hasKey("vlan200-bond-wan"));

    org.batfish.datamodel.Interface bondCore = viConfig.getAllInterfaces().get("bond-core");
    org.batfish.datamodel.Interface vlan200 =
        viConfig.getAllInterfaces().get("vlan200-bond-wan");
    assertThat(
        bondCore.getInterfaceType(), equalTo(org.batfish.datamodel.InterfaceType.AGGREGATED));
    assertThat(bondCore.getChannelGroupMembers(), containsInAnyOrder("ether1", "ether2"));
    assertThat(viConfig.getAllInterfaces().get("ether1").getChannelGroup(), equalTo("bond-core"));
    assertThat(viConfig.getAllInterfaces().get("ether2").getChannelGroup(), equalTo("bond-core"));
    assertThat(vlan200.getAddress(), equalTo(ConcreteInterfaceAddress.parse("172.16.200.2/30")));
    assertThat(vlan200.getVlan(), equalTo((Integer) 200));

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    ImmutableMap.of(
                        "mtik-bonding-vlan-vi",
                        readResource(TESTCONFIGS_PREFIX + "mikrotik_bonding_vlan_vi", UTF_8)))
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    String hostname =
        getOnlyElement(batfish.loadConfigurations(batfish.getSnapshot()).values()).getHostname();
    Set<AbstractRoute> routes =
        dp.getRibs().get(hostname, Configuration.DEFAULT_VRF_NAME).getRoutes();
    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.CONNECTED
                        && route.getNetwork().equals(Prefix.parse("172.16.200.0/30"))),
        equalTo(true));
  }

  @Test
  public void testViAndDataplaneTunnelFromFixture() throws Exception {
    ExtractionResult result = parseAndExtract("mikrotik_tunnel_interfaces");
    Configuration viConfig = getOnlyElement(result._configuration.toVendorIndependentConfigurations());

    assertThat(viConfig.getAllInterfaces(), hasKey("gre-core"));
    assertThat(viConfig.getAllInterfaces(), hasKey("ipip-backhaul"));
    assertThat(viConfig.getAllInterfaces(), hasKey("eoip-l2"));
    assertThat(viConfig.getAllInterfaces(), hasKey("wg-overlay"));

    org.batfish.datamodel.Interface greCore = viConfig.getAllInterfaces().get("gre-core");
    org.batfish.datamodel.Interface ipipBackhaul = viConfig.getAllInterfaces().get("ipip-backhaul");
    org.batfish.datamodel.Interface eoipL2 = viConfig.getAllInterfaces().get("eoip-l2");
    org.batfish.datamodel.Interface wgOverlay = viConfig.getAllInterfaces().get("wg-overlay");
    assertThat(greCore.getInterfaceType(), equalTo(org.batfish.datamodel.InterfaceType.TUNNEL));
    assertThat(ipipBackhaul.getInterfaceType(), equalTo(org.batfish.datamodel.InterfaceType.TUNNEL));
    assertThat(eoipL2.getInterfaceType(), equalTo(org.batfish.datamodel.InterfaceType.TUNNEL));
    assertThat(wgOverlay.getInterfaceType(), equalTo(org.batfish.datamodel.InterfaceType.TUNNEL));
    assertThat(greCore.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.0.2/30")));
    assertThat(ipipBackhaul.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.1.2/30")));
    assertThat(wgOverlay.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.255.255.1/24")));

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    ImmutableMap.of(
                        "mtik-tunnel-vi",
                        readResource(TESTCONFIGS_PREFIX + "mikrotik_tunnel_interfaces", UTF_8)))
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    String hostname =
        getOnlyElement(batfish.loadConfigurations(batfish.getSnapshot()).values()).getHostname();
    Set<AbstractRoute> routes =
        dp.getRibs().get(hostname, Configuration.DEFAULT_VRF_NAME).getRoutes();
    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.CONNECTED
                        && route.getNetwork().equals(Prefix.parse("10.0.0.0/30"))),
        equalTo(true));
    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.CONNECTED
                        && route.getNetwork().equals(Prefix.parse("10.0.1.0/30"))),
        equalTo(true));
    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.CONNECTED
                        && route.getNetwork().equals(Prefix.parse("10.255.255.0/24"))),
        equalTo(true));
  }

  @Test
  public void testViVrrpConversionFromFixture() {
    ExtractionResult result = parseAndExtract("mikrotik_vrrp_basic");
    Warnings conversionWarnings = new Warnings(false, true, false);
    result._configuration.setWarnings(conversionWarnings);
    Configuration viConfig = getOnlyElement(result._configuration.toVendorIndependentConfigurations());

    org.batfish.datamodel.Interface vlan10 = viConfig.getAllInterfaces().get("vlan10-lan");
    org.batfish.datamodel.Interface vlan20 = viConfig.getAllInterfaces().get("vlan20-wan");
    assertThat(vlan10.getVrrpGroups(), hasKey(10));
    assertThat(vlan10.getVrrpGroups().get(10).getPriority(), equalTo(110));
    assertThat(vlan10.getVrrpGroups().get(10).getPreempt(), equalTo(true));
    assertThat(
        vlan10.getVrrpGroups().get(10).getSourceAddress(),
        equalTo(ConcreteInterfaceAddress.parse("10.10.10.1/24")));
    assertThat(vlan10.getVrrpGroups().get(10).getVirtualAddresses(), hasKey("vlan10-lan"));
    assertThat(
        vlan10.getVrrpGroups().get(10).getVirtualAddresses().get("vlan10-lan"),
        contains(Ip.parse("10.10.10.254")));
    assertThat(vlan20.getVrrpGroups(), hasKey(20));
    assertThat(vlan20.getVrrpGroups().get(20).getPriority(), equalTo(90));
    assertThat(vlan10.getVrrpGroups().containsKey(30), equalTo(false));
    assertThat(conversionWarnings.getRedFlagWarnings(), hasSize(0));
  }

  @Test
  public void testViVrrpMissingParentInterfaceWarning() {
    String src =
        "/interface vrrp add interface=missing-iface name=vrrp-missing vrid=10"
            + " virtual-address=10.0.0.254/24\n";
    ExtractionResult result = parseAndExtractFromString(src);
    Warnings conversionWarnings = new Warnings(false, true, false);
    result._configuration.setWarnings(conversionWarnings);
    Configuration unused =
        getOnlyElement(result._configuration.toVendorIndependentConfigurations());
    assertThat(unused, notNullValue());

    assertThat(conversionWarnings.getRedFlagWarnings(), hasSize(1));
    assertThat(
        conversionWarnings.getRedFlagWarnings().first().getText(),
        containsString("references nonexistent interface 'missing-iface'"));
  }

  @Test
  public void testViVrrpMissingSourceAddressWarning() {
    String src =
        "/interface ethernet set [ find default-name=ether1 ] disable-running-check=no\n"
            + "/interface vlan add interface=ether1 name=vlan10 vlan-id=10\n"
            + "/interface vrrp add interface=vlan10 name=vrrp-no-source vrid=10"
            + " virtual-address=10.10.10.254/24\n";
    ExtractionResult result = parseAndExtractFromString(src);
    Warnings conversionWarnings = new Warnings(false, true, false);
    result._configuration.setWarnings(conversionWarnings);
    Configuration unused =
        getOnlyElement(result._configuration.toVendorIndependentConfigurations());
    assertThat(unused, notNullValue());

    assertThat(conversionWarnings.getRedFlagWarnings(), hasSize(1));
    assertThat(
        conversionWarnings.getRedFlagWarnings().first().getText(),
        containsString("parent interface 'vlan10' has no concrete address"));
  }

  @Test
  public void testViMtuFromFixture() {
    ExtractionResult result = parseAndExtract("mikrotik_mtu_interfaces");
    Configuration viConfig = getOnlyElement(result._configuration.toVendorIndependentConfigurations());

    assertThat(viConfig.getAllInterfaces().get("bridge-lan").getMtu(), equalTo((Integer) 1588));
    assertThat(viConfig.getAllInterfaces().get("ether1").getMtu(), equalTo((Integer) 1501));
    assertThat(viConfig.getAllInterfaces().get("ether2").getMtu(), equalTo((Integer) 1598));
    assertThat(viConfig.getAllInterfaces().get("eoip-l2").getMtu(), equalTo((Integer) 1458));
    assertThat(viConfig.getAllInterfaces().get("gre-core").getMtu(), equalTo((Integer) 1476));
    assertThat(viConfig.getAllInterfaces().get("ipip-backhaul").getMtu(), equalTo((Integer) 1480));
    assertThat(viConfig.getAllInterfaces().get("vlan110").getMtu(), equalTo((Integer) 1496));
    assertThat(viConfig.getAllInterfaces().get("vlan120").getMtu(), equalTo((Integer) 1488));
    assertThat(
        viConfig.getAllInterfaces().get("ether3").getMtu(),
        equalTo((Integer) org.batfish.datamodel.Interface.DEFAULT_MTU));
  }

  @Test
  public void testViAndDataplaneMtuFromFixture() throws Exception {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    ImmutableMap.of(
                        "mtik-mtu-vi",
                        readResource(TESTCONFIGS_PREFIX + "mikrotik_mtu_interfaces", UTF_8)))
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    List<Prefix> connectedNetworks =
        dp.getRibs().values().stream()
            .flatMap(rib -> rib.getRoutes().stream())
            .filter(route -> route.getProtocol() == RoutingProtocol.CONNECTED)
            .map(AbstractRoute::getNetwork)
            .toList();
    assertThat(connectedNetworks, hasItem(Prefix.parse("192.0.2.0/30")));
    assertThat(connectedNetworks, hasItem(Prefix.parse("10.11.0.0/24")));
    assertThat(connectedNetworks, hasItem(Prefix.parse("10.12.0.0/24")));
    assertThat(connectedNetworks, hasItem(Prefix.parse("10.0.0.0/30")));
    assertThat(connectedNetworks, hasItem(Prefix.parse("10.0.1.0/30")));
  }

  @Test
  public void testDataplaneNetwatchFixtureSmoke() throws Exception {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    ImmutableMap.of(
                        "mtik-netwatch-basic",
                        readResource(TESTCONFIGS_PREFIX + "mikrotik_netwatch_basic", UTF_8)))
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    String hostname =
        getOnlyElement(batfish.loadConfigurations(batfish.getSnapshot()).values()).getHostname();
    Set<AbstractRoute> routes =
        dp.getRibs().get(hostname, Configuration.DEFAULT_VRF_NAME).getRoutes();

    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.CONNECTED
                        && route.getNetwork().equals(Prefix.parse("192.0.2.0/30"))),
        equalTo(true));
    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.CONNECTED
                        && route.getNetwork().equals(Prefix.parse("198.51.100.0/30"))),
        equalTo(true));
    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.STATIC
                        && route.getNetwork().equals(Prefix.ZERO)
                        && route.getNextHopIp().equals(Ip.parse("192.0.2.1"))
                        && route.getAdministrativeCost() == 1L),
        equalTo(true));
  }

  @Test
  public void testDataplaneVrrpFixtureSmoke() throws Exception {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    ImmutableMap.of(
                        "mtik-vrrp-basic",
                        readResource(TESTCONFIGS_PREFIX + "mikrotik_vrrp_basic", UTF_8)))
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    String hostname =
        getOnlyElement(batfish.loadConfigurations(batfish.getSnapshot()).values()).getHostname();
    Set<AbstractRoute> routes =
        dp.getRibs().get(hostname, Configuration.DEFAULT_VRF_NAME).getRoutes();

    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.CONNECTED
                        && route.getNetwork().equals(Prefix.parse("10.10.10.0/24"))),
        equalTo(true));
    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.CONNECTED
                        && route.getNetwork().equals(Prefix.parse("198.51.100.0/24"))),
        equalTo(true));
    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.STATIC
                        && route.getNetwork().equals(Prefix.ZERO)
                        && route.getNextHopIp().equals(Ip.parse("198.51.100.1"))
                        && route.getAdministrativeCost() == 1L),
        equalTo(true));
  }

  @Test
  public void testBridgePortExtraction() {
    String src =
        "/interface bridge port add bridge=bridge-lan interface=customer-a pvid=10\n"
            + "/interface bridge port add bridge=bridge-lan interface=customer-b pvid=20\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(tree.toStringTree(parser.getParser()), containsString("if_bridge_port_prop_pvid"));

    ExtractionResult result = parseAndExtractFromString(src);

    List<MikrotikBridgePort> bridgePorts = result._configuration.getBridgePorts();
    assertThat(bridgePorts, hasSize(2));
    assertThat(bridgePorts.get(0).getBridge(), equalTo("bridge-lan"));
    assertThat(bridgePorts.get(0).getInterface(), equalTo("customer-a"));
    assertThat(bridgePorts.get(0).getPvid(), equalTo(10));
    assertThat(bridgePorts.get(1).getBridge(), equalTo("bridge-lan"));
    assertThat(bridgePorts.get(1).getInterface(), equalTo("customer-b"));
    assertThat(bridgePorts.get(1).getPvid(), equalTo(20));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testBridgePortInvalidPvidWarning() {
    String src = "/interface bridge port add bridge=bridge-lan interface=customer-a pvid=abc\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getBridgePorts(), hasSize(1));
    assertThat(result._configuration.getBridgePorts().get(0).getPvid(), equalTo((Integer) null));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Invalid pvid value"));
  }

  @Test
  public void testBridgeVlanExtraction() {
    String src =
        "/interface bridge vlan add bridge=bridge-lan vlan-ids=10,20 "
            + "tagged=bridge-lan,ether1 untagged=ether2,ether3\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(tree.toStringTree(parser.getParser()), containsString("interface_bridge_vlan_add"));
    assertThat(tree.toStringTree(parser.getParser()), containsString("value_list"));

    ExtractionResult result = parseAndExtractFromString(src);
    List<MikrotikBridgeVlan> bridgeVlans = result._configuration.getBridgeVlans();
    assertThat(bridgeVlans, hasSize(1));

    MikrotikBridgeVlan bridgeVlan = bridgeVlans.get(0);
    assertThat(bridgeVlan.getBridge(), equalTo("bridge-lan"));
    assertThat(bridgeVlan.getVlanIds(), containsInAnyOrder(10, 20));
    assertThat(bridgeVlan.getTagged(), containsInAnyOrder("bridge-lan", "ether1"));
    assertThat(bridgeVlan.getUntagged(), containsInAnyOrder("ether2", "ether3"));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testBridgeVlanInvalidVlanIdWarning() {
    String src =
        "/interface bridge vlan add bridge=bridge-lan vlan-ids=10,abc "
            + "tagged=bridge-lan,ether1 untagged=ether2\n";
    ExtractionResult result = parseAndExtractFromString(src);

    assertThat(result._configuration.getBridgeVlans(), hasSize(1));
    MikrotikBridgeVlan bridgeVlan = result._configuration.getBridgeVlans().get(0);
    assertThat(bridgeVlan.getVlanIds(), contains(10));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Invalid vlan-id 'abc' in bridge vlan entry"));
  }

  @Test
  public void testBridgeStructureReferences() {
    String src =
        "/interface bridge port add bridge=bridge-lan interface=customer-a pvid=10\n"
            + "/interface bridge vlan add bridge=bridge-lan vlan-ids=10 "
            + "tagged=bridge-lan,customer-a untagged=customer-b\n";
    ExtractionResult result = parseAndExtractFromString(src);

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(INTERFACE);
    assertThat(references.get("bridge-lan").get(BRIDGE_PORT_BRIDGE), hasItem(1));
    assertThat(references.get("customer-a").get(BRIDGE_PORT_INTERFACE), hasItem(1));
    assertThat(references.get("bridge-lan").get(BRIDGE_VLAN_BRIDGE), hasItem(2));
    assertThat(references.get("customer-a").get(BRIDGE_VLAN_TAGGED_INTERFACE), hasItem(2));
    assertThat(references.get("customer-b").get(BRIDGE_VLAN_UNTAGGED_INTERFACE), hasItem(2));
  }

  @Test
  public void testMikrotikVlanInterfaceInvalidNumericValuesWarn() {
    String src =
        "/interface bridge add name=bridge-lan\n"
            + "/interface vlan add interface=bridge-lan name=vlan25 vlan-id=abc mtu=bad\n";
    ExtractionResult result = parseAndExtractFromString(src);

    MikrotikInterface vlan25 = result._configuration.getMainVrf().getInterfaces().get("vlan25");
    assertThat(vlan25, notNullValue());
    assertThat(vlan25.getVlanId(), equalTo((Integer) null));
    assertThat(vlan25.getMtu(), equalTo((Integer) null));
    assertThat(result._warnings.getParseWarnings(), hasSize(2));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(), containsString("Invalid vlan-id value"));
    assertThat(
        result._warnings.getParseWarnings().get(1).getComment(), containsString("Invalid mtu value"));
  }

  @Test
  public void testMikrotikInterfaceExtractionFromFixtureIncludesVlans() {
    ExtractionResult result = parseAndExtract("mikrotik_interfaces_and_routes.export");
    MikrotikInterface vlan25 = result._configuration.getMainVrf().getInterfaces().get("vlan25");
    MikrotikInterface vlan30 = result._configuration.getMainVrf().getInterfaces().get("vlan30");

    assertThat(vlan25, notNullValue());
    assertThat(vlan25.getVlanId(), equalTo(25));
    assertThat(vlan25.getParentInterface(), equalTo("bridge-lan"));
    assertThat(
        vlan25.getAddresses(), contains(ConcreteInterfaceAddress.parse("192.168.25.1/24")));
    assertThat(vlan30, notNullValue());
    assertThat(vlan30.getVlanId(), equalTo(30));
    assertThat(vlan30.getParentInterface(), equalTo("ether6"));
    assertThat(vlan30.getAddresses(), hasSize(0));
  }

  @Test
  public void testMikrotikSystemIdentityExtraction() {
    ExtractionResult result =
        parseAndExtractFromString("/system identity set name=mtik-inline\n");

    assertThat(result._configuration.getHostname(), equalTo("mtik-inline"));
  }

  @Test
  public void testMikrotikSystemIdentityExtractionFromFixture() {
    ExtractionResult result = parseAndExtract("mikrotik_interfaces_and_routes.export");

    assertThat(result._configuration.getHostname(), equalTo("mtik-edge-01"));
  }

  @Test
  public void testMikrotikSystemIdentityHostnameUsedForViConversion() {
    ExtractionResult result = parseAndExtract("mikrotik_interfaces_and_routes.export");
    result._configuration.setFilename("configs/fallback-hostname");

    Configuration viConfig = getOnlyElement(result._configuration.toVendorIndependentConfigurations());
    assertThat(viConfig.getHostname(), equalTo("mtik-edge-01"));
  }

  @Test
  public void testMikrotikStaticRouteExtraction() {
    ExtractionResult result = parseAndExtract("mikrotik_static_route_basic");
    List<MikrotikStaticRoute> routes = result._configuration.getMainVrf().getStaticRoutes();

    assertThat(routes, hasSize(2));
    assertThat(routes.get(0).getNetwork(), equalTo(Prefix.ZERO));
    assertThat(routes.get(0).getNextHopIp(), equalTo(Ip.parse("10.0.0.1")));
    assertThat(routes.get(0).getAdminDistance(), equalTo(1));

    assertThat(routes.get(1).getNetwork(), equalTo(Prefix.parse("198.51.100.0/24")));
    assertThat(routes.get(1).getNextHopIp(), equalTo(Ip.parse("10.0.0.1")));
    assertThat(routes.get(1).getAdminDistance(), equalTo(10));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testMikrotikStaticRouteInvalidGatewayWarning() {
    ExtractionResult result = parseAndExtractFromString("/ip route add gateway=not-an-ip\n");
    assertThat(result._configuration.getMainVrf().getStaticRoutes(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Invalid gateway IP: not-an-ip"));
  }

  @Test
  public void testLexerTokenizesFirewallAddressListKeywords() {
    String input = "/ip firewall address-list add list=trusted-hosts address=10.0.0.1\n";
    MikrotikLexer lexer = new MikrotikLexer(CharStreams.fromString(input));
    List<Integer> tokenTypes = lexer.getAllTokens().stream().map(Token::getType).toList();

    assertThat(tokenTypes, hasItem(MikrotikLexer.ADDRESS_LIST));
    assertThat(tokenTypes, hasItem(MikrotikLexer.LIST));
    assertThat(tokenTypes, hasItem(MikrotikLexer.ADD));
  }

  @Test
  public void testLexerTokenizesFirewallReferenceKeywords() {
    String input = "src-address-list=foo dst-address-list=bar\n";
    MikrotikLexer lexer = new MikrotikLexer(CharStreams.fromString(input));
    List<Integer> tokenTypes = lexer.getAllTokens().stream().map(Token::getType).toList();

    assertThat(tokenTypes, hasItem(MikrotikLexer.SRC_ADDRESS_LIST));
    assertThat(tokenTypes, hasItem(MikrotikLexer.DST_ADDRESS_LIST));
  }

  @Test
  public void testParserParsesInlineFirewallAddressListWithoutGenericCommand() {
    String src = "/ip firewall address-list add list=trusted-hosts address=10.0.0.1\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    String treeText = tree.toStringTree(parser.getParser());
    assertThat(treeText, containsString("ip_firewall_address_list_add"));
    assertThat(treeText.contains("generic_command"), equalTo(false));
  }

  @Test
  public void testParserParsesInlineFirewallFilterWithoutGenericCommand() {
    String src = "/ip firewall filter add chain=input action=accept src-address-list=mylist\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    String treeText = tree.toStringTree(parser.getParser());
    assertThat(treeText, containsString("ip_firewall_filter_add"));
    assertThat(treeText.contains("generic_command"), equalTo(false));
  }

  @Test
  public void testExtractorFirewallAddressListHostEntry() {
    ExtractionResult result =
        parseAndExtractFromString(
            "/ip firewall address-list add list=mylist address=10.1.1.1 comment=\"a host\"\n");

    assertThat(result._configuration.getAddressLists(), hasKey("mylist"));
    assertThat(result._configuration.getAddressLists().get("mylist").getEntries(), hasSize(1));
    assertThat(
        result._configuration.getAddressLists().get("mylist").getEntries().get(0).getHostIp(),
        equalTo(Ip.parse("10.1.1.1")));
    assertThat(
        result._configuration.getAddressLists().get("mylist").getEntries().get(0).getPrefix(),
        equalTo((Prefix) null));
    assertThat(
        result._configuration.getAddressLists().get("mylist").getEntries().get(0).getComment(),
        equalTo("a host"));
    assertThat(
        result._configuration.getAddressLists().get("mylist").getEntries().get(0).isDisabled(),
        equalTo(false));
    assertThat(result._warnings.getParseWarnings(), hasSize(0));
  }

  @Test
  public void testExtractorFirewallAddressListPrefixEntry() {
    ExtractionResult result =
        parseAndExtractFromString("/ip firewall address-list add list=mylist address=10.1.0.0/16\n");

    assertThat(result._configuration.getAddressLists(), hasKey("mylist"));
    assertThat(
        result._configuration.getAddressLists().get("mylist").getEntries().get(0).getPrefix(),
        equalTo(Prefix.parse("10.1.0.0/16")));
    assertThat(
        result._configuration.getAddressLists().get("mylist").getEntries().get(0).getHostIp(),
        equalTo((Ip) null));
  }

  @Test
  public void testExtractorFirewallAddressListDisabledStored() {
    ExtractionResult result =
        parseAndExtractFromString(
            "/ip firewall address-list add list=mylist address=10.1.1.1 disabled=yes\n");

    assertThat(result._configuration.getAddressLists(), hasKey("mylist"));
    assertThat(result._configuration.getAddressLists().get("mylist").getEntries(), hasSize(1));
    assertThat(
        result._configuration.getAddressLists().get("mylist").getEntries().get(0).isDisabled(),
        equalTo(true));
  }

  @Test
  public void testExtractorFirewallAddressListInvalidAddressWarning() {
    ExtractionResult result =
        parseAndExtractFromString("/ip firewall address-list add list=mylist address=not-an-ip\n");

    assertThat(result._configuration.getAddressLists().entrySet(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(result._warnings.getParseWarnings().get(0).getComment(), containsString("invalid address"));
  }

  @Test
  public void testExtractorFirewallAddressListInvalidPrefixWarning() {
    ExtractionResult result =
        parseAndExtractFromString("/ip firewall address-list add list=mylist address=10.0.0.0/99\n");

    assertThat(result._configuration.getAddressLists().entrySet(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(result._warnings.getParseWarnings().get(0).getComment(), containsString("invalid prefix"));
  }

  @Test
  public void testExtractorFirewallAddressListMissingListWarning() {
    ExtractionResult result =
        parseAndExtractFromString("/ip firewall address-list add address=10.1.1.1\n");

    assertThat(result._configuration.getAddressLists().entrySet(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("missing required list="));
  }

  @Test
  public void testExtractorFirewallAddressListMissingAddressWarning() {
    ExtractionResult result =
        parseAndExtractFromString("/ip firewall address-list add list=mylist\n");

    assertThat(result._configuration.getAddressLists().entrySet(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("missing required address="));
  }

  @Test
  public void testExtractorFirewallFilterSrcAddressListReference() {
    ExtractionResult result =
        parseAndExtractFromString(
            "/ip firewall filter add chain=input action=accept src-address-list=mylist\n");

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(FIREWALL_ADDRESS_LIST);
    assertThat(references, hasKey("mylist"));
    assertThat(references.get("mylist"), hasKey(FIREWALL_FILTER_SRC_ADDRESS_LIST));
  }

  @Test
  public void testExtractorFirewallFilterDstAddressListReference() {
    ExtractionResult result =
        parseAndExtractFromString(
            "/ip firewall filter add chain=forward action=accept dst-address-list=mylist\n");

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(FIREWALL_ADDRESS_LIST);
    assertThat(references, hasKey("mylist"));
    assertThat(references.get("mylist"), hasKey(FIREWALL_FILTER_DST_ADDRESS_LIST));
  }

  @Test
  public void testExtractorFirewallNatSrcAddressListReference() {
    ExtractionResult result =
        parseAndExtractFromString(
            "/ip firewall nat add chain=srcnat action=masquerade src-address-list=mylist\n");

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(FIREWALL_ADDRESS_LIST);
    assertThat(references, hasKey("mylist"));
    assertThat(references.get("mylist"), hasKey(FIREWALL_NAT_SRC_ADDRESS_LIST));
  }

  @Test
  public void testExtractorFirewallNatDstAddressListReference() {
    ExtractionResult result =
        parseAndExtractFromString(
            "/ip firewall nat add chain=dstnat action=dst-nat dst-address-list=mylist\n");

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(FIREWALL_ADDRESS_LIST);
    assertThat(references, hasKey("mylist"));
    assertThat(references.get("mylist"), hasKey(FIREWALL_NAT_DST_ADDRESS_LIST));
  }

  @Test
  public void testExtractorFirewallAddressListFixture() {
    ExtractionResult result = parseAndExtract("mikrotik_address_list_basic");

    assertThat(
        result._configuration.getAddressLists().keySet(),
        containsInAnyOrder("trusted-hosts", "branch-prefixes", "wan-monitors", "disabled-entry"));
    assertThat(result._configuration.getAddressLists().get("trusted-hosts").getEntries(), hasSize(2));
    assertThat(
        result._configuration.getAddressLists().get("trusted-hosts").getEntries().get(0).getHostIp(),
        equalTo(Ip.parse("192.0.2.10")));
    assertThat(
        result._configuration.getAddressLists().get("trusted-hosts").getEntries().get(0).getComment(),
        equalTo("single host"));
    assertThat(
        result._configuration.getAddressLists().get("trusted-hosts").getEntries().get(1).getHostIp(),
        equalTo(Ip.parse("192.0.2.11")));
    assertThat(
        result._configuration.getAddressLists().get("trusted-hosts").getEntries().get(1).getComment(),
        equalTo("second host"));
    assertThat(result._configuration.getAddressLists().get("branch-prefixes").getEntries(), hasSize(2));
    assertThat(
        result._configuration
            .getAddressLists()
            .get("branch-prefixes")
            .getEntries()
            .get(0)
            .getPrefix(),
        equalTo(Prefix.parse("10.10.10.0/24")));
    assertThat(
        result._configuration
            .getAddressLists()
            .get("branch-prefixes")
            .getEntries()
            .get(0)
            .getComment(),
        equalTo("branch a"));
    assertThat(
        result._configuration
            .getAddressLists()
            .get("branch-prefixes")
            .getEntries()
            .get(1)
            .getPrefix(),
        equalTo(Prefix.parse("10.20.20.0/24")));
    assertThat(
        result._configuration
            .getAddressLists()
            .get("branch-prefixes")
            .getEntries()
            .get(1)
            .getComment(),
        equalTo("branch b"));
    assertThat(result._configuration.getAddressLists().get("wan-monitors").getEntries(), hasSize(2));
    assertThat(result._configuration.getAddressLists().get("disabled-entry").getEntries(), hasSize(1));
    assertThat(
        result._configuration
            .getAddressLists()
            .get("disabled-entry")
            .getEntries()
            .get(0)
            .isDisabled(),
        equalTo(true));
    assertThat(result._warnings.getParseWarnings(), hasSize(2));
    assertThat(
        result._warnings.getParseWarnings().stream()
            .map(Warnings.ParseWarning::getComment)
            .anyMatch(comment -> comment.contains("not-an-ip")),
        equalTo(true));
    assertThat(
        result._warnings.getParseWarnings().stream()
            .map(Warnings.ParseWarning::getComment)
            .anyMatch(comment -> comment.contains("10.0.0.0/99")),
        equalTo(true));
  }

  @Test
  public void testExtractorFirewallAddressListStructureTrackingFromFixture() {
    ExtractionResult result = parseAndExtract("mikrotik_address_list_basic");

    assertThat(
        result
            ._configuration
            .getStructureManager()
            .hasDefinition(FIREWALL_ADDRESS_LIST.getDescription(), "trusted-hosts"),
        equalTo(true));
    assertThat(
        result
            ._configuration
            .getStructureManager()
            .hasDefinition(FIREWALL_ADDRESS_LIST.getDescription(), "branch-prefixes"),
        equalTo(true));
    assertThat(
        result
            ._configuration
            .getStructureManager()
            .hasDefinition(FIREWALL_ADDRESS_LIST.getDescription(), "wan-monitors"),
        equalTo(true));
    assertThat(
        result
            ._configuration
            .getStructureManager()
            .hasDefinition(FIREWALL_ADDRESS_LIST.getDescription(), "disabled-entry"),
        equalTo(true));
    assertThat(
        result
            ._configuration
            .getStructureManager()
            .hasDefinition(FIREWALL_ADDRESS_LIST.getDescription(), "bad-entries"),
        equalTo(false));

    Map<String, Map<org.batfish.vendor.StructureUsage, Multiset<Integer>>> references =
        result._configuration.getStructureManager().getStructureReferences(FIREWALL_ADDRESS_LIST);
    assertThat(references, hasKey("missing-list"));
    assertThat(references.get("missing-list"), hasKey(FIREWALL_FILTER_SRC_ADDRESS_LIST));
    assertThat(references.get("trusted-hosts"), hasKey(FIREWALL_FILTER_SRC_ADDRESS_LIST));
    assertThat(references.get("branch-prefixes"), hasKey(FIREWALL_FILTER_DST_ADDRESS_LIST));
    assertThat(references.get("wan-monitors"), hasKey(FIREWALL_FILTER_DST_ADDRESS_LIST));
    assertThat(references.get("branch-prefixes"), hasKey(FIREWALL_NAT_SRC_ADDRESS_LIST));
    assertThat(references.get("trusted-hosts"), hasKey(FIREWALL_NAT_DST_ADDRESS_LIST));
  }

  @Test
  public void testViFirewallAddressListConversionFromFixture() throws Exception {
    ExtractionResult result = parseAndExtract("mikrotik_address_list_basic");
    Configuration viConfig = getOnlyElement(result._configuration.toVendorIndependentConfigurations());

    assertThat(viConfig.getIpSpaces(), hasKey("trusted-hosts"));
    assertThat(viConfig.getIpSpaces(), hasKey("branch-prefixes"));
    assertThat(viConfig.getIpSpaces(), hasKey("wan-monitors"));
    assertThat(viConfig.getIpSpaces(), hasKey("disabled-entry"));
    assertThat(viConfig.getIpSpaces().get("trusted-hosts"), notNullValue());
    assertThat(viConfig.getIpSpaces().get("branch-prefixes"), notNullValue());
    assertThat(viConfig.getIpSpaces().get("wan-monitors"), notNullValue());
    assertThat(viConfig.getIpSpaces().get("disabled-entry"), equalTo(EmptyIpSpace.INSTANCE));

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    ImmutableMap.of(
                        "mtik-address-list-basic",
                        readResource(TESTCONFIGS_PREFIX + "mikrotik_address_list_basic", UTF_8)))
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    String hostname =
        getOnlyElement(batfish.loadConfigurations(batfish.getSnapshot()).values()).getHostname();
    Set<AbstractRoute> routes =
        dp.getRibs().get(hostname, Configuration.DEFAULT_VRF_NAME).getRoutes();
    assertThat(
        routes.stream()
            .anyMatch(
                route ->
                    route.getProtocol() == RoutingProtocol.CONNECTED
                        && route.getNetwork().equals(Prefix.parse("192.0.2.0/30"))),
        equalTo(true));
  }

  private static ExtractionResult parseAndExtract(String fixtureName) {
    String src = readResource(TESTCONFIGS_PREFIX + fixtureName, UTF_8);
    return parseAndExtractFromString(src);
  }

  private static ExtractionResult parseAndExtractFromString(String src) {
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    Warnings warnings = new Warnings();
    MikrotikControlPlaneExtractor extractor =
        new MikrotikControlPlaneExtractor(src, parser, warnings, new SilentSyntaxCollection());
    extractor.processParseTree(
        new NetworkSnapshot(new NetworkId("test-network"), new SnapshotId("test-snapshot")), tree);

    return new ExtractionResult((MikrotikConfiguration) extractor.getVendorConfiguration(), warnings);
  }

  private static final class ExtractionResult {
    private final MikrotikConfiguration _configuration;
    private final Warnings _warnings;

    private ExtractionResult(MikrotikConfiguration configuration, Warnings warnings) {
      _configuration = configuration;
      _warnings = warnings;
    }
  }
}
