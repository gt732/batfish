package org.batfish.vendor.mikrotik.representation;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
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
    // Stub: real conversion implemented in later stories.
    return ImmutableList.of();
  }
}
