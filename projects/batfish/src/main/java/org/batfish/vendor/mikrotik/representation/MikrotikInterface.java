package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.ConcreteInterfaceAddress;

/** Vendor-specific interface representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikInterface implements Serializable {

  public MikrotikInterface(String name, String type) {
    _name = name;
    _type = type;
    _addresses = new ArrayList<>();
    _slaves = new ArrayList<>();
  }

  public @Nonnull String getName() {
    return _name;
  }

  public @Nonnull String getType() {
    return _type;
  }

  public void setType(String type) {
    _type = type;
  }

  public @Nullable Integer getMtu() {
    return _mtu;
  }

  public void setMtu(@Nullable Integer mtu) {
    _mtu = mtu;
  }

  public boolean isDisabled() {
    return _disabled;
  }

  public void setDisabled(boolean disabled) {
    _disabled = disabled;
  }

  public @Nullable Integer getVlanId() {
    return _vlanId;
  }

  public void setVlanId(@Nullable Integer vlanId) {
    _vlanId = vlanId;
  }

  public @Nullable String getParentInterface() {
    return _parentInterface;
  }

  public void setParentInterface(@Nullable String parentInterface) {
    _parentInterface = parentInterface;
  }

  public @Nonnull List<ConcreteInterfaceAddress> getAddresses() {
    return _addresses;
  }

  public void addAddress(ConcreteInterfaceAddress address) {
    _addresses.add(address);
  }

  public @Nonnull List<String> getSlaves() {
    return _slaves;
  }

  public void addSlave(String slave) {
    _slaves.add(slave);
  }

  public @Nullable String getBondingMode() {
    return _bondingMode;
  }

  public void setBondingMode(@Nullable String bondingMode) {
    _bondingMode = bondingMode;
  }

  public @Nullable String getLacpRate() {
    return _lacpRate;
  }

  public void setLacpRate(@Nullable String lacpRate) {
    _lacpRate = lacpRate;
  }

  private final @Nonnull String _name;
  private @Nonnull String _type;
  private @Nullable Integer _mtu;
  private boolean _disabled;
  private @Nullable Integer _vlanId;
  private @Nullable String _parentInterface;
  private final @Nonnull List<ConcreteInterfaceAddress> _addresses;
  private final @Nonnull List<String> _slaves;
  private @Nullable String _bondingMode;
  private @Nullable String _lacpRate;
}
