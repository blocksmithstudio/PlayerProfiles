package com.muhammaddaffa.playerprofiles.commands.subcommands;

import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.commands.abstraction.SubCommand;
import com.muhammaddaffa.mdlib.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenProfileCommand extends SubCommand {

    @Override
    public @Nullable String getPermission() {
        return "playerprofiles.admin";
    }

    @Override
    public @NotNull List<String> parseTabCompletion(PlayerProfiles plugin, CommandSender sender, String[] args) {
        if(args.length == 2){
            return Collections.singletonList("(target-player)");
        }
        if(args.length == 3){
            return Collections.singletonList("(open-for)");
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(PlayerProfiles plugin, CommandSender sender, String[] args) {
        // The full command is /playerprofiles openprofile (target) (for-player) - args length = 3
        // So we want to tell the command sender if the the args doesn't enough
        if(args.length < 3){
            sender.sendMessage(Common.color("&cUsage: /playerprofiles openprofile (target) (for-player)"));
            return;
        }
        // Get the target as Player object
        Player target = Bukkit.getPlayer(args[1]);
        // Check if the target is not valid player
        if(target == null){
            // If the target is not valid player, we want to warn the command sender
            sender.sendMessage(Common.color(ConfigValue.INVALID_PLAYER
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", args[1])));
            return;
        }
        // After we check with the above method, we guaranteed the target isn't null
        // And now we want to get the player object
        Player player = Bukkit.getPlayer(args[2]);
        // Check if the player is not valid player
        if(player == null){
            // If the player is not valid player, we want to warn the command sender
            sender.sendMessage(Common.color(ConfigValue.INVALID_PLAYER
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", args[2])));
            return;
        }
        // Now we are guaranteed both player and target will not be null
        // Finally, we open the profile of target for player
        plugin.getInventoryManager().openInventory(null, player, target, true);
    }

}
