package reign.software.hyforged.currency.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * HUD element displaying the player's Tradebar currency.
 * Shows inventory balance and vault balance (if vault exists).
 */
public class CurrencyHud extends CustomUIHud {

    public static final String UI_PATH = "Hyforged/CurrencyHud.ui";

    public CurrencyHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append(UI_PATH);
    }

    /**
     * Update the currency display values.
     *
     * @param inventoryBalance Tradebars in player's inventory
     * @param vaultBalance Tradebars in player's vault (0 if no vault)
     * @param hasVault Whether the player has a vault
     */
    public void updateValues(int inventoryBalance, int vaultBalance, boolean hasVault) {
        UICommandBuilder builder = new UICommandBuilder();
        
        // Format inventory balance with thousands separator
        builder.set("#InventoryBalance.Text", formatNumber(inventoryBalance));
        
        // Show vault section only if player has a vault
        builder.set("#VaultSection.Visible", hasVault);
        if (hasVault) {
            builder.set("#VaultBalance.Text", formatNumber(vaultBalance));
        }
        
        // Show total if vault exists
        if (hasVault) {
            int total = inventoryBalance + vaultBalance;
            builder.set("#TotalSection.Visible", true);
            builder.set("#TotalBalance.Text", formatNumber(total));
        } else {
            builder.set("#TotalSection.Visible", false);
        }
        
        update(false, builder);
    }

    @Nonnull
    private static String formatNumber(int value) {
        return String.format("%,d", value);
    }
}
