package reign.software.hyforged.passive.model;

/**
 * Constants for passive node types.
 * <p>
 * Following project rules, we avoid enums in favor of string constants
 * that can be extended via JSON data.
 */
public final class PassiveNodeType {
    
    /** Small stat bonuses - majority of tree (~70%) */
    public static final String MINOR = "minor";
    
    /** Significant named bonuses (~20%) */
    public static final String NOTABLE = "notable";
    
    /** Build-defining nodes with upsides and downsides (~1-2%) */
    public static final String KEYSTONE = "keystone";
    
    /** Unlocks upon reaching a cluster; provides a choice */
    public static final String MASTERY = "mastery";
    
    /** Gates mechanics, abilities, or spells */
    public static final String UNLOCK = "unlock";
    
    private PassiveNodeType() {
        // Constants class
    }
    
    /**
     * Check if a type string is a valid node type.
     *
     * @param type The type to check
     * @return true if valid
     */
    public static boolean isValid(String type) {
        if (type == null) {
            return false;
        }
        return switch (type.toLowerCase()) {
            case MINOR, NOTABLE, KEYSTONE, MASTERY, UNLOCK -> true;
            default -> false;
        };
    }
}
