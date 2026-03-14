package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;

/** Vendor-specific VRRP group representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikVrrpGroup implements Serializable {

  public MikrotikVrrpGroup(String name, String parentInterface, int vrid) {
    _name = name;
    _parentInterface = parentInterface;
    _vrid = vrid;
  }

  public @Nonnull String getName() {
    return _name;
  }

  public @Nonnull String getParentInterface() {
    return _parentInterface;
  }

  public int getVrid() {
    return _vrid;
  }

  public int getPriority() {
    return _priority;
  }

  public void setPriority(int priority) {
    _priority = priority;
  }

  public boolean isPreempt() {
    return _preempt;
  }

  public void setPreempt(boolean preempt) {
    _preempt = preempt;
  }

  public boolean isDisabled() {
    return _disabled;
  }

  public void setDisabled(boolean disabled) {
    _disabled = disabled;
  }

  public @Nullable String getVersion() {
    return _version;
  }

  public void setVersion(@Nullable String version) {
    _version = version;
  }

  public @Nullable String getV3Protocol() {
    return _v3Protocol;
  }

  public void setV3Protocol(@Nullable String v3Protocol) {
    _v3Protocol = v3Protocol;
  }

  public @Nullable Ip getVirtualAddress() {
    return _virtualAddress;
  }

  public void setVirtualAddress(@Nullable Ip virtualAddress) {
    _virtualAddress = virtualAddress;
  }

  private final @Nonnull String _name;
  private final @Nonnull String _parentInterface;
  private final int _vrid;
  private int _priority = 100;
  private boolean _preempt = true;
  private boolean _disabled;
  private @Nullable String _version;
  private @Nullable String _v3Protocol;
  private @Nullable Ip _virtualAddress;
}
