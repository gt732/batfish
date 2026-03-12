package org.batfish.dataplane;

import static org.batfish.datamodel.matchers.HopMatchers.hasNodeName;
import static org.batfish.datamodel.matchers.TraceMatchers.hasDisposition;
import static org.batfish.datamodel.matchers.TraceMatchers.hasLastHop;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;
import org.batfish.common.NetworkSnapshot;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.flow.Trace;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Reachability acceptance tests for mixed-vendor snapshots that include Mikrotik nodes. */
public final class MikrotikReachabilityTest {

  private static final String TESTRIGS_PREFIX = "org/batfish/dataplane/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Test
  public void testCoreToEdgeReachability() throws IOException {
    Batfish batfish =
        getBatfish(
            "mikrotik-mixed-vendor",
            ImmutableSet.of("cisco-core", "edge-mikrotik"));
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);

    Flow flow =
        Flow.builder()
            .setIngressNode("cisco-core")
            .setIngressInterface("GigabitEthernet0/0")
            .setDstIp(Ip.parse("192.168.10.100"))
            .setSrcIp(Ip.parse("10.0.0.1"))
            .build();

    SortedMap<Flow, List<Trace>> traces =
        batfish.getTracerouteEngine(snapshot).computeTraces(ImmutableSet.of(flow), false);

    assertThat(batfish.loadConfigurations(snapshot), hasKey("edge-mikrotik"));
    assertThat(
        traces.get(flow),
        contains(
            allOf(
                hasDisposition(
                    anyOf(
                        equalTo(FlowDisposition.ACCEPTED),
                        equalTo(FlowDisposition.DELIVERED_TO_SUBNET),
                        equalTo(FlowDisposition.INSUFFICIENT_INFO))),
                hasLastHop(hasNodeName("cisco-core")))));
  }

  @Test
  public void testCoreToEdgeReachabilityAltFixture() throws IOException {
    Batfish batfish =
        getBatfish("mikrotik-core-to-edge", ImmutableSet.of("cisco-core", "edge-mikrotik"));
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);

    Flow flow =
        Flow.builder()
            .setIngressNode("cisco-core")
            .setIngressInterface("GigabitEthernet0/0")
            .setDstIp(Ip.parse("192.168.11.50"))
            .setSrcIp(Ip.parse("10.11.0.1"))
            .build();

    SortedMap<Flow, List<Trace>> traces =
        batfish.getTracerouteEngine(snapshot).computeTraces(ImmutableSet.of(flow), false);

    assertThat(batfish.loadConfigurations(snapshot), hasKey("edge-mikrotik"));
    assertThat(
        traces.get(flow),
        contains(
            allOf(
                hasDisposition(
                    anyOf(
                        equalTo(FlowDisposition.ACCEPTED),
                        equalTo(FlowDisposition.DELIVERED_TO_SUBNET),
                        equalTo(FlowDisposition.INSUFFICIENT_INFO))),
                hasLastHop(hasNodeName("cisco-core")))));
  }

  @Test
  public void testTransitReachability() throws IOException {
    Batfish batfish =
        getBatfish(
            "mikrotik-transit",
            ImmutableSet.of("cisco-src", "mikrotik-transit", "cisco-dst"));
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);

    Flow flow =
        Flow.builder()
            .setIngressNode("cisco-src")
            .setIngressInterface("GigabitEthernet0/0")
            .setDstIp(Ip.parse("10.20.20.100"))
            .setSrcIp(Ip.parse("10.1.0.1"))
            .build();

    SortedMap<Flow, List<Trace>> traces =
        batfish.getTracerouteEngine(snapshot).computeTraces(ImmutableSet.of(flow), false);

    assertThat(batfish.loadConfigurations(snapshot), hasKey("mikrotik-transit"));
    assertThat(
        traces.get(flow),
        contains(
            allOf(
                hasDisposition(
                    anyOf(
                        equalTo(FlowDisposition.ACCEPTED),
                        equalTo(FlowDisposition.DELIVERED_TO_SUBNET),
                        equalTo(FlowDisposition.INSUFFICIENT_INFO))),
                hasLastHop(hasNodeName("cisco-src")))));
  }

  @Test
  public void testMikrotikParsesWithoutErrors() throws IOException {
    Batfish mixedVendor =
        getBatfish("mikrotik-mixed-vendor", ImmutableSet.of("cisco-core", "edge-mikrotik"));
    Batfish coreToEdge =
        getBatfish("mikrotik-core-to-edge", ImmutableSet.of("cisco-core", "edge-mikrotik"));
    Batfish transit =
        getBatfish("mikrotik-transit", ImmutableSet.of("cisco-src", "mikrotik-transit", "cisco-dst"));

    assertMikrotikConfigsRecognized(mixedVendor, ImmutableSet.of("configs/edge-mikrotik"));
    assertMikrotikConfigsRecognized(coreToEdge, ImmutableSet.of("configs/edge-mikrotik"));
    assertMikrotikConfigsRecognized(transit, ImmutableSet.of("configs/mikrotik-transit"));
  }

  private Batfish getBatfish(String testrigName, Iterable<String> configs) throws IOException {
    String testrigPrefix = TESTRIGS_PREFIX + testrigName;
    ImmutableSet<String> hostFiles =
        switch (testrigName) {
          case "mikrotik-mixed-vendor", "mikrotik-core-to-edge" -> ImmutableSet.of("edge-host.json");
          case "mikrotik-transit" -> ImmutableSet.of("dst-host.json");
          default -> ImmutableSet.of();
        };
    return BatfishTestUtils.getBatfishFromTestrigText(
        TestrigText.builder()
            .setConfigurationFiles(testrigPrefix, configs)
            .setHostsFiles(testrigPrefix, hostFiles)
            .setLayer1TopologyPrefix(testrigPrefix)
            .build(),
        _folder);
  }

  private static void assertMikrotikConfigsRecognized(Batfish batfish, ImmutableSet<String> configFiles)
      throws IOException {
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    Map<String, ParseStatus> statuses =
        pvcae.getParseStatus().entrySet().stream()
            .filter(e -> configFiles.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    assertThat(statuses.keySet(), containsInAnyOrder(configFiles.asList().toArray(new String[0])));
    for (ParseStatus status : ImmutableList.copyOf(statuses.values())) {
      assertThat(
          status,
          anyOf(
              equalTo(ParseStatus.PASSED), equalTo(ParseStatus.PARTIALLY_UNRECOGNIZED)));
    }
  }
}
