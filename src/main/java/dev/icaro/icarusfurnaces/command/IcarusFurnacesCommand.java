package dev.icaro.icarusfurnaces.command;

import dev.icaro.icarusfurnaces.IcarusFurnacesPlugin;
import dev.icaro.icarusfurnaces.gui.FurnaceRecipeBookRegistry;
import dev.icaro.icarusfurnaces.gui.RecipeBookIndexGui;
import dev.icaro.icarusfurnaces.tier.FurnaceTier;
import dev.icaro.icarusfurnaces.tier.FurnaceTierService;
import dev.icaro.icarusfurnaces.upgrade.FurnaceKitRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Root command for IcarusFurnaces: {@code ping} (health check), {@code info}
 * (debug: reports the tier of the furnace the player is looking at), {@code
 * recipebook} (opens the Recipe Book menu directly — see {@code
 * RecipeBookIndexGui}), and the admin-only {@code give}/{@code reload}.
 */
public final class IcarusFurnacesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("ping", "info", "give", "reload", "recipebook");
    private static final int INFO_MAX_DISTANCE = 6;
    private static final String ADMIN_PERMISSION = "icarusfurnaces.admin";
    private static final String INFO_PERMISSION = "icarusfurnaces.info";
    private static final String RECIPE_BOOK_PERMISSION = "icarusfurnaces.recipebook";

    private final IcarusFurnacesPlugin plugin;
    private final FurnaceKitRegistry furnaceKitRegistry;
    private final FurnaceRecipeBookRegistry recipeBookRegistry;

    public IcarusFurnacesCommand(IcarusFurnacesPlugin plugin, FurnaceKitRegistry furnaceKitRegistry, FurnaceRecipeBookRegistry recipeBookRegistry) {
        this.plugin = plugin;
        this.furnaceKitRegistry = furnaceKitRegistry;
        this.recipeBookRegistry = recipeBookRegistry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("ping")) {
            sender.sendMessage(Component.text("IcarusFurnaces v" + plugin.getPluginMeta().getVersion()
                    + " está online.", NamedTextColor.GOLD));
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(sender);
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            case "recipebook" -> handleRecipeBook(sender, args);
            default -> {
                sender.sendMessage(Component.text("Subcomando desconhecido: " + args[0], NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission(INFO_PERMISSION)) {
            sender.sendMessage(Component.text("Você não tem permissão para isso.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando só pode ser usado por um jogador.", NamedTextColor.RED));
            return true;
        }

        Block target = player.getTargetBlockExact(INFO_MAX_DISTANCE);
        if (target == null || target.getType() != Material.FURNACE) {
            sender.sendMessage(Component.text("Nenhuma fornalha mirada a até " + INFO_MAX_DISTANCE + " blocos.",
                    NamedTextColor.RED));
            return true;
        }

        Location loc = target.getLocation();
        Optional<FurnaceTier> tier = FurnaceTierService.tierOf(target);
        sender.sendMessage(Component.text("Fornalha em (" + loc.getBlockX() + ", " + loc.getBlockY() + ", "
                + loc.getBlockZ() + ")", NamedTextColor.GOLD));
        if (tier.isPresent()) {
            sender.sendMessage(Component.text("  Tier: " + tier.get().displayName()
                    + " (" + tier.get().cookTicks() + " ticks por item configurado)", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("  Tier: fornalha comum (nunca evoluida, 200 ticks por item)", NamedTextColor.YELLOW));
        }

        // Números ao vivo do bloco, não o que devia ser — se "Cozimento total" nunca bater com o
        // tier acima, é sinal de que o FurnaceStartSmeltEvent não está pegando o valor certo nessa
        // fornalha; se bater mas "Progresso" ficar parado com "Queima" > 0, o problema é outro,
        // fora do que este plugin controla.
        if (target.getState() instanceof Furnace furnaceState) {
            sender.sendMessage(Component.text("  Progresso: " + furnaceState.getCookTime() + " / "
                    + furnaceState.getCookTimeTotal() + " ticks | Queima restante: " + furnaceState.getBurnTime() + " ticks",
                    NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(Component.text("Você não tem permissão para isso.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /icarusfurnaces give <tier> [jogador]", NamedTextColor.RED));
            return true;
        }

        Optional<FurnaceTier> tier = parseTier(args[1]);
        if (tier.isEmpty()) {
            sender.sendMessage(Component.text("Tier inválido: " + args[1], NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Jogador offline ou inexistente: " + args[2], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("Especifique um jogador ao usar este comando pelo console.",
                    NamedTextColor.RED));
            return true;
        }

        target.getInventory().addItem(furnaceKitRegistry.createKit(tier.get()));
        sender.sendMessage(Component.text("Kit de upgrade (" + tier.get().displayName() + ") entregue a "
                + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleRecipeBook(CommandSender sender, String[] args) {
        if (!sender.hasPermission(RECIPE_BOOK_PERMISSION)) {
            sender.sendMessage(Component.text("Você não tem permissão para isso.", NamedTextColor.RED));
            return true;
        }

        Optional<Player> target = resolveTarget(sender, args, 1);
        if (target.isEmpty()) {
            return true; // resolveTarget already messaged the sender
        }

        target.get().openInventory(RecipeBookIndexGui.open(recipeBookRegistry.buildAll()));
        sender.sendMessage(Component.text("Livro de Receitas aberto para " + target.get().getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    /** {@code args[argIndex]} if present, else {@code sender} itself (must be a player). Messages the sender and returns empty on failure. */
    private Optional<Player> resolveTarget(CommandSender sender, String[] args, int argIndex) {
        if (args.length > argIndex) {
            Player target = plugin.getServer().getPlayerExact(args[argIndex]);
            if (target == null) {
                sender.sendMessage(Component.text("Jogador offline ou inexistente: " + args[argIndex], NamedTextColor.RED));
                return Optional.empty();
            }
            return Optional.of(target);
        }
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        sender.sendMessage(Component.text("Especifique um jogador ao usar este comando pelo console.", NamedTextColor.RED));
        return Optional.empty();
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(Component.text("Você não tem permissão para isso.", NamedTextColor.RED));
            return true;
        }
        plugin.reloadPluginConfig();
        sender.sendMessage(Component.text("Configuração recarregada.", NamedTextColor.GREEN));
        return true;
    }

    private Optional<FurnaceTier> parseTier(String raw) {
        for (FurnaceTier tier : FurnaceTier.values()) {
            if (tier.name().equalsIgnoreCase(raw) || tier.displayName().equalsIgnoreCase(raw)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> tiers = new ArrayList<>();
            for (FurnaceTier tier : FurnaceTier.values()) {
                tiers.add(tier.name());
            }
            return tiers;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("recipebook")) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
