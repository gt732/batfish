package org.batfish.vendor.mikrotik.representation;

import org.batfish.vendor.StructureType;

public enum MikrotikStructureType implements StructureType {
  INTERFACE("interface"),
  IP_ADDRESS("ip address");

  private final String _description;

  MikrotikStructureType(String description) {
    _description = description;
  }

  @Override
  public String getDescription() {
    return _description;
  }
}
