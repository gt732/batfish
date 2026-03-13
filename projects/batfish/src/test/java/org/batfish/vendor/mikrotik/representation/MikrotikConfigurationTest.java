package org.batfish.vendor.mikrotik.representation;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Vrf;
import org.junit.Test;

public final class MikrotikConfigurationTest {

  @Test
  public void testToVendorIndependentConfigurations() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setHostname("test-mikrotik");
    MikrotikInterface iface = new MikrotikInterface("ether1", "ether");
    ConcreteInterfaceAddress address = ConcreteInterfaceAddress.parse("10.0.0.1/24");
    iface.addAddress(address);
    vc.getInterfaces().put(iface.getName(), iface);

    List<Configuration> configs = vc.toVendorIndependentConfigurations();

    assertThat(configs, hasSize(1));
    Configuration c = getOnlyElement(configs);
    assertThat(c.getConfigurationFormat(), equalTo(ConfigurationFormat.MIKROTIK));
    assertThat(c.getHostname(), equalTo("test-mikrotik"));
    assertThat(c.getVrfs(), hasKey(Configuration.DEFAULT_VRF_NAME));
    assertThat(c.getAllInterfaces(), hasKey("ether1"));
    assertThat(c.getAllInterfaces().get("ether1").getAddress(), equalTo(address));
  }

  @Test
  public void testToVendorIndependentConfigurationsEmpty() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setFilename("configs/router1.rsc");

    List<Configuration> configs = vc.toVendorIndependentConfigurations();

    assertThat(configs, hasSize(1));
    Configuration c = getOnlyElement(configs);
    assertThat(c.getConfigurationFormat(), equalTo(ConfigurationFormat.MIKROTIK));
    assertThat(c.getHostname(), containsString("router1.rsc"));
    assertThat(c.getVrfs(), hasKey(Configuration.DEFAULT_VRF_NAME));
    assertThat(c.getAllInterfaces().isEmpty(), equalTo(true));
  }

  @Test
  public void testToVendorIndependentConfigurationsHostnameOverridesFilenameFallback() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setFilename("configs/router1.rsc");
    vc.setHostname("config-hostname");

    List<Configuration> configs = vc.toVendorIndependentConfigurations();

    assertThat(configs, hasSize(1));
    Configuration c = getOnlyElement(configs);
    assertThat(c.getHostname(), equalTo("config-hostname"));
  }

  @Test
  public void testToVendorIndependentConfigurationsWithStaticRoute() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setHostname("test-mikrotik");
    MikrotikInterface iface = new MikrotikInterface("ether1", "ether");
    iface.addAddress(ConcreteInterfaceAddress.parse("10.0.0.1/24"));
    vc.getInterfaces().put(iface.getName(), iface);
    vc.getStaticRoutes()
        .add(new MikrotikStaticRoute(Prefix.parse("0.0.0.0/0"), Ip.parse("10.0.0.254"), 1));

    List<Configuration> configs = vc.toVendorIndependentConfigurations();

    assertThat(configs, hasSize(1));
    Configuration c = getOnlyElement(configs);
    assertThat(c.getAllInterfaces(), hasKey("ether1"));
    Vrf defaultVrf = c.getVrfs().get(Configuration.DEFAULT_VRF_NAME);
    assertThat(defaultVrf.getStaticRoutes(), hasSize(1));
    StaticRoute sr = defaultVrf.getStaticRoutes().first();
    assertThat(sr.getNetwork(), equalTo(Prefix.parse("0.0.0.0/0")));
    assertThat(sr.getAdministrativeCost(), equalTo(1L));
  }

  @Test
  public void testToVendorIndependentConfigurationsEmptyStaticRoutes() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setHostname("test-mikrotik");
    MikrotikInterface iface = new MikrotikInterface("ether1", "ether");
    iface.addAddress(ConcreteInterfaceAddress.parse("10.0.0.1/24"));
    vc.getInterfaces().put(iface.getName(), iface);

    List<Configuration> configs = vc.toVendorIndependentConfigurations();

    assertThat(configs, hasSize(1));
    Configuration c = getOnlyElement(configs);
    Vrf defaultVrf = c.getVrfs().get(Configuration.DEFAULT_VRF_NAME);
    assertThat(defaultVrf.getStaticRoutes().isEmpty(), equalTo(true));
  }

  @Test
  public void testToVendorIndependentConfigurationsVlanInterface() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setHostname("test-mikrotik");
    MikrotikInterface iface = new MikrotikInterface("vlan25", "vlan");
    iface.setVlanId(25);
    iface.setParentInterface("bridge-lan");
    iface.setDisabled(true);
    ConcreteInterfaceAddress address = ConcreteInterfaceAddress.parse("192.168.25.1/24");
    iface.addAddress(address);
    vc.getInterfaces().put(iface.getName(), iface);

    List<Configuration> configs = vc.toVendorIndependentConfigurations();

    assertThat(configs, hasSize(1));
    Configuration c = getOnlyElement(configs);
    assertThat(c.getAllInterfaces(), hasKey("vlan25"));
    org.batfish.datamodel.Interface viIface = c.getAllInterfaces().get("vlan25");
    assertThat(viIface.getInterfaceType(), equalTo(InterfaceType.VLAN));
    assertThat(viIface.getVlan(), equalTo(25));
    assertThat(viIface.getAutoState(), equalTo(false));
    assertThat(viIface.getDependencies(), hasItem(new Dependency("bridge-lan", DependencyType.BIND)));
    assertThat(viIface.getAddress(), equalTo(address));
    assertThat(viIface.getAdminUp(), equalTo(false));
  }

  @Test
  public void testToVendorIndependentConfigurationsVlanInterfaceEmptyParentNoDependency() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setHostname("test-mikrotik");
    MikrotikInterface iface = new MikrotikInterface("vlan25", "vlan");
    iface.setVlanId(25);
    iface.setParentInterface("");
    vc.getInterfaces().put(iface.getName(), iface);

    List<Configuration> configs = vc.toVendorIndependentConfigurations();

    assertThat(configs, hasSize(1));
    Configuration c = getOnlyElement(configs);
    org.batfish.datamodel.Interface viIface = c.getAllInterfaces().get("vlan25");
    assertThat(viIface.getDependencies(), hasSize(0));
  }

  @Test
  public void testBridgeVlanSwitchportConversion() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setHostname("test-mikrotik");

    MikrotikInterface bridge = new MikrotikInterface("bridge-lan", "bridge");
    MikrotikInterface customerA = new MikrotikInterface("customer-a", "ethernet");
    MikrotikInterface customerB = new MikrotikInterface("customer-b", "ethernet");
    MikrotikInterface uplinkCore = new MikrotikInterface("uplink-core", "ethernet");
    MikrotikInterface mgmt = new MikrotikInterface("mgmt", "ethernet");
    vc.getInterfaces().put(bridge.getName(), bridge);
    vc.getInterfaces().put(customerA.getName(), customerA);
    vc.getInterfaces().put(customerB.getName(), customerB);
    vc.getInterfaces().put(uplinkCore.getName(), uplinkCore);
    vc.getInterfaces().put(mgmt.getName(), mgmt);

    MikrotikBridgePort bridgePortA = new MikrotikBridgePort("bridge-lan", "customer-a");
    bridgePortA.setPvid(10);
    vc.getBridgePorts().add(bridgePortA);
    MikrotikBridgePort bridgePortB = new MikrotikBridgePort("bridge-lan", "customer-b");
    bridgePortB.setPvid(20);
    vc.getBridgePorts().add(bridgePortB);

    MikrotikBridgeVlan vlan10 = new MikrotikBridgeVlan("bridge-lan");
    vlan10.addVlanId(10);
    vlan10.addTagged("bridge-lan");
    vlan10.addTagged("uplink-core");
    vlan10.addUntagged("bridge-lan");
    vlan10.addUntagged("customer-a");
    vlan10.addUntagged("uplink-core");
    MikrotikBridgeVlan vlan20 = new MikrotikBridgeVlan("bridge-lan");
    vlan20.addVlanId(20);
    vlan20.addTagged("bridge-lan");
    vlan20.addTagged("uplink-core");
    vlan20.addUntagged("customer-b");
    vlan20.addUntagged("uplink-core");
    vc.getBridgeVlans().add(vlan10);
    vc.getBridgeVlans().add(vlan20);

    Configuration c = getOnlyElement(vc.toVendorIndependentConfigurations());
    Interface viCustomerA = c.getAllInterfaces().get("customer-a");
    Interface viCustomerB = c.getAllInterfaces().get("customer-b");
    Interface viUplinkCore = c.getAllInterfaces().get("uplink-core");
    Interface viBridge = c.getAllInterfaces().get("bridge-lan");
    Interface viMgmt = c.getAllInterfaces().get("mgmt");

    assertThat(viCustomerA.getSwitchport(), equalTo(true));
    assertThat(viCustomerA.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
    assertThat(viCustomerA.getAccessVlan(), equalTo(10));

    assertThat(viCustomerB.getSwitchport(), equalTo(true));
    assertThat(viCustomerB.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
    assertThat(viCustomerB.getAccessVlan(), equalTo(20));

    assertThat(viUplinkCore.getSwitchport(), equalTo(true));
    assertThat(viUplinkCore.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
    assertThat(
        viUplinkCore.getAllowedVlans(),
        equalTo(IntegerSpace.of(10).union(IntegerSpace.of(20))));
    assertThat(viUplinkCore.getNativeVlan(), equalTo((Integer) null));

    assertThat(viBridge.getSwitchport(), equalTo(false));
    assertThat(viMgmt.getSwitchport(), equalTo(false));
  }

  @Test
  public void testBridgeVlanSwitchportConversionNativeVlanFromPvidOnTrunk() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setHostname("test-mikrotik");

    vc.getInterfaces().put("bridge-lan", new MikrotikInterface("bridge-lan", "bridge"));
    vc.getInterfaces().put("trunk1", new MikrotikInterface("trunk1", "ethernet"));

    MikrotikBridgePort trunkPort = new MikrotikBridgePort("bridge-lan", "trunk1");
    trunkPort.setPvid(100);
    vc.getBridgePorts().add(trunkPort);

    MikrotikBridgeVlan bridgeVlan = new MikrotikBridgeVlan("bridge-lan");
    bridgeVlan.addVlanId(200);
    bridgeVlan.addTagged("bridge-lan");
    bridgeVlan.addTagged("trunk1");
    vc.getBridgeVlans().add(bridgeVlan);

    Interface viTrunk = getOnlyElement(vc.toVendorIndependentConfigurations()).getAllInterfaces().get("trunk1");
    assertThat(viTrunk.getSwitchport(), equalTo(true));
    assertThat(viTrunk.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
    assertThat(viTrunk.getNativeVlan(), equalTo(100));
  }

  @Test
  public void testBridgeVlanSwitchportConversionSingleEntryMultiVlanExpansion() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setHostname("test-mikrotik");

    vc.getInterfaces().put("bridge-lan", new MikrotikInterface("bridge-lan", "bridge"));
    vc.getInterfaces().put("ether1", new MikrotikInterface("ether1", "ethernet"));

    MikrotikBridgeVlan bridgeVlan = new MikrotikBridgeVlan("bridge-lan");
    bridgeVlan.addVlanId(10);
    bridgeVlan.addVlanId(20);
    bridgeVlan.addTagged("bridge-lan");
    bridgeVlan.addTagged("ether1");
    vc.getBridgeVlans().add(bridgeVlan);

    Interface viEther1 = getOnlyElement(vc.toVendorIndependentConfigurations()).getAllInterfaces().get("ether1");
    assertThat(viEther1.getSwitchport(), equalTo(true));
    assertThat(viEther1.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
    assertThat(viEther1.getAllowedVlans(), equalTo(IntegerSpace.of(10).union(IntegerSpace.of(20))));
  }

  @Test
  public void testBridgePortDuplicateFirstWinsForNativeVlan() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.setHostname("test-mikrotik");

    vc.getInterfaces().put("bridge-lan", new MikrotikInterface("bridge-lan", "bridge"));
    vc.getInterfaces().put("trunk1", new MikrotikInterface("trunk1", "ethernet"));

    MikrotikBridgePort first = new MikrotikBridgePort("bridge-lan", "trunk1");
    first.setPvid(100);
    MikrotikBridgePort second = new MikrotikBridgePort("bridge-lan", "trunk1");
    second.setPvid(200);
    vc.getBridgePorts().add(first);
    vc.getBridgePorts().add(second);

    MikrotikBridgeVlan bridgeVlan = new MikrotikBridgeVlan("bridge-lan");
    bridgeVlan.addVlanId(300);
    bridgeVlan.addTagged("bridge-lan");
    bridgeVlan.addTagged("trunk1");
    vc.getBridgeVlans().add(bridgeVlan);

    Interface viTrunk = getOnlyElement(vc.toVendorIndependentConfigurations()).getAllInterfaces().get("trunk1");
    assertThat(viTrunk.getNativeVlan(), equalTo(100));
  }
}
