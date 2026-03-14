package org.batfish.vendor.mikrotik.representation;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.route.nh.NextHopIp;

/** Utilities for converting Mikrotik-specific representations to VI models. */
@ParametersAreNonnullByDefault
public final class Conversions {

  private Conversions() {}

  public static @Nonnull InterfaceType toInterfaceType(String type) {
    if (type == null) {
      return InterfaceType.PHYSICAL;
    }
    switch (type.toLowerCase(Locale.ROOT)) {
      case "loopback":
        return InterfaceType.LOOPBACK;
      case "vlan":
        return InterfaceType.VLAN;
      case "bonding":
        return InterfaceType.AGGREGATED;
      case "gre":
      case "ipip":
      case "eoip":
      case "wireguard":
        return InterfaceType.TUNNEL;
      case "ether":
      case "ethernet":
      case "bridge":
      default:
        return InterfaceType.PHYSICAL;
    }
  }

  public static @Nonnull Interface toViInterface(MikrotikInterface iface) {
    Interface.Builder builder =
        Interface.builder()
            .setName(iface.getName())
            .setType(toInterfaceType(iface.getType()))
            .setAdminUp(!iface.isDisabled());

    Integer mtu = iface.getMtu();
    if (mtu != null) {
      builder.setMtu(mtu);
    }
    Double bandwidth = iface.getBandwidth();
    if (bandwidth != null) {
      builder.setBandwidth(bandwidth);
    }

    List<ConcreteInterfaceAddress> addresses = iface.getAddresses();
    if (!addresses.isEmpty()) {
      builder.setAddress(addresses.get(0));
      if (addresses.size() > 1) {
        builder.setSecondaryAddresses(ImmutableSet.copyOf(addresses.subList(1, addresses.size())));
      }
    }

    Integer vlanId = iface.getVlanId();
    if (vlanId != null) {
      builder.setVlan(vlanId);
      // MikroTik /interface vlan behaves as routed L3 subinterface, not switch SVI autostate.
      builder.setAutoState(false);
    }

    Interface viIface = builder.build();
    String parentInterface = iface.getParentInterface();
    if (parentInterface != null && !parentInterface.isEmpty()) {
      viIface.setDependencies(ImmutableSet.of(new Dependency(parentInterface, DependencyType.BIND)));
    } else if (!iface.getSlaves().isEmpty()) {
      viIface.setChannelGroupMembers(iface.getSlaves());
      viIface.setDependencies(
          iface.getSlaves().stream()
              .map(slave -> new Dependency(slave, DependencyType.AGGREGATE))
              .collect(ImmutableSet.toImmutableSet()));
    }
    return viIface;
  }

  public static @Nonnull StaticRoute toViStaticRoute(MikrotikStaticRoute route) {
    return StaticRoute.builder()
        .setNetwork(route.getNetwork())
        .setNextHop(NextHopIp.of(route.getNextHopIp()))
        .setAdministrativeCost(route.getAdminDistance())
        .build();
  }

  static @Nonnull Map<String, Vrf> materializeViVrfs(Iterable<MikrotikVrf> mikrotikVrfs) {
    Map<String, Vrf> viVrfs = new LinkedHashMap<>();
    for (MikrotikVrf mikrotikVrf : mikrotikVrfs) {
      String vrfName = toViVrfName(mikrotikVrf.getName());
      viVrfs.put(vrfName, new Vrf(vrfName));
    }
    return viVrfs;
  }

  static void materializeVrfInterfacesAndStaticRoutes(
      Configuration c, Iterable<MikrotikVrf> mikrotikVrfs) {
    for (MikrotikVrf mikrotikVrf : mikrotikVrfs) {
      Vrf viVrf = c.getVrfs().get(toViVrfName(mikrotikVrf.getName()));
      mikrotikVrf
          .getInterfaces()
          .values()
          .forEach(
              iface -> {
                Interface viIface = toViInterface(iface);
                viIface.setOwner(c);
                viIface.setVrf(viVrf);
                c.getAllInterfaces().put(viIface.getName(), viIface);
              });
      // Populate aggregate/member metadata used by interfaceProperties:
      // aggregate gets Channel_Group_Members, and each member points back via Channel_Group.
      mikrotikVrf
          .getInterfaces()
          .values()
          .forEach(
              iface -> {
                if (!iface.getType().equalsIgnoreCase("bonding")) {
                  return;
                }
                Interface aggregate = c.getAllInterfaces().get(iface.getName());
                if (aggregate == null) {
                  return;
                }
                List<String> presentSlaves =
                    iface.getSlaves().stream().filter(c.getAllInterfaces()::containsKey).toList();
                aggregate.setChannelGroupMembers(presentSlaves);
                presentSlaves.forEach(
                    slaveName -> c.getAllInterfaces().get(slaveName).setChannelGroup(iface.getName()));
              });
      mikrotikVrf
          .getStaticRoutes()
          .forEach(sr -> viVrf.getStaticRoutes().add(toViStaticRoute(sr)));
    }
  }

  static @Nonnull Set<String> collectBridgeNames(Iterable<MikrotikVrf> mikrotikVrfs) {
    return StreamSupport.stream(mikrotikVrfs.spliterator(), false)
        .flatMap(vrf -> vrf.getInterfaces().values().stream())
        .filter(iface -> iface.getType().equalsIgnoreCase("bridge"))
        .map(MikrotikInterface::getName)
        .collect(ImmutableSet.toImmutableSet());
  }

  static @Nonnull Map<String, MikrotikBridgePort> toBridgePortsByInterface(
      List<MikrotikBridgePort> bridgePorts) {
    Map<String, MikrotikBridgePort> bridgePortsByInterface = new LinkedHashMap<>();
    bridgePorts.forEach(
        bridgePort -> bridgePortsByInterface.putIfAbsent(bridgePort.getInterface(), bridgePort));
    return bridgePortsByInterface;
  }

  public static void applyBridgeVlanSwitchports(
      List<MikrotikBridgeVlan> bridgeVlans,
      Map<String, MikrotikBridgePort> bridgePortsByInterface,
      Map<String, Interface> viInterfaces,
      Set<String> bridgeNames) {
    Map<String, Set<Integer>> taggedVlans = new HashMap<>();
    Map<String, Set<Integer>> untaggedVlans = new HashMap<>();
    for (MikrotikBridgeVlan bridgeVlan : bridgeVlans) {
      for (int vlanId : bridgeVlan.getVlanIds()) {
        for (String port : bridgeVlan.getTagged()) {
          if (bridgeNames.contains(port)) {
            continue;
          }
          taggedVlans.computeIfAbsent(port, unused -> new HashSet<>()).add(vlanId);
        }
        for (String port : bridgeVlan.getUntagged()) {
          if (bridgeNames.contains(port)) {
            continue;
          }
          untaggedVlans.computeIfAbsent(port, unused -> new HashSet<>()).add(vlanId);
        }
      }
    }

    for (Map.Entry<String, Interface> entry : viInterfaces.entrySet()) {
      String portName = entry.getKey();
      Interface viIface = entry.getValue();
      Set<Integer> tagged = taggedVlans.getOrDefault(portName, ImmutableSet.of());
      Set<Integer> untagged = untaggedVlans.getOrDefault(portName, ImmutableSet.of());
      if (tagged.isEmpty() && untagged.isEmpty()) {
        continue;
      }
      viIface.setSwitchport(true);

      if (tagged.isEmpty()) {
        viIface.setSwitchportMode(SwitchportMode.ACCESS);
        viIface.setAccessVlan(untagged.iterator().next());
      } else {
        viIface.setSwitchportMode(SwitchportMode.TRUNK);
        Set<Integer> allVlans = new HashSet<>(tagged);
        allVlans.addAll(untagged);
        viIface.setAllowedVlans(
            allVlans.stream()
                .reduce(
                    IntegerSpace.EMPTY,
                    (acc, vlanId) -> acc.union(IntegerSpace.of(vlanId)),
                    (a, b) -> a.union(b)));
        Integer nativeVlan = null;
        MikrotikBridgePort bridgePort = bridgePortsByInterface.get(portName);
        if (bridgePort != null && bridgePort.getPvid() != null) {
          nativeVlan = bridgePort.getPvid();
        } else if (untagged.size() == 1) {
          nativeVlan = untagged.iterator().next();
        }
        if (nativeVlan != null) {
          viIface.setNativeVlan(nativeVlan);
        }
      }
    }
  }

  private static @Nonnull String toViVrfName(String vrfName) {
    return vrfName.equals(MikrotikConfiguration.MAIN_VRF_NAME)
        ? Configuration.DEFAULT_VRF_NAME
        : vrfName;
  }
}
