package org.batfish.vendor.mikrotik.grammar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.batfish.config.Settings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Smoke tests for {@link MikrotikCombinedParser} scaffolding.
 *
 * <p>Verifies that the parser stub is instantiable and produces a non-null parse tree. Deeper
 * grammar and extraction tests belong to Story 1.2+.
 */
@RunWith(JUnit4.class)
public class MikrotikGrammarTest {

  /** Verify that the combined parser can be instantiated and parse a minimal input. */
  @Test
  public void testParserInstantiates() {
    String minimalInput = "# mar/11/2026 12:00:00 by RouterOS 6.49.6\n/system identity\n";
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(minimalInput, settings);
    assertThat(parser, notNullValue());
    assertThat(parser.parse(), notNullValue());
  }
}
