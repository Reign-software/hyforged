package reign.software.hyforged.stats.asset;

import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.scaling.DiminishingScaling;
import reign.software.hyforged.stats.scaling.LinearScaling;
import reign.software.hyforged.stats.scaling.ScalingRule;
import reign.software.hyforged.stats.scaling.ThresholdScaling;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JSON asset representation of a scaling rule for deserialization.
 * <p>
 * Supports three scaling types:
 * <ul>
 *   <li><b>linear</b>: contribution = sourceValue * ratio</li>
 *   <li><b>threshold</b>: contribution = floor(sourceValue / perPoints) * bonusBps</li>
 *   <li><b>diminishing</b>: rating-to-effectiveness with cap</li>
 * </ul>
 * <p>
 * JSON Schema:
 * <pre>
 * // Linear scaling
 * {
 *   "Type": "linear",
 *   "Source": "hyforged:strength",
 *   "Ratio": 2.0
 * }
 * 
 * // Threshold scaling
 * {
 *   "Type": "threshold",
 *   "Source": "hyforged:luck",
 *   "PerPoints": 5,
 *   "BonusBps": 100
 * }
 * 
 * // Diminishing scaling
 * {
 *   "Type": "diminishing",
 *   "Source": "hyforged:crit-rating",
 *   "Curve": "rating",
 *   "Scale": 1.0,
 *   "CapBps": 7500
 * }
 * </pre>
 */
public class ScalingRuleAsset {
    
    private static final Logger LOGGER = Logger.getLogger(ScalingRuleAsset.class.getName());
    
    // Common fields
    private String type;
    private String source;
    
    // Linear scaling fields
    private Double ratio;
    
    // Threshold scaling fields
    private Integer perPoints;
    private Integer bonusBps;
    
    // Diminishing scaling fields
    private String curve;
    private Double scale;
    private Integer capBps;
    
    public ScalingRuleAsset() {
    }
    
    // ========== Setters (used by codec) ==========
    
    public void setType(String type) {
        this.type = type;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public void setRatio(Double ratio) {
        this.ratio = ratio;
    }
    
    public void setPerPoints(Integer perPoints) {
        this.perPoints = perPoints;
    }
    
    public void setBonusBps(Integer bonusBps) {
        this.bonusBps = bonusBps;
    }
    
    public void setCurve(String curve) {
        this.curve = curve;
    }
    
    public void setScale(Double scale) {
        this.scale = scale;
    }
    
    public void setCapBps(Integer capBps) {
        this.capBps = capBps;
    }
    
    // ========== Getters ==========
    
    @Nullable
    public String getType() {
        return type;
    }
    
    @Nullable
    public String getSource() {
        return source;
    }
    
    @Nullable
    public Double getRatio() {
        return ratio;
    }
    
    @Nullable
    public Integer getPerPoints() {
        return perPoints;
    }
    
    @Nullable
    public Integer getBonusBps() {
        return bonusBps;
    }
    
    @Nullable
    public String getCurve() {
        return curve;
    }
    
    @Nullable
    public Double getScale() {
        return scale;
    }
    
    @Nullable
    public Integer getCapBps() {
        return capBps;
    }
    
    // ========== Conversion ==========
    
    /**
     * Convert this asset to a ScalingRule.
     * <p>
     * Returns empty if the source stat is not registered or if required fields are missing.
     * 
     * @param contextStatId The ID of the stat this scaling rule belongs to (for error messages)
     * @return The ScalingRule, or empty if conversion failed
     */
    @Nonnull
    public Optional<ScalingRule> toScalingRule(@Nonnull String contextStatId) {
        Objects.requireNonNull(contextStatId, "contextStatId cannot be null");
        
        // Validate common required fields
        if (type == null || type.isEmpty()) {
            LOGGER.warning("Scaling rule for stat '" + contextStatId + "' has no Type - skipping");
            return Optional.empty();
        }
        
        if (source == null || source.isEmpty()) {
            LOGGER.warning("Scaling rule for stat '" + contextStatId + "' has no Source - skipping");
            return Optional.empty();
        }
        
        // Resolve source stat ID
        StatId sourceStatId;
        try {
            sourceStatId = StatId.parse(source);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid source stat ID '" + source + "' in scaling rule for stat '" + 
                    contextStatId + "': " + e.getMessage() + " - skipping");
            return Optional.empty();
        }
        
        // Note: Source stat existence is validated post-load by StatAssetLoader.validateScalingRules()
        // We don't check here because stats may be loaded in any order within the same batch
        
        // Convert based on type
        return switch (type.toLowerCase()) {
            case LinearScaling.TYPE -> toLinearScaling(sourceStatId, contextStatId);
            case ThresholdScaling.TYPE -> toThresholdScaling(sourceStatId, contextStatId);
            case DiminishingScaling.TYPE -> toDiminishingScaling(sourceStatId, contextStatId);
            default -> {
                LOGGER.warning("Unknown scaling rule type '" + type + "' for stat '" + contextStatId + "' - skipping");
                yield Optional.empty();
            }
        };
    }
    
    @Nonnull
    private Optional<ScalingRule> toLinearScaling(@Nonnull StatId sourceStatId, @Nonnull String contextStatId) {
        if (ratio == null) {
            LOGGER.warning("Linear scaling rule for stat '" + contextStatId + 
                    "' missing required field 'Ratio' - skipping");
            return Optional.empty();
        }
        
        try {
            return Optional.of(new LinearScaling(sourceStatId, ratio));
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid linear scaling rule for stat '" + contextStatId + 
                    "': " + e.getMessage() + " - skipping");
            return Optional.empty();
        }
    }
    
    @Nonnull
    private Optional<ScalingRule> toThresholdScaling(@Nonnull StatId sourceStatId, @Nonnull String contextStatId) {
        if (perPoints == null) {
            LOGGER.warning("Threshold scaling rule for stat '" + contextStatId + 
                    "' missing required field 'PerPoints' - skipping");
            return Optional.empty();
        }
        
        if (bonusBps == null) {
            LOGGER.warning("Threshold scaling rule for stat '" + contextStatId + 
                    "' missing required field 'BonusBps' - skipping");
            return Optional.empty();
        }
        
        try {
            return Optional.of(new ThresholdScaling(sourceStatId, perPoints, bonusBps));
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid threshold scaling rule for stat '" + contextStatId + 
                    "': " + e.getMessage() + " - skipping");
            return Optional.empty();
        }
    }
    
    @Nonnull
    private Optional<ScalingRule> toDiminishingScaling(@Nonnull StatId sourceStatId, @Nonnull String contextStatId) {
        if (capBps == null) {
            LOGGER.warning("Diminishing scaling rule for stat '" + contextStatId + 
                    "' missing required field 'CapBps' - skipping");
            return Optional.empty();
        }
        
        // Default values for optional fields
        String curveToUse = (curve != null && !curve.isEmpty()) ? curve : DiminishingScaling.DEFAULT_CURVE;
        double scaleToUse = (scale != null) ? scale : 1.0;
        
        try {
            return Optional.of(new DiminishingScaling(sourceStatId, curveToUse, scaleToUse, capBps));
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid diminishing scaling rule for stat '" + contextStatId + 
                    "': " + e.getMessage() + " - skipping");
            return Optional.empty();
        }
    }
    
    @Override
    public String toString() {
        return "ScalingRuleAsset[type=" + type + ", source=" + source + "]";
    }
}
