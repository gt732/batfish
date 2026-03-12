package org.batfish.vendor.mikrotik.grammar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.batfish.common.BatfishLogger;
import org.batfish.config.Settings;
import org.batfish.main.Batfish;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MikrotikGrammarTest {

  private static final String TESTCONFIGS_PREFIX =
      "org/batfish/vendor/mikrotik/grammar/testconfigs/";

  @Test
  public void testLexerTokenizesRouterOsSyntax() {
    String input =
        "/ip address add address=10.0.0.2/30 comment=\"L3 to Cisco core\" "
            + "servers=1.1.1.1,8.8.8.8\n";
    MikrotikLexer lexer = new MikrotikLexer(CharStreams.fromString(input));
    List<Integer> tokenTypes = lexer.getAllTokens().stream().map(Token::getType).toList();

    assertThat(tokenTypes, hasItem(MikrotikLexer.SLASH));
    assertThat(tokenTypes, hasItem(MikrotikLexer.EQUALS));
    assertThat(tokenTypes, hasItem(MikrotikLexer.IP_PREFIX));
    assertThat(tokenTypes, hasItem(MikrotikLexer.IP_ADDRESS));
    assertThat(tokenTypes, hasItem(MikrotikLexer.QUOTED_STRING));
  }

  @Test
  public void testParserParsesReferenceFixture() {
    String src = readResource(TESTCONFIGS_PREFIX + "mikrotik_interfaces_and_routes.export", UTF_8);
    Settings settings = new Settings();
    MikrotikCombinedParser parser = new MikrotikCombinedParser(src, settings);

    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);

    assertThat(parser, notNullValue());
    assertThat(tree, notNullValue());
  }
}
