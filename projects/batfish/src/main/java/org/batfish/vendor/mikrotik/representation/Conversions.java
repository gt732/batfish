package org.batfish.vendor.mikrotik.representation;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.StaticRoute;
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

    List<ConcreteInterfaceAddress> addresses = iface.getAddresses();
    if (!addresses.isEmpty()) {
      builder.setAddress(addresses.get(0));
      if (addresses.size() > 1) {
        builder.setSecondaryAddresses(ImmutableSet.copyOf(addresses.subList(1, addresses.size())));
      }
    }
    return builder.build();
  }

  public static @Nonnull StaticRoute toViStaticRoute(MikrotikStaticRoute route) {
    return StaticRoute.builder()
        .setNetwork(route.getNetwork())
        .setNextHop(NextHopIp.of(route.getNextHopIp()))
        .setAdministrativeCost(route.getAdminDistance())
        .build();
  }
}
