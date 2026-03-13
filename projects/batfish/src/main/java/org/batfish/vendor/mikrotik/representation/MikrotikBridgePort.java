package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Vendor-specific bridge port representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikBridgePort implements Serializable {

  public MikrotikBridgePort(String bridge, String interfaceName) {
    _bridge = bridge;
    _interface = interfaceName;
  }

  public @Nonnull String getBridge() {
    return _bridge;
  }

  public @Nonnull String getInterface() {
    return _interface;
  }

  public @Nullable Integer getPvid() {
    return _pvid;
  }

  public void setPvid(@Nullable Integer pvid) {
    _pvid = pvid;
  }

  private final @Nonnull String _bridge;
  private final @Nonnull String _interface;
  private @Nullable Integer _pvid;
}
