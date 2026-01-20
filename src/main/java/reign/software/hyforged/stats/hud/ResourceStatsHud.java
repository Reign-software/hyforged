package reign.software.hyforged.stats.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class ResourceStatsHud extends CustomUIHud {

    public static final String UI_PATH = "Hyforged/ResourceStatsHud.ui";
    private static final int BAR_WIDTH = 160;

    public ResourceStatsHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append(UI_PATH);
    }

    public void updateValues(
            boolean showConcentration,
            int concentrationCurrent,
            int concentrationMax,
            boolean showRage,
            int rageCurrent,
            int rageMax
    ) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ConcentrationContainer.Visible", showConcentration);
        builder.set("#RageContainer.Visible", showRage);
        builder.set("#ConcentrationValue.Text", concentrationCurrent + "/" + concentrationMax);
        builder.set("#RageValue.Text", rageCurrent + "/" + rageMax);
        builder.set("#ConcentrationFill.Width", computeFillWidth(concentrationCurrent, concentrationMax));
        builder.set("#RageFill.Width", computeFillWidth(rageCurrent, rageMax));
        update(false, builder);
    }

    private int computeFillWidth(int current, int max) {
        if (max <= 0) {
            return 0;
        }
        float ratio = Math.max(0.0f, Math.min(1.0f, (float) current / (float) max));
        return Math.round(BAR_WIDTH * ratio);
    }
}
