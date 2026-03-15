package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;

/** Vendor-specific firewall filter rule representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikFirewallFilterRule implements Serializable {

  public MikrotikFirewallFilterRule(
      String chain,
      LineAction action,
      @Nullable String srcAddressList,
      @Nullable String dstAddressList,
      @Nullable IpProtocol protocol,
      @Nullable Integer dstPort,
      @Nullable Integer srcPort) {
    _chain = chain;
    _action = action;
    _srcAddressList = srcAddressList;
    _dstAddressList = dstAddressList;
    _protocol = protocol;
    _dstPort = dstPort;
    _srcPort = srcPort;
  }

  public @Nonnull String getChain() {
    return _chain;
  }

  public @Nonnull LineAction getAction() {
    return _action;
  }

  public @Nullable String getSrcAddressList() {
    return _srcAddressList;
  }

  public @Nullable String getDstAddressList() {
    return _dstAddressList;
  }

  public @Nullable IpProtocol getProtocol() {
    return _protocol;
  }

  public @Nullable Integer getDstPort() {
    return _dstPort;
  }

  public @Nullable Integer getSrcPort() {
    return _srcPort;
  }

  private final @Nonnull String _chain;
  private final @Nonnull LineAction _action;
  private final @Nullable String _srcAddressList;
  private final @Nullable String _dstAddressList;
  private final @Nullable IpProtocol _protocol;
  private final @Nullable Integer _dstPort;
  private final @Nullable Integer _srcPort;
}
