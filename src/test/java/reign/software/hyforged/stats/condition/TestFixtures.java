package reign.software.hyforged.stats.condition;

/**
 * Test fixtures for condition tests.
 * <p>
 * These constants are for testing only. In production, status effects and weapon types
 * are loaded from JSON data files.
 */
public final class TestFixtures {
    
    private TestFixtures() {}
    
    /**
     * Sample status effect IDs for testing.
     */
    public static final class StatusEffects {
        public static final String BLEEDING = "hyforged:bleeding";
        public static final String POISONED = "hyforged:poisoned";
        public static final String BURNING = "hyforged:burning";
        public static final String FROZEN = "hyforged:frozen";
        public static final String STUNNED = "hyforged:stunned";
        
        private StatusEffects() {}
    }
    
    /**
     * Sample weapon type IDs for testing.
     */
    public static final class WeaponTypes {
        public static final String SWORD = "hyforged:sword";
        public static final String AXE = "hyforged:axe";
        public static final String BOW = "hyforged:bow";
        public static final String STAFF = "hyforged:staff";
        public static final String SHIELD = "hyforged:shield";
        
        private WeaponTypes() {}
    }
}
