package reign.software.hyforged.passive.model;

/**
 * Constants for passive tree types.
 */
public final class PassiveTreeType {
    
    /** General passive tree - shared by all characters */
    public static final String GENERAL = "general";
    
    /** Class passive tree - specific to a class */
    public static final String CLASS = "class";
    
    private PassiveTreeType() {
        // Constants class
    }
    
    /**
     * Check if a type string is a valid tree type.
     *
     * @param type The type to check
     * @return true if valid
     */
    public static boolean isValid(String type) {
        if (type == null) {
            return false;
        }
        return switch (type.toLowerCase()) {
            case GENERAL, CLASS -> true;
            default -> false;
        };
    }
}
