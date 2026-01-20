package reign.software.hyforged.progression.asset;

import reign.software.hyforged.progression.XPCurve;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for XP curve definitions.
 * <p>
 * Provides lookup of XP curves by ID and by type for progression calculations.
 * This is a singleton registry populated by the XPCurveAssetLoader.
 */
public final class XPCurveRegistry {

    private static final Logger LOGGER = Logger.getLogger(XPCurveRegistry.class.getName());
    
    private static final XPCurveRegistry INSTANCE = new XPCurveRegistry();
    
    /** Default character XP curve ID */
    public static final String CHARACTER_CURVE_ID = "hyforged:character_xp";
    
    /** Default class XP curve ID */
    public static final String CLASS_CURVE_ID = "hyforged:class_xp";
    
    private final Map<String, XPCurve> curves = new ConcurrentHashMap<>();
    
    // Cached default curves for fast access
    private volatile XPCurve characterCurve;
    private volatile XPCurve classCurve;

    private XPCurveRegistry() {
        // Singleton
    }

    /**
     * Get the singleton registry instance.
     *
     * @return The XP curve registry
     */
    @Nonnull
    public static XPCurveRegistry get() {
        return INSTANCE;
    }

    /**
     * Register an XP curve.
     *
     * @param curve The XP curve to register
     */
    public void register(@Nonnull XPCurve curve) {
        String id = curve.id();
        if (curves.containsKey(id)) {
            LOGGER.warning("Duplicate XP curve ID: " + id + " - overwriting");
        }
        curves.put(id, curve);
        
        // Cache default curves
        if (CHARACTER_CURVE_ID.equals(id)) {
            characterCurve = curve;
        } else if (CLASS_CURVE_ID.equals(id)) {
            classCurve = curve;
        }
        
        LOGGER.fine("Registered XP curve: " + id);
    }

    /**
     * Get an XP curve by ID.
     *
     * @param id The curve ID
     * @return The XP curve, or null if not found
     */
    @Nullable
    public XPCurve get(@Nonnull String id) {
        return curves.get(id);
    }

    /**
     * Get the character XP curve.
     * Returns a default curve if not configured.
     *
     * @return The character XP curve
     */
    @Nonnull
    public XPCurve getCharacterCurve() {
        XPCurve curve = characterCurve;
        if (curve == null) {
            curve = curves.get(CHARACTER_CURVE_ID);
        }
        if (curve == null) {
            // Return default curve
            LOGGER.warning("No character XP curve configured, using defaults");
            curve = new XPCurve(
                CHARACTER_CURVE_ID,
                XPCurve.CurveType.CHARACTER,
                XPCurve.DEFAULT_CHARACTER_BASE_XP,
                XPCurve.DEFAULT_EXPONENT_FACTOR,
                100
            );
        }
        return curve;
    }

    /**
     * Get the class XP curve.
     * Returns a default curve if not configured.
     *
     * @return The class XP curve
     */
    @Nonnull
    public XPCurve getClassCurve() {
        XPCurve curve = classCurve;
        if (curve == null) {
            curve = curves.get(CLASS_CURVE_ID);
        }
        if (curve == null) {
            // Return default curve
            LOGGER.warning("No class XP curve configured, using defaults");
            curve = new XPCurve(
                CLASS_CURVE_ID,
                XPCurve.CurveType.CLASS,
                XPCurve.DEFAULT_CLASS_BASE_XP,
                XPCurve.DEFAULT_EXPONENT_FACTOR,
                20
            );
        }
        return curve;
    }

    /**
     * Check if a curve is registered.
     *
     * @param id The curve ID
     * @return true if registered
     */
    public boolean contains(@Nonnull String id) {
        return curves.containsKey(id);
    }

    /**
     * Get the total number of registered curves.
     *
     * @return Number of curves
     */
    public int getCurveCount() {
        return curves.size();
    }

    /**
     * Clear all registered curves.
     * Primarily for testing.
     */
    public void clear() {
        curves.clear();
        characterCurve = null;
        classCurve = null;
    }
}
