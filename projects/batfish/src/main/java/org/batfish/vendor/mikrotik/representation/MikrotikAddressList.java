package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/** Vendor-specific firewall address-list representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikAddressList implements Serializable {

  public MikrotikAddressList(String name) {
    _name = name;
    _entries = new ArrayList<>();
  }

  public @Nonnull String getName() {
    return _name;
  }

  public @Nonnull List<MikrotikAddressListEntry> getEntries() {
    return _entries;
  }

  public void addEntry(MikrotikAddressListEntry entry) {
    _entries.add(entry);
  }

  private final @Nonnull String _name;
  private final @Nonnull List<MikrotikAddressListEntry> _entries;
}
