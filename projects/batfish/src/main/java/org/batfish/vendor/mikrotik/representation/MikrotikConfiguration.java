package org.batfish.vendor.mikrotik.representation;

import com.google.common.collect.ImmutableList;
import java.util.List;
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

  @Override
  public @Nonnull List<Configuration> toVendorIndependentConfigurations() {
    // Stub: real conversion implemented in later stories.
    return ImmutableList.of();
  }
}
