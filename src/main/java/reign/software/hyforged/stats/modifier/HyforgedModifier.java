package reign.software.hyforged.stats.modifier;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Hyforged's ARPG-style modifier extending Hytale's native Modifier system.
 * <p>
 * This integrates with Hytale's EntityStatMap and can be used directly in item JSON
 * via the "Hyforged" modifier type.
 * <p>
 * Stacking semantics:
 * <ul>
 *   <li>FLAT - Added directly to base value</li>
 *   <li>INCREASED - Additive with other INCREASED, then multiplied (PoE-style)</li>
 *   <li>MORE - Multiplicative with each other (PoE-style)</li>
 *   <li>CAP - Enforces min/max bounds</li>
 * </ul>
 * <p>
 * Values use basis points: 10000 = 100%
 * <p>
 * Note: When used via Hytale's apply() method, this converts to float-based math.
 * For full ARPG stacking, use HyforgedStackingSystem which collects all modifiers
 * per stat and applies them in the correct order.
 */
public class HyforgedModifier extends Modifier {
    
    /** Basis points representing 100% */
    public static final int BPS_100_PERCENT = 10000;
    
    /** Sentinel value indicating no tag targeting */
    public static final int NO_TAG = Integer.MIN_VALUE;
    
    /**
     * Base codec for HyforgedModifier fields (without Type discriminator).
     * Used for JSON deserialization in item StatModifiers.
     * <p>
     * Note: TargetStatIndex, TargetTagIndex, and ExpirationTick are optional -
     * they use boxed types so null means "use default".
     */
    public static final BuilderCodec<HyforgedModifier> CODEC = BuilderCodec.builder(
            HyforgedModifier.class,
            HyforgedModifier::new,
            Modifier.BASE_CODEC
        )
        .append(
            new KeyedCodec<>("StackType", new EnumCodec<>(StackType.class)),
            (mod, value) -> mod.stackType = value,
            mod -> mod.stackType
        )
        .add()
        .append(
            new KeyedCodec<>("Amount", Codec.INTEGER),
            (mod, value) -> mod.amount = value,
            mod -> mod.amount
        )
        .add()
        .append(
            new KeyedCodec<>("SourceType", new EnumCodec<>(SourceType.class)),
            (mod, value) -> mod.sourceType = value,
            mod -> mod.sourceType
        )
        .add()
        .append(
            new KeyedCodec<>("SourceId", Codec.STRING),
            (mod, value) -> mod.sourceId = value,
            mod -> mod.sourceId
        )
        .add()
        .append(
            new KeyedCodec<>("Priority", Codec.INTEGER),
            (mod, value) -> mod.priority = value,
            mod -> mod.priority
        )
        .add()
        .append(
            new KeyedCodec<>("TargetStatIndex", Codec.INTEGER),
            (mod, value) -> { if (value != null) mod.targetStatIndex = value; },
            mod -> mod.targetStatIndex >= 0 ? mod.targetStatIndex : null
        )
        .add()
        .append(
            new KeyedCodec<>("TargetTagIndex", Codec.INTEGER),
            (mod, value) -> { if (value != null) mod.targetTagIndex = value; },
            mod -> mod.targetTagIndex != NO_TAG ? mod.targetTagIndex : null
        )
        .add()
        .append(
            new KeyedCodec<>("ExpirationTick", Codec.LONG),
            (mod, value) -> { if (value != null) mod.expirationTick = value; },
            mod -> mod.expirationTick > 0 ? mod.expirationTick : null
        )
        .add()
        .build();
    
    /**
     * Stacking behavior type for ARPG-style modifier computation.
     */
    public enum StackType {
        /**
         * Flat addition/subtraction. Applied first, all FLAT modifiers are summed.
         * Amount is the raw value to add.
         */
        FLAT(0),
        
        /**
         * Percentage increase (additive with other INCREASED).
         * Applied second, all INCREASED values are summed then applied as multiplier.
         * Amount is in basis points (100 = 1% increase).
         */
        INCREASED(1),
        
        /**
         * Percentage more (multiplicative with each other).
         * Applied third, each MORE modifier is applied sequentially.
         * Amount is in basis points (100 = 1% more).
         */
        MORE(2),
        
        /**
         * Cap/clamp enforcement. Applied last.
         * Positive amount = max cap, negative amount = min cap.
         */
        CAP(3);
        
        private final int order;
        
        StackType(int order) {
            this.order = order;
        }
        
        public int getOrder() {
            return order;
        }
    }
    
    /**
     * Source category for UI grouping and debugging.
     */
    public enum SourceType {
        BASE,
        ABILITY_SCORE,
        EQUIPMENT,
        BUFF,
        PASSIVE,
        CLASS,
        EFFECT,
        ADMIN
    }
    
    protected StackType stackType = StackType.FLAT;
    protected int amount = 0;
    protected SourceType sourceType = SourceType.EQUIPMENT;
    protected String sourceId = "";
    protected int priority = 0;
    
    /** Index of the target stat (-1 if targeting a tag) */
    protected int targetStatIndex = -1;
    
    /** Index of the target tag from AssetRegistry (NO_TAG if targeting a specific stat) */
    protected int targetTagIndex = NO_TAG;
    
    /** Game tick when this modifier expires (0 = permanent) */
    protected long expirationTick = 0;
    
    /**
     * Default constructor for codec deserialization.
     */
    public HyforgedModifier() {
        super();
    }
    
    /**
     * Full constructor for programmatic creation.
     */
    public HyforgedModifier(
            ModifierTarget target,
            StackType stackType,
            int amount,
            SourceType sourceType,
            String sourceId,
            int priority,
            int targetStatIndex,
            int targetTagIndex,
            long expirationTick
    ) {
        super(target);
        this.stackType = stackType;
        this.amount = amount;
        this.sourceType = sourceType;
        this.sourceId = sourceId != null ? sourceId : "";
        this.priority = priority;
        this.targetStatIndex = targetStatIndex;
        this.targetTagIndex = targetTagIndex;
        this.expirationTick = expirationTick;
    }
    
    /**
     * Legacy constructor for backwards compatibility.
     */
    public HyforgedModifier(
            ModifierTarget target,
            StackType stackType,
            int amount,
            SourceType sourceType,
            String sourceId,
            int priority
    ) {
        this(target, stackType, amount, sourceType, sourceId, priority, -1, NO_TAG, 0);
    }
    
    /**
     * Convenience constructor for simple modifiers.
     */
    public HyforgedModifier(StackType stackType, int amount) {
        this(ModifierTarget.MAX, stackType, amount, SourceType.EQUIPMENT, "", 0, -1, NO_TAG, 0);
    }
    
    // ============ Getters ============
    
    public StackType getStackType() {
        return stackType;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public SourceType getSourceType() {
        return sourceType;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public int getTargetStatIndex() {
        return targetStatIndex;
    }
    
    public int getTargetTagIndex() {
        return targetTagIndex;
    }
    
    public long getExpirationTick() {
        return expirationTick;
    }
    
    /**
     * Check if this modifier targets a tag (affects multiple stats).
     */
    public boolean isTagModifier() {
        return targetTagIndex != NO_TAG;
    }
    
    /**
     * Check if this modifier is expired.
     */
    public boolean isExpired(long currentTick) {
        return expirationTick > 0 && expirationTick <= currentTick;
    }
    
    /**
     * Check if this modifier is permanent (never expires).
     */
    public boolean isPermanent() {
        return expirationTick == 0;
    }
    
    // ============ Hytale Modifier Integration ============
    
    /**
     * Apply this modifier to a stat value.
     * <p>
     * Note: This is called by Hytale's EntityStatMap for individual modifier application.
     * For proper ARPG stacking (where FLAT is summed before INCREASED is applied),
     * use HyforgedStackingSystem which collects all modifiers first.
     * <p>
     * This method provides a reasonable approximation for single-modifier application:
     * - FLAT: Adds amount directly
    * - INCREASED: Multiplies by (1 + amount/10000)
    * - MORE: Multiplies by (1 + amount/10000)
     * - CAP: Clamps to amount (positive = max, negative = min)
     */
    @Override
    public float apply(float statValue) {
        return switch (stackType) {
            case FLAT -> statValue + amount;
            case INCREASED, MORE -> statValue * (1.0f + (float) amount / BPS_100_PERCENT);
            case CAP -> {
                if (amount >= 0) {
                    yield Math.min(statValue, amount);
                } else {
                    yield Math.max(statValue, -amount);
                }
            }
        };
    }
    
    /**
     * Convert to network packet for client sync.
     * <p>
     * Uses StaticModifier's packet format for client compatibility,
     * mapping our StackType to Hytale's CalculationType.
     */
    @Nonnull
    @Override
    public com.hypixel.hytale.protocol.Modifier toPacket() {
        com.hypixel.hytale.protocol.Modifier packet = new com.hypixel.hytale.protocol.Modifier();

        packet.target = switch (this.target) {
            case MIN -> com.hypixel.hytale.protocol.ModifierTarget.Min;
            case MAX -> com.hypixel.hytale.protocol.ModifierTarget.Max;
        };
        
        // Map our StackType to Hytale's CalculationType for client display
        packet.calculationType = switch (stackType) {
            case FLAT -> com.hypixel.hytale.protocol.CalculationType.Additive;
            case INCREASED, MORE, CAP -> com.hypixel.hytale.protocol.CalculationType.Multiplicative;
        };
        
        // Convert basis points to float for client
        packet.amount = switch (stackType) {
            case FLAT -> (float) amount;
            case INCREASED, MORE -> (float) amount / BPS_100_PERCENT;
            case CAP -> (float) amount;
        };
        
        return packet;
    }
    
    // ============ Object Methods ============
    
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        HyforgedModifier that = (HyforgedModifier) o;
        return amount == that.amount
            && priority == that.priority
            && targetStatIndex == that.targetStatIndex
            && targetTagIndex == that.targetTagIndex
            && expirationTick == that.expirationTick
            && stackType == that.stackType
            && sourceType == that.sourceType
            && sourceId.equals(that.sourceId);
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + stackType.hashCode();
        result = 31 * result + amount;
        result = 31 * result + sourceType.hashCode();
        result = 31 * result + sourceId.hashCode();
        result = 31 * result + priority;
        result = 31 * result + targetStatIndex;
        result = 31 * result + targetTagIndex;
        result = 31 * result + Long.hashCode(expirationTick);
        return result;
    }
    
    @Nonnull
    @Override
    public String toString() {
        return "HyforgedModifier{" +
            "stackType=" + stackType +
            ", amount=" + amount +
            ", sourceType=" + sourceType +
            ", sourceId='" + sourceId + "'" +
            ", priority=" + priority +
            ", targetStatIndex=" + targetStatIndex +
            ", targetTagIndex=" + targetTagIndex +
            ", expirationTick=" + expirationTick +
            "} " + super.toString();
    }
    
    // ============ Builder ============
    
    /**
     * Create a builder for constructing HyforgedModifier instances.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ModifierTarget target = ModifierTarget.MAX;
        private StackType stackType = StackType.FLAT;
        private int amount = 0;
        private SourceType sourceType = SourceType.EQUIPMENT;
        private String sourceId = "";
        private int priority = 0;
        private int targetStatIndex = -1;
        private int targetTagIndex = NO_TAG;
        private long expirationTick = 0;
        
        public Builder target(ModifierTarget target) {
            this.target = target;
            return this;
        }
        
        public Builder stackType(StackType stackType) {
            this.stackType = stackType;
            return this;
        }
        
        public Builder flat(int amount) {
            this.stackType = StackType.FLAT;
            this.amount = amount;
            return this;
        }
        
        public Builder increased(int basisPoints) {
            this.stackType = StackType.INCREASED;
            this.amount = basisPoints;
            return this;
        }
        
        public Builder more(int basisPoints) {
            this.stackType = StackType.MORE;
            this.amount = basisPoints;
            return this;
        }
        
        public Builder maxCap(int cap) {
            this.stackType = StackType.CAP;
            this.amount = cap;
            return this;
        }
        
        public Builder minCap(int cap) {
            this.stackType = StackType.CAP;
            this.amount = -cap;
            return this;
        }
        
        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder sourceType(SourceType sourceType) {
            this.sourceType = sourceType;
            return this;
        }
        
        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder targetStat(int statIndex) {
            this.targetStatIndex = statIndex;
            this.targetTagIndex = NO_TAG;
            return this;
        }
        
        public Builder targetTag(int tagIndex) {
            this.targetTagIndex = tagIndex;
            this.targetStatIndex = -1;
            return this;
        }
        
        public Builder expiresAt(long tick) {
            this.expirationTick = tick;
            return this;
        }
        
        public Builder permanent() {
            this.expirationTick = 0;
            return this;
        }
        
        public HyforgedModifier build() {
            return new HyforgedModifier(target, stackType, amount, sourceType, sourceId, priority,
                    targetStatIndex, targetTagIndex, expirationTick);
        }
    }
}
