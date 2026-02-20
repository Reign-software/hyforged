package reign.software.hyforged.stats.damage;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nullable;

/**
 * Immutable data holder for Hyforged damage type extension properties.
 * <p>
 * This record represents the parsed and validated extension data for a damage type.
 * It is created from {@link DamageTypeExtensionAsset} and stored in
 * {@link DamageTypeExtensionRegistry}.
 *
 * @param damageTypeId      The Hytale DamageCause ID this extends
 * @param inherits          Optional parent damage type for stat inheritance
 * @param resistanceStat    The stat ID that provides resistance to this damage type
 * @param penetrationStat   The stat ID that provides penetration against resistance
 * @param elementTag        The element tag for ailment triggering (e.g., "fire", "ice")
 * @param damageBonusStat   Attacker stat that increases outgoing damage for this type
 * @param damageTakenStat   Defender stat that modifies incoming damage for this type
 * @param ailmentChanceStat Attacker stat for per-hit direct ailment trigger chance (BPS)
 * @param ailmentDurationStat Attacker stat for per-element ailment duration scaling (BPS)
 * @param ailmentDamageStat   Attacker stat for per-element ailment damage scaling (BPS)
 */
public record DamageTypeExtension(
        String damageTypeId,
        @Nullable String inherits,
        @Nullable StatId resistanceStat,
        @Nullable StatId penetrationStat,
        @Nullable String elementTag,
        @Nullable StatId damageBonusStat,
        @Nullable StatId damageTakenStat,
        @Nullable StatId ailmentChanceStat,
        @Nullable StatId ailmentDurationStat,
        @Nullable StatId ailmentDamageStat
) {
    /**
     * Create a DamageTypeExtension from an asset.
     *
     * @param asset The loaded asset data
     * @return A new DamageTypeExtension instance
     */
    public static DamageTypeExtension fromAsset(DamageTypeExtensionAsset asset) {
        return new DamageTypeExtension(
                asset.getId(),
                asset.getInherits(),
                asset.getResistanceStatId(),
                asset.getPenetrationStatId(),
                asset.getElementTag(),
                asset.getDamageBonusStatId(),
                asset.getDamageTakenStatId(),
                asset.getAilmentChanceStatId(),
                asset.getAilmentDurationStatId(),
                asset.getAilmentDamageStatId()
        );
    }
}
