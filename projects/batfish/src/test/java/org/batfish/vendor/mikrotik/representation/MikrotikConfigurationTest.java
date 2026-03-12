package org.batfish.vendor.mikrotik.representation;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
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
}
