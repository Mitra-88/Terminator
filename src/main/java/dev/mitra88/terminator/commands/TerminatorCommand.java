package dev.mitra88.terminator.commands;

import dev.mitra88.terminator.builder.TerminatorBuilder;
import dev.mitra88.terminator.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;


public class TerminatorCommand implements CommandExecutor {

    public TerminatorCommand() {
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        ItemStack terminatorBow = TerminatorBuilder.giveTerminator();
        player.getInventory().addItem(terminatorBow);
        player.sendMessage(ColorUtils.color("&aYou have received the Terminator."));

        return true;
    }
}
