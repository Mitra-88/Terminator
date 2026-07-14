package dev.mitra88.terminator;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public class TerminatorCommand implements BasicCommand {

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
        if (isReload) {
            if (!source.getSender().hasPermission("terminator.reload")) {
                source.getSender().sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission to use this command."));
                return;
            }

            config.reload(plugin);
            source.getSender().sendMessage(MiniMessage.miniMessage().deserialize("<green>Terminator config reloaded successfully."));
            return;
        }

        if (!source.getSender().hasPermission("terminator.give")) {
            source.getSender().sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission to use this command."));
            return;
        }

        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("This command can only be run by a player.");
            return;
        }

        ItemStack terminatorBow = TerminatorBuilder.giveTerminator(config);
        player.getInventory().addItem(terminatorBow);
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>You have received the Terminator."));
    }
}
