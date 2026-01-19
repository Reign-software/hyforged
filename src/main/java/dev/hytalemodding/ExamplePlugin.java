package dev.hytalemodding;

import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import reign.software.hyforged.HyforgedPlugin;

import javax.annotation.Nonnull;

/**
 * @deprecated Use {@link reign.software.hyforged.HyforgedPlugin} instead.
 */
@Deprecated
public class ExamplePlugin extends HyforgedPlugin {

    public ExamplePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }
}