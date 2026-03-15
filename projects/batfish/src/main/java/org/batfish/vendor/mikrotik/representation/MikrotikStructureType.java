package org.batfish.vendor.mikrotik.representation;

import org.batfish.vendor.StructureType;

public enum MikrotikStructureType implements StructureType {
  INTERFACE("interface"),
  FIREWALL_ADDRESS_LIST("firewall address-list"),
  FIREWALL_FILTER("firewall filter"),
  IP_ADDRESS("ip address"),
  STATIC_ROUTE("static route");

  private final String _description;

  MikrotikStructureType(String description) {
    _description = description;
  }

  @Override
  public String getDescription() {
    return _description;
  }
}
