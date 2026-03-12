package org.batfish.vendor.mikrotik.grammar;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
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
