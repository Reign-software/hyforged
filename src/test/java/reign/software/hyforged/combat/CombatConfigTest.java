package reign.software.hyforged.combat;

import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CombatConfig}.
 */
@DisplayName("CombatConfig Tests")
class CombatConfigTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOutput;

    @BeforeEach
    void setUp() {
        // Reset state before each test
        CombatConfig.setDebugEnabled(false);
        CombatConfig.setVerboseEnabled(false);

        // Capture stdout
        originalOut = System.out;
        capturedOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutput));
    }

    @AfterEach
    void tearDown() {
        // Restore stdout
        System.setOut(originalOut);

        // Reset state after tests
        CombatConfig.setDebugEnabled(false);
        CombatConfig.setVerboseEnabled(false);
    }

    @Nested
    @DisplayName("Debug Mode Tests")
    class DebugModeTests {

        @Test
        @DisplayName("Debug disabled by default")
        void debugDisabledByDefault() {
            assertFalse(CombatConfig.isDebugEnabled());
        }

        @Test
        @DisplayName("Can enable debug mode")
        void canEnableDebug() {
            CombatConfig.setDebugEnabled(true);
            assertTrue(CombatConfig.isDebugEnabled());
        }

        @Test
        @DisplayName("Can disable debug mode")
        void canDisableDebug() {
            CombatConfig.setDebugEnabled(true);
            CombatConfig.setDebugEnabled(false);
            assertFalse(CombatConfig.isDebugEnabled());
        }

        @Test
        @DisplayName("Debug message logged when enabled")
        void debugMessageLoggedWhenEnabled() {
            CombatConfig.setDebugEnabled(true);
            capturedOutput.reset(); // Clear the "enabled" message

            CombatConfig.debug("Test message");

            String output = capturedOutput.toString();
            assertTrue(output.contains("[COMBAT DEBUG]"));
            assertTrue(output.contains("Test message"));
        }

        @Test
        @DisplayName("Debug message not logged when disabled")
        void debugMessageNotLoggedWhenDisabled() {
            CombatConfig.debug("Test message");

            String output = capturedOutput.toString();
            assertFalse(output.contains("Test message"));
        }

        @Test
        @DisplayName("Debug format message works")
        void debugFormatMessageWorks() {
            CombatConfig.setDebugEnabled(true);
            capturedOutput.reset();

            CombatConfig.debug("Value: %d, Name: %s", 42, "test");

            String output = capturedOutput.toString();
            assertTrue(output.contains("Value: 42"));
            assertTrue(output.contains("Name: test"));
        }
    }

    @Nested
    @DisplayName("Verbose Mode Tests")
    class VerboseModeTests {

        @Test
        @DisplayName("Verbose disabled by default")
        void verboseDisabledByDefault() {
            assertFalse(CombatConfig.isVerboseEnabled());
        }

        @Test
        @DisplayName("Can enable verbose mode")
        void canEnableVerbose() {
            CombatConfig.setVerboseEnabled(true);
            assertTrue(CombatConfig.isVerboseEnabled());
        }

        @Test
        @DisplayName("Verbose message logged when enabled")
        void verboseMessageLoggedWhenEnabled() {
            CombatConfig.setDebugEnabled(true); // Required for logging
            CombatConfig.setVerboseEnabled(true);
            capturedOutput.reset();

            CombatConfig.verbose("Verbose test");

            String output = capturedOutput.toString();
            assertTrue(output.contains("[COMBAT VERBOSE]"));
            assertTrue(output.contains("Verbose test"));
        }

        @Test
        @DisplayName("Verbose message not logged when disabled")
        void verboseMessageNotLoggedWhenDisabled() {
            CombatConfig.verbose("Verbose test");

            String output = capturedOutput.toString();
            assertFalse(output.contains("Verbose test"));
        }
    }

    @Nested
    @DisplayName("Calculation Logging Tests")
    class CalculationLoggingTests {

        @BeforeEach
        void enableDebug() {
            CombatConfig.setDebugEnabled(true);
            capturedOutput.reset();
        }

        @Test
        @DisplayName("Log hit calculation - hit")
        void logHitCalcHit() {
            CombatConfig.logHitCalc(8000, 2000, 8500, 4000, true);

            String output = capturedOutput.toString();
            assertTrue(output.contains("HIT CHECK"));
            assertTrue(output.contains("HIT"));
        }

        @Test
        @DisplayName("Log hit calculation - miss")
        void logHitCalcMiss() {
            CombatConfig.logHitCalc(3000, 5000, 4500, 6000, false);

            String output = capturedOutput.toString();
            assertTrue(output.contains("HIT CHECK"));
            assertTrue(output.contains("MISS"));
        }

        @Test
        @DisplayName("Log block calculation")
        void logBlockCalc() {
            CombatConfig.logBlockCalc(5000, 80f, 15f, true);

            String output = capturedOutput.toString();
            assertTrue(output.contains("BLOCK CHECK"));
            assertTrue(output.contains("BLOCKED"));
        }

        @Test
        @DisplayName("Log damage calculation")
        void logDamageCalc() {
            CombatConfig.logDamageCalc("fire", 100f, 3000, 1000, 80f);

            String output = capturedOutput.toString();
            assertTrue(output.contains("DAMAGE"));
            assertTrue(output.contains("fire"));
        }

        @Test
        @DisplayName("Log crit calculation - crit")
        void logCritCalcCrit() {
            CombatConfig.logCritCalc(2500, 1000, true, 15000);

            String output = capturedOutput.toString();
            assertTrue(output.contains("CRIT CHECK"));
            assertTrue(output.contains("CRITICAL"));
        }

        @Test
        @DisplayName("Log crit calculation - no crit")
        void logCritCalcNoCrit() {
            CombatConfig.logCritCalc(1000, 5000, false, 0);

            String output = capturedOutput.toString();
            assertTrue(output.contains("CRIT CHECK"));
            assertTrue(output.contains("NO CRIT"));
        }

        @Test
        @DisplayName("Log heal calculation")
        void logHealCalc() {
            CombatConfig.logHealCalc(50f, 11000, 10500, 10200, 57.75f);

            String output = capturedOutput.toString();
            assertTrue(output.contains("HEAL"));
        }
    }

    @Nested
    @DisplayName("No Output When Disabled Tests")
    class NoOutputWhenDisabledTests {

        @Test
        @DisplayName("No hit log when disabled")
        void noHitLogWhenDisabled() {
            CombatConfig.logHitCalc(8000, 2000, 8500, 4000, true);
            assertTrue(capturedOutput.toString().isEmpty());
        }

        @Test
        @DisplayName("No block log when disabled")
        void noBlockLogWhenDisabled() {
            CombatConfig.logBlockCalc(5000, 80f, 15f, true);
            assertTrue(capturedOutput.toString().isEmpty());
        }

        @Test
        @DisplayName("No damage log when disabled")
        void noDamageLogWhenDisabled() {
            CombatConfig.logDamageCalc("fire", 100f, 3000, 1000, 80f);
            assertTrue(capturedOutput.toString().isEmpty());
        }

        @Test
        @DisplayName("No crit log when disabled")
        void noCritLogWhenDisabled() {
            CombatConfig.logCritCalc(2500, 1000, true, 15000);
            assertTrue(capturedOutput.toString().isEmpty());
        }

        @Test
        @DisplayName("No heal log when disabled")
        void noHealLogWhenDisabled() {
            CombatConfig.logHealCalc(50f, 11000, 10500, 10200, 57.75f);
            assertTrue(capturedOutput.toString().isEmpty());
        }
    }
}
