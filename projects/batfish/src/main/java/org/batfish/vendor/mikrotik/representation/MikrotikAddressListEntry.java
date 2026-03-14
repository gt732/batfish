package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.Prefix;

/** Vendor-specific firewall address-list entry representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikAddressListEntry implements Serializable {

  public MikrotikAddressListEntry(
      String rawAddress,
      @Nullable Prefix prefix,
      @Nullable Ip hostIp,
      @Nullable String comment,
      boolean disabled) {
    _rawAddress = rawAddress;
    _prefix = prefix;
    _hostIp = hostIp;
    _comment = comment;
    _disabled = disabled;
  }

  public @Nonnull String getRawAddress() {
    return _rawAddress;
  }

  public @Nullable Prefix getPrefix() {
    return _prefix;
  }

  public @Nullable Ip getHostIp() {
    return _hostIp;
  }

  public @Nullable String getComment() {
    return _comment;
  }

  public boolean isDisabled() {
    return _disabled;
  }

  public @Nonnull IpSpace toIpSpace() {
    if (_prefix != null) {
      return _prefix.toIpSpace();
    }
    assert _hostIp != null;
    return _hostIp.toIpSpace();
  }

  private final @Nonnull String _rawAddress;
  private final @Nullable Prefix _prefix;
  private final @Nullable Ip _hostIp;
  private final @Nullable String _comment;
  private final boolean _disabled;
}
