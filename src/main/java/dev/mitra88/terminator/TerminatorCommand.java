package dev.mitra88.terminator;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public class TerminatorCommand implements BasicCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Terminator plugin;
    private final TerminatorConfig config;
    private final boolean isReload;

    public TerminatorCommand(Terminator plugin, TerminatorConfig config, boolean isReload) {
        this.plugin = plugin;
        this.config = config;
        this.isReload = isReload;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (isReload) {
            if (!sender.hasPermission("terminator.reload")) {
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>You do not have permission to use this command."));
                return;
            }
            config.reload(plugin);
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>Terminator config reloaded successfully."));
            return;
        }

        if (!sender.hasPermission("terminator.give")) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>You do not have permission to use this command."));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return;
        }

        ItemStack terminatorBow = TerminatorBuilder.giveTerminator(config);
        player.getInventory().addItem(terminatorBow).forEach((index, leftover) ->
                player.getWorld().dropItem(player.getLocation(), leftover)
        );
        player.sendMessage(MINI_MESSAGE.deserialize("<green>You have received the Terminator."));
    }
}