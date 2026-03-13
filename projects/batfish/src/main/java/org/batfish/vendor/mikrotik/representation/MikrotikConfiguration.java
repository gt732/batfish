package org.batfish.vendor.mikrotik.representation;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

  public static final @Nonnull String MAIN_VRF_NAME = "main";

  private @Nullable String _hostname;
  private final @Nonnull Map<String, MikrotikVrf> _vrfs;
  private final @Nonnull List<MikrotikBridgePort> _bridgePorts;
  private final @Nonnull List<MikrotikBridgeVlan> _bridgeVlans;

  public MikrotikConfiguration() {
    _vrfs = new LinkedHashMap<>();
    _bridgePorts = new ArrayList<>();
    _bridgeVlans = new ArrayList<>();
    getOrCreateVrf(MAIN_VRF_NAME);
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

  public @Nonnull Map<String, MikrotikVrf> getVrfs() {
    return _vrfs;
  }

  public @Nonnull MikrotikVrf getMainVrf() {
    return _vrfs.get(MAIN_VRF_NAME);
  }

  public @Nonnull MikrotikVrf getOrCreateVrf(String vrfName) {
    return _vrfs.computeIfAbsent(vrfName, MikrotikVrf::new);
  }

  public @Nonnull MikrotikVrf getVrfOrMain(@Nullable String vrfName) {
    if (vrfName == null) {
      return getMainVrf();
    }
    return firstNonNull(_vrfs.get(vrfName), getMainVrf());
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

    Map<String, Vrf> viVrfs = new LinkedHashMap<>();
    _vrfs.values()
        .forEach(
            mikrotikVrf -> {
              String vrfName =
                  mikrotikVrf.getName().equals(MAIN_VRF_NAME)
                      ? Configuration.DEFAULT_VRF_NAME
                      : mikrotikVrf.getName();
              viVrfs.put(vrfName, new Vrf(vrfName));
            });
    c.setVrfs(ImmutableMap.copyOf(viVrfs));

    _vrfs.values()
        .forEach(
            mikrotikVrf -> {
              String vrfName =
                  mikrotikVrf.getName().equals(MAIN_VRF_NAME)
                      ? Configuration.DEFAULT_VRF_NAME
                      : mikrotikVrf.getName();
              Vrf viVrf = c.getVrfs().get(vrfName);
              mikrotikVrf
                  .getInterfaces()
                  .values()
                  .forEach(
                      iface -> {
                        Interface viIface = Conversions.toViInterface(iface);
                        viIface.setOwner(c);
                        viIface.setVrf(viVrf);
                        c.getAllInterfaces().put(viIface.getName(), viIface);
                      });
              mikrotikVrf
                  .getStaticRoutes()
                  .forEach(sr -> viVrf.getStaticRoutes().add(Conversions.toViStaticRoute(sr)));
            });

    Set<String> bridgeNames =
        _vrfs.values().stream()
            .flatMap(vrf -> vrf.getInterfaces().values().stream())
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
