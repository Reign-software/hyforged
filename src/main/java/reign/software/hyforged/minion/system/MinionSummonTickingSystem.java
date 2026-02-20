package reign.software.hyforged.minion.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.minion.MinionSummonService;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * ECS ticking system that delegates to {@link MinionSummonService#processTick(Store)}
 * each tick to drain spawn/despawn request queues and check duration timers.
 * <p>
 * All entity mutations happen on the world tick thread via this system.
 */
public class MinionSummonTickingSystem extends TickingSystem<EntityStore> {

    public MinionSummonTickingSystem() {
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public void tick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        MinionSummonService.get().processTick(store);
    }
}
