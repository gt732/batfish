package org.batfish.vendor.mikrotik.representation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Map;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.route.nh.NextHopIp;
import org.junit.Test;

public final class MikrotikConversionsTest {

  @Test
  public void testToViInterfaceEthernetWithAddress() {
    MikrotikInterface iface = new MikrotikInterface("ether1", "ether");
    iface.setDisabled(false);
    ConcreteInterfaceAddress address = ConcreteInterfaceAddress.parse("192.0.2.1/24");
    iface.addAddress(address);

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getName(), equalTo("ether1"));
    assertThat(viIface.getInterfaceType(), equalTo(InterfaceType.PHYSICAL));
    assertThat(viIface.getAdminUp(), equalTo(true));
    assertThat(viIface.getAddress(), equalTo(address));
  }

  @Test
  public void testToViInterfaceDisabled() {
    MikrotikInterface iface = new MikrotikInterface("ether2", "ether");
    iface.setDisabled(true);

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getAdminUp(), equalTo(false));
  }

  @Test
  public void testToViInterfaceBridgeType() {
    MikrotikInterface iface = new MikrotikInterface("bridge1", "bridge");

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getInterfaceType(), equalTo(InterfaceType.PHYSICAL));
  }

  @Test
  public void testToViInterfaceBondingTypeAndAggregateDependencies() {
    MikrotikInterface iface = new MikrotikInterface("bond-core", "bonding");
    iface.addSlave("ether1");
    iface.addSlave("ether2");

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getInterfaceType(), equalTo(InterfaceType.AGGREGATED));
    assertThat(
        viIface.getDependencies(),
        containsInAnyOrder(
            new Interface.Dependency("ether1", Interface.DependencyType.AGGREGATE),
            new Interface.Dependency("ether2", Interface.DependencyType.AGGREGATE)));
    assertThat(viIface.getChannelGroupMembers(), containsInAnyOrder("ether1", "ether2"));
  }

  @Test
  public void testToViInterfaceTunnelTypes() {
    MikrotikInterface gre = new MikrotikInterface("gre0", "gre");
    MikrotikInterface ipip = new MikrotikInterface("ipip0", "ipip");
    MikrotikInterface eoip = new MikrotikInterface("eoip0", "eoip");
    MikrotikInterface wireguard = new MikrotikInterface("wg0", "wireguard");

    assertThat(Conversions.toViInterface(gre).getInterfaceType(), equalTo(InterfaceType.TUNNEL));
    assertThat(Conversions.toViInterface(ipip).getInterfaceType(), equalTo(InterfaceType.TUNNEL));
    assertThat(Conversions.toViInterface(eoip).getInterfaceType(), equalTo(InterfaceType.TUNNEL));
    assertThat(
        Conversions.toViInterface(wireguard).getInterfaceType(), equalTo(InterfaceType.TUNNEL));
  }

  @Test
  public void testToViInterfaceVlanOverBondingBindDependency() {
    MikrotikInterface iface = new MikrotikInterface("vlan200-bond-wan", "vlan");
    iface.setVlanId(200);
    iface.setParentInterface("bond-core");

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getInterfaceType(), equalTo(InterfaceType.VLAN));
    assertThat(viIface.getVlan(), equalTo(200));
    assertThat(
        viIface.getDependencies(),
        contains(new Interface.Dependency("bond-core", Interface.DependencyType.BIND)));
  }

  @Test
  public void testToViInterfaceNoAddress() {
    MikrotikInterface iface = new MikrotikInterface("ether3", "ether");

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getAddress(), nullValue());
  }

  @Test
  public void testToViInterfaceDeterministic() {
    MikrotikInterface iface = new MikrotikInterface("loopback1", "loopback");
    ConcreteInterfaceAddress address = ConcreteInterfaceAddress.parse("198.51.100.1/32");
    iface.addAddress(address);

    Interface viIface1 = Conversions.toViInterface(iface);
    Interface viIface2 = Conversions.toViInterface(iface);
    Interface viIface3 = Conversions.toViInterface(iface);

    assertThat(viIface1.getName(), equalTo(viIface2.getName()));
    assertThat(viIface2.getName(), equalTo(viIface3.getName()));
    assertThat(viIface1.getInterfaceType(), equalTo(viIface2.getInterfaceType()));
    assertThat(viIface2.getInterfaceType(), equalTo(viIface3.getInterfaceType()));
    assertThat(viIface1.getAddress(), equalTo(viIface2.getAddress()));
    assertThat(viIface2.getAddress(), equalTo(viIface3.getAddress()));
    assertThat(viIface1.getAdminUp(), equalTo(viIface2.getAdminUp()));
    assertThat(viIface2.getAdminUp(), equalTo(viIface3.getAdminUp()));
  }

  @Test
  public void testToViStaticRouteBasic() {
    MikrotikStaticRoute route =
        new MikrotikStaticRoute(Prefix.parse("10.0.0.0/8"), Ip.parse("192.168.1.1"), 1);

    StaticRoute sr = Conversions.toViStaticRoute(route);

    assertThat(sr.getNetwork(), equalTo(Prefix.parse("10.0.0.0/8")));
    assertThat(sr.getNextHop(), equalTo(NextHopIp.of(Ip.parse("192.168.1.1"))));
    assertThat(sr.getAdministrativeCost(), equalTo(1L));
  }

  @Test
  public void testToViStaticRouteDefaultRoute() {
    MikrotikStaticRoute route =
        new MikrotikStaticRoute(Prefix.parse("0.0.0.0/0"), Ip.parse("10.0.0.1"), 1);

    StaticRoute sr = Conversions.toViStaticRoute(route);

    assertThat(sr.getNetwork(), equalTo(Prefix.ZERO));
  }

  @Test
  public void testToViStaticRouteDeterministic() {
    MikrotikStaticRoute route =
        new MikrotikStaticRoute(Prefix.parse("10.0.0.0/8"), Ip.parse("192.168.1.1"), 1);

    StaticRoute sr1 = Conversions.toViStaticRoute(route);
    StaticRoute sr2 = Conversions.toViStaticRoute(route);
    StaticRoute sr3 = Conversions.toViStaticRoute(route);

    assertThat(sr1.getNetwork(), equalTo(sr2.getNetwork()));
    assertThat(sr2.getNetwork(), equalTo(sr3.getNetwork()));
    assertThat(sr1.getNextHop(), equalTo(sr2.getNextHop()));
    assertThat(sr2.getNextHop(), equalTo(sr3.getNextHop()));
    assertThat(sr1.getAdministrativeCost(), equalTo(sr2.getAdministrativeCost()));
    assertThat(sr2.getAdministrativeCost(), equalTo(sr3.getAdministrativeCost()));
  }

  @Test
  public void testMaterializeViVrfsMapsMainAndPreservesOrder() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    vc.getOrCreateVrf("blue");
    vc.getOrCreateVrf("green");

    Map<String, Vrf> viVrfs = Conversions.materializeViVrfs(vc.getVrfs().values());

    assertThat(
        new ArrayList<>(viVrfs.keySet()),
        contains(Configuration.DEFAULT_VRF_NAME, "blue", "green"));
  }

  @Test
  public void testMaterializeVrfInterfacesAndStaticRoutes() {
    MikrotikConfiguration vc = new MikrotikConfiguration();
    MikrotikInterface mainIface = new MikrotikInterface("ether1", "ether");
    mainIface.addAddress(ConcreteInterfaceAddress.parse("192.0.2.1/24"));
    vc.getMainVrf().getInterfaces().put(mainIface.getName(), mainIface);
    MikrotikInterface ether2 = new MikrotikInterface("ether2", "ether");
    vc.getMainVrf().getInterfaces().put(ether2.getName(), ether2);
    MikrotikInterface bondCore = new MikrotikInterface("bond-core", "bonding");
    bondCore.addSlave("ether1");
    bondCore.addSlave("ether2");
    vc.getMainVrf().getInterfaces().put(bondCore.getName(), bondCore);
    vc.getMainVrf()
        .getStaticRoutes()
        .add(new MikrotikStaticRoute(Prefix.parse("0.0.0.0/0"), Ip.parse("192.0.2.254"), 10));

    MikrotikVrf blueVrf = vc.getOrCreateVrf("blue");
    MikrotikInterface blueIface = new MikrotikInterface("vlan25", "vlan");
    blueIface.setVlanId(25);
    blueVrf.getInterfaces().put(blueIface.getName(), blueIface);

    Configuration c = new Configuration("test", ConfigurationFormat.MIKROTIK);
    c.setVrfs(ImmutableMap.copyOf(Conversions.materializeViVrfs(vc.getVrfs().values())));

    Conversions.materializeVrfInterfacesAndStaticRoutes(c, vc.getVrfs().values());

    assertThat(c.getAllInterfaces().get("ether1").getOwner(), equalTo(c));
    assertThat(c.getAllInterfaces().get("ether1").getVrfName(), equalTo(Configuration.DEFAULT_VRF_NAME));
    assertThat(c.getAllInterfaces().get("vlan25").getVrfName(), equalTo("blue"));
    assertThat(
        c.getAllInterfaces().get("bond-core").getChannelGroupMembers(),
        containsInAnyOrder("ether1", "ether2"));
    assertThat(c.getAllInterfaces().get("ether1").getChannelGroup(), equalTo("bond-core"));
    assertThat(c.getAllInterfaces().get("ether2").getChannelGroup(), equalTo("bond-core"));
    assertThat(
        c.getVrfs().get(Configuration.DEFAULT_VRF_NAME).getStaticRoutes().first().getNetwork(),
        equalTo(Prefix.ZERO));
  }
}
