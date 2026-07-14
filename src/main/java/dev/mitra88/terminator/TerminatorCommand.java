package dev.mitra88.terminator;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public final class TerminatorCommand implements BasicCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final TerminatorConfig config;
    private final boolean reload;

    public TerminatorCommand(TerminatorConfig config, boolean reload) {
        this.config = config;
        this.reload = reload;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (reload) {
            if (!sender.hasPermission("terminator.reload")) {
                sender.sendMessage(MM.deserialize("<red>You do not have permission to use this command."));
                return;
            }
            config.reload();
            sender.sendMessage(MM.deserialize("<green>Terminator config reloaded successfully."));
            return;
        }

        if (!sender.hasPermission("terminator.give")) {
            sender.sendMessage(MM.deserialize("<red>You do not have permission to use this command."));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>This command can only be run by a player."));
            return;
        }

        ItemStack bow = TerminatorBuilder.build(config);
        player.getInventory().addItem(bow).forEach((_, leftover) ->
                player.getWorld().dropItem(player.getLocation(), leftover));
        player.sendMessage(MM.deserialize("<green>You have received the Terminator."));
    }
}
