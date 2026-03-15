package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/** Vendor-specific firewall filter (chain) representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikFirewallFilter implements Serializable {

  public MikrotikFirewallFilter(String chain) {
    _chain = chain;
    _rules = new ArrayList<>();
  }

  public @Nonnull String getChain() {
    return _chain;
  }

  public @Nonnull List<MikrotikFirewallFilterRule> getRules() {
    return _rules;
  }

  public void addRule(MikrotikFirewallFilterRule rule) {
    _rules.add(rule);
  }

  private final @Nonnull String _chain;
  private final @Nonnull List<MikrotikFirewallFilterRule> _rules;
}
