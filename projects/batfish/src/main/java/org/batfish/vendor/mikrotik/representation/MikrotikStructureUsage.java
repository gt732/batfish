package org.batfish.vendor.mikrotik.representation;

import org.batfish.vendor.StructureUsage;

public enum MikrotikStructureUsage implements StructureUsage {
  INTERFACE_SELF_REFERENCE("interface self-reference"),
  IP_ADDRESS_INTERFACE("ip address interface"),
  STATIC_ROUTE_SELF_REFERENCE("static route self-reference");

  private final String _description;

  MikrotikStructureUsage(String description) {
    _description = description;
  }

  @Override
  public String getDescription() {
    return _description;
  }
}
