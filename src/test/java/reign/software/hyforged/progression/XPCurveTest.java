package reign.software.hyforged.progression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XP curve math and progression records.
 */
class XPCurveTest {

    private XPCurve characterCurve;
    private XPCurve classCurve;

    @BeforeEach
    void setUp() {
        // Character curve: base 100, exponent 1.15, max 100
        characterCurve = new XPCurve(
            "test:character",
            XPCurve.CurveType.CHARACTER,
            100,
            1.15,
            100
        );
        
        // Class curve: base 50, exponent 1.12, max 20
        classCurve = new XPCurve(
            "test:class",
            XPCurve.CurveType.CLASS,
            50,
            1.12,
            20
        );
    }

    @Nested
    @DisplayName("XP Curve Math")
    class XPCurveMath {

        @Test
        @DisplayName("Level 1 requires 0 XP")
        void level1RequiresZeroXP() {
            assertEquals(0, characterCurve.getXpForLevel(1));
            assertEquals(0, classCurve.getXpForLevel(1));
        }

        @Test
        @DisplayName("Level 2 requires base XP")
        void level2RequiresBaseXP() {
            assertEquals(100, characterCurve.getXpForLevel(2));
            assertEquals(50, classCurve.getXpForLevel(2));
        }

        @Test
        @DisplayName("XP scales exponentially")
        void xpScalesExponentially() {
            // Level 3: 100 * 1.15^1 = 115
            assertEquals(115, characterCurve.getXpForLevel(3));
            
            // Level 4: 100 * 1.15^2 = 132.25 -> 132
            assertEquals(132, characterCurve.getXpForLevel(4));
            
            // Level 5: 100 * 1.15^3 = 152.0875 -> 152
            assertEquals(152, characterCurve.getXpForLevel(5));
        }

        @Test
        @DisplayName("Total XP accumulates correctly")
        void totalXpAccumulatesCorrectly() {
            // Total for level 1: 0
            assertEquals(0, characterCurve.getTotalXpForLevel(1));
            
            // Total for level 2: 100
            assertEquals(100, characterCurve.getTotalXpForLevel(2));
            
            // Total for level 3: 100 + 115 = 215
            assertEquals(215, characterCurve.getTotalXpForLevel(3));
            
            // Total for level 4: 100 + 115 + 132 = 347
            assertEquals(347, characterCurve.getTotalXpForLevel(4));
        }

        @Test
        @DisplayName("Level calculation from total XP")
        void levelCalculationFromTotalXp() {
            // 0 XP = level 1
            assertEquals(1, characterCurve.getLevelForTotalXp(0));
            
            // 99 XP = level 1 (need 100 for level 2)
            assertEquals(1, characterCurve.getLevelForTotalXp(99));
            
            // 100 XP = level 2
            assertEquals(2, characterCurve.getLevelForTotalXp(100));
            
            // 214 XP = level 2 (need 215 for level 3)
            assertEquals(2, characterCurve.getLevelForTotalXp(214));
            
            // 215 XP = level 3
            assertEquals(3, characterCurve.getLevelForTotalXp(215));
            
            // 347 XP = level 4
            assertEquals(4, characterCurve.getLevelForTotalXp(347));
        }

        @Test
        @DisplayName("Remaining XP calculation")
        void remainingXpCalculation() {
            // 150 total = level 2 with 50 remaining (100 to reach level 2)
            assertEquals(50, characterCurve.getRemainingXp(150));
            
            // 300 total = level 3 with 85 remaining (215 to reach level 3)
            assertEquals(85, characterCurve.getRemainingXp(300));
        }

        @Test
        @DisplayName("Max level enforcement")
        void maxLevelEnforcement() {
            // Beyond max level returns MAX_VALUE
            assertEquals(Long.MAX_VALUE, characterCurve.getXpForLevel(101));
            assertEquals(Long.MAX_VALUE, characterCurve.getTotalXpForLevel(101));
            
            // Max level stops at configured max
            long hugeXp = Long.MAX_VALUE / 2;
            assertEquals(100, characterCurve.getLevelForTotalXp(hugeXp));
        }

        @Test
        @DisplayName("Multi-level gain from single XP award")
        void multiLevelGainFromSingleAward() {
            // Total XP for levels:
            // Level 2: 100
            // Level 3: 215 (100 + 115)
            // Level 4: 347 (100 + 115 + 132)
            // Level 5: 499 (100 + 115 + 132 + 152)
            // Level 6: 674 (100 + 115 + 132 + 152 + 175)
            
            // 500 XP should be level 5 (total for level 5 is 499)
            assertEquals(5, characterCurve.getLevelForTotalXp(500));
            // 600 XP should be level 5 (total for level 6 is 674)
            assertEquals(5, characterCurve.getLevelForTotalXp(600));
            // 700 XP should be level 6
            assertEquals(6, characterCurve.getLevelForTotalXp(700));
        }

        @Test
        @DisplayName("Negative XP handled gracefully")
        void negativeXpHandledGracefully() {
            assertEquals(1, characterCurve.getLevelForTotalXp(-100));
            assertEquals(0, characterCurve.getRemainingXp(-100));
        }
    }

    @Nested
    @DisplayName("Character Progression Record")
    class CharacterProgressionTests {

        @Test
        @DisplayName("Initial progression at level 1")
        void initialProgressionAtLevel1() {
            CharacterProgression prog = CharacterProgression.initial(100);
            
            assertEquals(1, prog.level());
            assertEquals(0, prog.currentXp());
            assertEquals(100, prog.xpToNext());
            assertEquals(0, prog.getGeneralPassivePoints());
            assertEquals(0.0, prog.getProgressPercent(), 0.001);
            assertFalse(prog.isMaxLevel());
        }

        @Test
        @DisplayName("General passive points = level - 1")
        void generalPassivePointsCalculation() {
            CharacterProgression level1 = new CharacterProgression(1, 0, 100);
            assertEquals(0, level1.getGeneralPassivePoints());
            
            CharacterProgression level50 = new CharacterProgression(50, 0, 1000);
            assertEquals(49, level50.getGeneralPassivePoints());
            
            CharacterProgression level100 = new CharacterProgression(100, 0, 0);
            assertEquals(99, level100.getGeneralPassivePoints());
        }

        @Test
        @DisplayName("Progress percent calculation")
        void progressPercentCalculation() {
            CharacterProgression half = new CharacterProgression(5, 50, 100);
            assertEquals(0.5, half.getProgressPercent(), 0.001);
            
            CharacterProgression quarter = new CharacterProgression(5, 25, 100);
            assertEquals(0.25, quarter.getProgressPercent(), 0.001);
        }

        @Test
        @DisplayName("Max level detection")
        void maxLevelDetection() {
            CharacterProgression notMax = new CharacterProgression(99, 0, 1000);
            assertFalse(notMax.isMaxLevel());
            
            CharacterProgression max = new CharacterProgression(100, 0, 0);
            assertTrue(max.isMaxLevel());
        }

        @Test
        @DisplayName("Level bounds enforcement")
        void levelBoundsEnforcement() {
            CharacterProgression belowMin = new CharacterProgression(-5, 0, 100);
            assertEquals(CharacterProgression.MIN_LEVEL, belowMin.level());
            
            CharacterProgression aboveMax = new CharacterProgression(150, 0, 100);
            assertEquals(CharacterProgression.MAX_LEVEL, aboveMax.level());
        }
    }

    @Nested
    @DisplayName("Class Progression Record")
    class ClassProgressionTests {

        @Test
        @DisplayName("Initial progression at level 1")
        void initialProgressionAtLevel1() {
            ClassProgression prog = ClassProgression.initial("hyforged:warrior", 50);
            
            assertEquals("hyforged:warrior", prog.classId());
            assertEquals(1, prog.level());
            assertEquals(0, prog.currentXp());
            assertEquals(50, prog.xpToNext());
            assertEquals(1, prog.getClassPassivePoints());
            assertEquals(0.0, prog.getProgressPercent(), 0.001);
            assertFalse(prog.isMaxLevel());
        }

        @Test
        @DisplayName("Class passive points = class level")
        void classPassivePointsCalculation() {
            ClassProgression level1 = new ClassProgression("test", 1, 0, 50);
            assertEquals(1, level1.getClassPassivePoints());
            
            ClassProgression level10 = new ClassProgression("test", 10, 0, 500);
            assertEquals(10, level10.getClassPassivePoints());
            
            ClassProgression level20 = new ClassProgression("test", 20, 0, 0);
            assertEquals(20, level20.getClassPassivePoints());
        }

        @Test
        @DisplayName("Max level is 20")
        void maxLevelIs20() {
            ClassProgression notMax = new ClassProgression("test", 19, 0, 1000);
            assertFalse(notMax.isMaxLevel());
            
            ClassProgression max = new ClassProgression("test", 20, 0, 0);
            assertTrue(max.isMaxLevel());
        }

        @Test
        @DisplayName("Level bounds enforcement")
        void levelBoundsEnforcement() {
            ClassProgression belowMin = new ClassProgression("test", -5, 0, 50);
            assertEquals(ClassProgression.MIN_LEVEL, belowMin.level());
            
            ClassProgression aboveMax = new ClassProgression("test", 50, 0, 50);
            assertEquals(ClassProgression.MAX_LEVEL, aboveMax.level());
        }
    }

    @Nested
    @DisplayName("XP Curve Types")
    class XPCurveTypes {

        @Test
        @DisplayName("Curve type parsing")
        void curveTypeParsing() {
            assertEquals(XPCurve.CurveType.CHARACTER, XPCurve.CurveType.fromString("character"));
            assertEquals(XPCurve.CurveType.CHARACTER, XPCurve.CurveType.fromString("CHARACTER"));
            assertEquals(XPCurve.CurveType.CLASS, XPCurve.CurveType.fromString("class"));
            assertEquals(XPCurve.CurveType.CLASS, XPCurve.CurveType.fromString("CLASS"));
            
            // Unknown defaults to CHARACTER
            assertEquals(XPCurve.CurveType.CHARACTER, XPCurve.CurveType.fromString("unknown"));
        }

        @Test
        @DisplayName("Curve type values")
        void curveTypeValues() {
            assertEquals("character", XPCurve.CurveType.CHARACTER.getValue());
            assertEquals("class", XPCurve.CurveType.CLASS.getValue());
        }
    }

    @Nested
    @DisplayName("Curve Validation")
    class CurveValidation {

        @Test
        @DisplayName("Invalid values are corrected")
        void invalidValuesAreCorrected() {
            XPCurve curve = new XPCurve(null, null, -10, 0.5, -5);
            
            assertEquals("unknown", curve.id());
            assertEquals(XPCurve.CurveType.CHARACTER, curve.type());
            assertEquals(1, curve.baseXp());
            assertEquals(1.0, curve.exponentFactor(), 0.001);
            assertEquals(1, curve.maxLevel());
        }
    }
}
