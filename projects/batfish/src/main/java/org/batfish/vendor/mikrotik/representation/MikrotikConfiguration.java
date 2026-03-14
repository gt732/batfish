package org.batfish.vendor.mikrotik.representation;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
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
  private final @Nonnull Map<String, MikrotikNetwatch> _netwatch;
  private final @Nonnull Map<String, MikrotikVrrpGroup> _vrrpGroups;

  public MikrotikConfiguration() {
    _vrfs = new LinkedHashMap<>();
    _bridgePorts = new ArrayList<>();
    _bridgeVlans = new ArrayList<>();
    _netwatch = new LinkedHashMap<>();
    _vrrpGroups = new LinkedHashMap<>();
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

  public @Nonnull Map<String, MikrotikNetwatch> getNetwatch() {
    return _netwatch;
  }

  public @Nonnull Map<String, MikrotikVrrpGroup> getVrrpGroups() {
    return _vrrpGroups;
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

    Map<String, Vrf> viVrfs = Conversions.materializeViVrfs(_vrfs.values());
    c.setVrfs(ImmutableMap.copyOf(viVrfs));
    Conversions.materializeVrfInterfacesAndStaticRoutes(c, _vrfs.values());
    Conversions.applyVrrpGroups(_vrrpGroups, c.getAllInterfaces(), getWarnings());

    Set<String> bridgeNames = Conversions.collectBridgeNames(_vrfs.values());
    Map<String, MikrotikBridgePort> bridgePortsByInterface =
        Conversions.toBridgePortsByInterface(_bridgePorts);
    Conversions.applyBridgeVlanSwitchports(
        _bridgeVlans, bridgePortsByInterface, c.getAllInterfaces(), bridgeNames);

    return c;
  }
}
