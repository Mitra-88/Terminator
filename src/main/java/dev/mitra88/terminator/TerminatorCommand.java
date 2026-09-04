package dev.mitra88.terminator;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Locale;

public final class TerminatorCommand implements BasicCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Component NO_PERMISSION = Component.text("You do not have permission to use this command.", NamedTextColor.RED);
    private static final Component PLAYERS_ONLY = Component.text("This command can only be run by a player.", NamedTextColor.RED);
    private static final Component CONFIG_RELOADED = Component.text("Terminator config reloaded successfully.", NamedTextColor.GREEN);
    private static final Component GIVEN_SELF = MM.deserialize("<green>You have received the Terminator.");

    private final TerminatorConfig config;
    private final TerminatorEventListener listener;
    private final boolean reload;

    public TerminatorCommand(TerminatorConfig config, TerminatorEventListener listener, boolean reload) {
        this.config = config;
        this.listener = listener;
        this.reload = reload;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        if (reload) {
            reloadConfig(source.getSender());
        } else {
            giveBow(source.getSender(), args);
        }
    }

    private void reloadConfig(CommandSender sender) {
        if (!sender.hasPermission("terminator.reload")) {
            sender.sendMessage(NO_PERMISSION);
            return;
        }
        config.reload();
        listener.cleanup();
        sender.sendMessage(CONFIG_RELOADED);
    }

    private void giveBow(CommandSender sender, String[] args) {
        if (!sender.hasPermission("terminator.give")) {
            sender.sendMessage(NO_PERMISSION);
            return;
        }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: ", NamedTextColor.RED).append(Component.text(args[0], NamedTextColor.YELLOW)));
                return;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(PLAYERS_ONLY);
            return;
        }

        ItemStack bow = TerminatorBuilder.build(config);
        target.getInventory().addItem(bow).values().forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));

        target.sendMessage(GIVEN_SELF);
        if (!target.equals(sender)) {
            sender.sendMessage(Component.text("You gave a Terminator to ", NamedTextColor.GREEN).append(Component.text(target.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        }
    }

    @Override
    public @NonNull List<String> suggest(@NonNull CommandSourceStack source, String @NonNull [] args) {
        if (reload || !source.getSender().hasPermission("terminator.give") || args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
