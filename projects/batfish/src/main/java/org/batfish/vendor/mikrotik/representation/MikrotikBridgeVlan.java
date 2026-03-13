package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/** Vendor-specific bridge VLAN representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikBridgeVlan implements Serializable {

  public MikrotikBridgeVlan(String bridge) {
    _bridge = bridge;
    _vlanIds = new LinkedHashSet<>();
    _tagged = new LinkedHashSet<>();
    _untagged = new LinkedHashSet<>();
  }

  public @Nonnull String getBridge() {
    return _bridge;
  }

  public @Nonnull Set<Integer> getVlanIds() {
    return _vlanIds;
  }

  public void addVlanId(int id) {
    _vlanIds.add(id);
  }

  public @Nonnull Set<String> getTagged() {
    return _tagged;
  }

  public void addTagged(String taggedPort) {
    _tagged.add(taggedPort);
  }

  public @Nonnull Set<String> getUntagged() {
    return _untagged;
  }

  public void addUntagged(String untaggedPort) {
    _untagged.add(untaggedPort);
  }

  private final @Nonnull String _bridge;
  private final @Nonnull Set<Integer> _vlanIds;
  private final @Nonnull Set<String> _tagged;
  private final @Nonnull Set<String> _untagged;
}
