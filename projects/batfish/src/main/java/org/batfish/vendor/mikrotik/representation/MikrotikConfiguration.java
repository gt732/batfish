package org.batfish.vendor.mikrotik.representation;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Vrf;
import org.batfish.vendor.VendorConfiguration;

/** Vendor-specific representation of a MikroTik RouterOS configuration. */
@ParametersAreNonnullByDefault
public class MikrotikConfiguration extends VendorConfiguration {

  private @Nullable String _hostname;
  private final @Nonnull Map<String, MikrotikInterface> _interfaces;
  private final @Nonnull List<MikrotikStaticRoute> _staticRoutes;
  private final @Nonnull List<MikrotikBridgePort> _bridgePorts;
  private final @Nonnull List<MikrotikBridgeVlan> _bridgeVlans;

  public MikrotikConfiguration() {
    _interfaces = new HashMap<>();
    _staticRoutes = new ArrayList<>();
    _bridgePorts = new ArrayList<>();
    _bridgeVlans = new ArrayList<>();
  }

  @Override
  public @Nullable String getHostname() {
    return _hostname;
  }

  @Override
  public void setHostname(String hostname) {
    _hostname = hostname;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    // ignored; format is always MIKROTIK
  }

  public @Nonnull Map<String, MikrotikInterface> getInterfaces() {
    return _interfaces;
  }

  public @Nonnull List<MikrotikStaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  public @Nonnull List<MikrotikBridgePort> getBridgePorts() {
    return _bridgePorts;
  }

  public @Nonnull List<MikrotikBridgeVlan> getBridgeVlans() {
    return _bridgeVlans;
  }

  @Override
  public @Nonnull List<Configuration> toVendorIndependentConfigurations() {
    return ImmutableList.of(toVendorIndependentConfiguration());
  }

  private @Nonnull Configuration toVendorIndependentConfiguration() {
    String hostname =
        firstNonNull(_hostname, firstNonNull(getFilename(), "~batfish_mikrotik_no_hostname~"));
    Configuration c = new Configuration(hostname, ConfigurationFormat.MIKROTIK);
    c.setDefaultInboundAction(LineAction.PERMIT);
    c.setDefaultCrossZoneAction(LineAction.PERMIT);

    Vrf vrf = new Vrf(Configuration.DEFAULT_VRF_NAME);
    c.setVrfs(ImmutableMap.of(Configuration.DEFAULT_VRF_NAME, vrf));

    _interfaces.values()
        .forEach(
            iface -> {
              Interface viIface = Conversions.toViInterface(iface);
              viIface.setOwner(c);
              viIface.setVrf(vrf);
              c.getAllInterfaces().put(viIface.getName(), viIface);
            });
    for (MikrotikStaticRoute sr : _staticRoutes) {
      vrf.getStaticRoutes().add(Conversions.toViStaticRoute(sr));
    }
    Set<String> bridgeNames =
        _interfaces.values().stream()
            .filter(iface -> iface.getType().equalsIgnoreCase("bridge"))
            .map(MikrotikInterface::getName)
            .collect(ImmutableSet.toImmutableSet());
    Map<String, MikrotikBridgePort> bridgePortsByInterface =
        _bridgePorts.stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    MikrotikBridgePort::getInterface, Function.identity(), (existing, ignored) -> existing));
    Conversions.applyBridgeVlanSwitchports(
        _bridgeVlans, bridgePortsByInterface, c.getAllInterfaces(), bridgeNames);

    return c;
  }
}
