package org.batfish.vendor.mikrotik.grammar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishLogger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.config.Settings;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.main.Batfish;
import org.batfish.vendor.VendorConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class MikrotikRecoveryTest {

  private static final String TESTCONFIGS_PREFIX =
      "org/batfish/vendor/mikrotik/grammar/testconfigs/";

  @Test
  public void testUnsupportedCommandsRecoverWithWarnings() {
    String src = readResource(TESTCONFIGS_PREFIX + "mikrotik_unsupported_commands.export", UTF_8);
    Settings settings = new Settings();

    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);
    Warnings warnings = new Warnings();
    MikrotikControlPlaneExtractor extractor =
        new MikrotikControlPlaneExtractor(src, parser, warnings, new SilentSyntaxCollection());
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(
        new NetworkSnapshot(new NetworkId("test-network"), new SnapshotId("test-snapshot")), tree);
    VendorConfiguration vc = extractor.getVendorConfiguration();

    assertThat(vc, notNullValue());
    assertThat(warnings.getParseWarnings().size(), greaterThan(0));
  }
}
