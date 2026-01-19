package reign.software.hyforged;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import reign.software.hyforged.commands.HyforgedCommand;
import reign.software.hyforged.events.HyforgedEvent;

import javax.annotation.Nonnull;

public class HyforgedPlugin extends JavaPlugin {

    public HyforgedPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new HyforgedCommand("hyforged", "A Hyforged command"));
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, HyforgedEvent::onPlayerReady);
    }
}
