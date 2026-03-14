package org.batfish.vendor.mikrotik.representation;

import org.batfish.vendor.StructureUsage;

public enum MikrotikStructureUsage implements StructureUsage {
  INTERFACE_SELF_REFERENCE("interface self-reference"),
  FIREWALL_ADDRESS_LIST_SELF_REFERENCE("firewall address-list self-reference"),
  FIREWALL_FILTER_SRC_ADDRESS_LIST("firewall filter src-address-list"),
  FIREWALL_FILTER_DST_ADDRESS_LIST("firewall filter dst-address-list"),
  FIREWALL_NAT_SRC_ADDRESS_LIST("firewall nat src-address-list"),
  FIREWALL_NAT_DST_ADDRESS_LIST("firewall nat dst-address-list"),
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
