package org.batfish.vendor.mikrotik.grammar;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureType.INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_PORT_BRIDGE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_PORT_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_VLAN_BRIDGE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_VLAN_TAGGED_INTERFACE;
import static org.batfish.vendor.mikrotik.representation.MikrotikStructureUsage.BRIDGE_VLAN_UNTAGGED_INTERFACE;
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

import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.batfish.common.BatfishLogger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.config.Settings;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.main.Batfish;
import org.batfish.vendor.mikrotik.representation.MikrotikBridgePort;
import org.batfish.vendor.mikrotik.representation.MikrotikBridgeVlan;
import org.batfish.vendor.mikrotik.representation.MikrotikConfiguration;
import org.batfish.vendor.mikrotik.representation.MikrotikInterface;
import org.batfish.vendor.mikrotik.representation.MikrotikStaticRoute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MikrotikGrammarTest {

  private static final String TESTCONFIGS_PREFIX =
      "org/batfish/vendor/mikrotik/grammar/testconfigs/";

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
    assertThat(vc.getInterfaces().keySet(), containsInAnyOrder("bridge-lan", "customer-a", "spare"));

    MikrotikInterface bridgeLan = vc.getInterfaces().get("bridge-lan");
    assertThat(bridgeLan, notNullValue());
    assertThat(bridgeLan.getType(), equalTo("bridge"));
    assertThat(bridgeLan.isDisabled(), equalTo(false));

    MikrotikInterface customerA = vc.getInterfaces().get("customer-a");
    assertThat(customerA, notNullValue());
    assertThat(customerA.getType(), equalTo("ethernet"));

    MikrotikInterface spare = vc.getInterfaces().get("spare");
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
        result._configuration.getInterfaces().get("uplink-core");
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
    MikrotikInterface ether6 = result._configuration.getInterfaces().get("ether6");
    MikrotikInterface vlan25 = result._configuration.getInterfaces().get("vlan25");
    MikrotikInterface vlan30 = result._configuration.getInterfaces().get("vlan30");
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

    MikrotikInterface vlan25 = result._configuration.getInterfaces().get("vlan25");
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
    MikrotikInterface vlan25 = result._configuration.getInterfaces().get("vlan25");
    MikrotikInterface vlan30 = result._configuration.getInterfaces().get("vlan30");

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
    List<MikrotikStaticRoute> routes = result._configuration.getStaticRoutes();

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
    assertThat(result._configuration.getStaticRoutes(), hasSize(0));
    assertThat(result._warnings.getParseWarnings(), hasSize(1));
    assertThat(
        result._warnings.getParseWarnings().get(0).getComment(),
        containsString("Invalid gateway IP: not-an-ip"));
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
