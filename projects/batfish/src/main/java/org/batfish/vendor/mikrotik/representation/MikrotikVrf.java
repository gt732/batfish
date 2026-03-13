package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/** Vendor-specific VRF representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikVrf implements Serializable {

  public MikrotikVrf(String name) {
    _name = name;
    _interfaces = new LinkedHashMap<>();
    _staticRoutes = new ArrayList<>();
  }

  public @Nonnull String getName() {
    return _name;
  }

  public @Nonnull Map<String, MikrotikInterface> getInterfaces() {
    return _interfaces;
  }

  public @Nonnull List<MikrotikStaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  private final @Nonnull String _name;
  private final @Nonnull Map<String, MikrotikInterface> _interfaces;
  private final @Nonnull List<MikrotikStaticRoute> _staticRoutes;
}
