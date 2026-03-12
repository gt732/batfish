package org.batfish.vendor.mikrotik.representation;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private @Nullable String _hostname;
  private final @Nonnull Map<String, MikrotikInterface> _interfaces;
  private final @Nonnull List<MikrotikStaticRoute> _staticRoutes;

  public MikrotikConfiguration() {
    _interfaces = new HashMap<>();
    _staticRoutes = new ArrayList<>();
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

    _interfaces.values().forEach(iface -> c.getAllInterfaces().put(iface.getName(), Conversions.toViInterface(iface)));

    return c;
  }
}
