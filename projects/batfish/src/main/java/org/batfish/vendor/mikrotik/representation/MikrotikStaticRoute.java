package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

/** Vendor-specific static route extracted from MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikStaticRoute implements Serializable {

  public MikrotikStaticRoute(Prefix network, Ip nextHopIp, int adminDistance) {
    _network = network;
    _nextHopIp = nextHopIp;
    _adminDistance = adminDistance;
  }

  public @Nonnull Prefix getNetwork() {
    return _network;
  }

  public @Nonnull Ip getNextHopIp() {
    return _nextHopIp;
  }

  public int getAdminDistance() {
    return _adminDistance;
  }

  private final @Nonnull Prefix _network;
  private final @Nonnull Ip _nextHopIp;
  private final int _adminDistance;
}
