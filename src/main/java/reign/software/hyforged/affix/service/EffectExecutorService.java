package reign.software.hyforged.affix.service;

import reign.software.hyforged.affix.model.AffixTriggeredEffect;

import javax.annotation.Nonnull;

/**
 * Executes triggered affix effects by integrating with Hytale systems.
 */
public interface EffectExecutorService {

    /**
     * Execute the effect for a triggered affix.
     *
     * @return true if any effect action executed successfully.
     */
    boolean execute(@Nonnull AffixTriggeredEffect triggeredEffect, @Nonnull EffectContext context);
}
