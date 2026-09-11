package dev.icaro.icarusfurnaces.listener;

import dev.icaro.icarusfurnaces.gui.FurnaceRecipeBookRegistry;
import dev.icaro.icarusfurnaces.gui.RecipeBookEntry;
import dev.icaro.icarusfurnaces.gui.RecipeBookGui;
import dev.icaro.icarusfurnaces.gui.RecipeBookHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Opens the read-only recipe book GUI (see {@code RecipeBookGui}) when a
 * player right-clicks while holding the Recipe Book item, and handles its
 * page-navigation clicks. Every other click inside it is cancelled — it's
 * for browsing, not storage.
 */
public final class RecipeBookListener implements Listener {

    private final FurnaceRecipeBookRegistry recipeBookRegistry;

    public RecipeBookListener(FurnaceRecipeBookRegistry recipeBookRegistry) {
        this.recipeBookRegistry = recipeBookRegistry;
    }

    @EventHandler
    public void onOpenRecipeBook(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        ItemStack inHand = event.getPlayer().getInventory().getItemInMainHand();
        if (!FurnaceRecipeBookRegistry.isBookItem(inHand)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().openInventory(RecipeBookGui.open(recipeBookRegistry.buildAll(), 0));
    }

    @EventHandler
    public void onRecipeBookClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RecipeBookHolder holder)) {
            return;
        }
        event.setCancelled(true); // read-only: nothing here can be taken, placed or moved
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Optional<String> direction = RecipeBookGui.navDirection(event.getCurrentItem());
        if (direction.isEmpty()) {
            return;
        }

        List<RecipeBookEntry> entries = recipeBookRegistry.buildAll();
        int newPage = holder.getPage() + ("next".equals(direction.get()) ? 1 : -1);
        if (newPage < 0 || newPage >= entries.size()) {
            return;
        }
        holder.setPage(newPage);
        RecipeBookGui.populate(event.getInventory(), entries, newPage);
    }
}
