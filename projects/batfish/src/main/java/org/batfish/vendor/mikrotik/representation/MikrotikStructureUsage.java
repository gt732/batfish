package org.batfish.vendor.mikrotik.representation;

import org.batfish.vendor.StructureUsage;

public enum MikrotikStructureUsage implements StructureUsage {
  INTERFACE_SELF_REFERENCE("interface self-reference"),
  IP_ADDRESS_INTERFACE("ip address interface"),
  BRIDGE_PORT_BRIDGE("bridge port bridge"),
  BRIDGE_PORT_INTERFACE("bridge port interface"),
  BRIDGE_VLAN_BRIDGE("bridge vlan bridge"),
  BRIDGE_VLAN_TAGGED_INTERFACE("bridge vlan tagged interface"),
  BRIDGE_VLAN_UNTAGGED_INTERFACE("bridge vlan untagged interface"),
  BONDING_SLAVE_INTERFACE("bonding slave interface"),
  VLAN_INTERFACE_PARENT("vlan interface parent"),
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
