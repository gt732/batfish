package org.batfish.vendor.mikrotik.representation;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Vendor-specific netwatch entry representation for MikroTik RouterOS. */
@ParametersAreNonnullByDefault
public final class MikrotikNetwatch implements Serializable {

  public MikrotikNetwatch(String id, String host) {
    _id = id;
    _host = host;
  }

  public @Nonnull String getId() {
    return _id;
  }

  public @Nonnull String getHost() {
    return _host;
  }

  public @Nullable String getComment() {
    return _comment;
  }

  public void setComment(@Nullable String comment) {
    _comment = comment;
  }

  public boolean isDisabled() {
    return _disabled;
  }

  public void setDisabled(boolean disabled) {
    _disabled = disabled;
  }

  public @Nullable String getInterval() {
    return _interval;
  }

  public void setInterval(@Nullable String interval) {
    _interval = interval;
  }

  public @Nullable String getTimeout() {
    return _timeout;
  }

  public void setTimeout(@Nullable String timeout) {
    _timeout = timeout;
  }

  public @Nullable String getUpScript() {
    return _upScript;
  }

  public void setUpScript(@Nullable String upScript) {
    _upScript = upScript;
  }

  public @Nullable String getDownScript() {
    return _downScript;
  }

  public void setDownScript(@Nullable String downScript) {
    _downScript = downScript;
  }

  private final @Nonnull String _id;
  private final @Nonnull String _host;
  private @Nullable String _comment;
  private boolean _disabled;
  private @Nullable String _interval;
  private @Nullable String _timeout;
  private @Nullable String _upScript;
  private @Nullable String _downScript;
}
