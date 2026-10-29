package com.muhammaddaffa.playerprofiles.inventory;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import com.muhammaddaffa.playerprofiles.utils.ClickManager;
import com.muhammaddaffa.playerprofiles.utils.ItemManager;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProfileInventory extends FastInv {

    public ProfileInventory(List<GUIItem> items, Player player, Player target, int size, String title) {
        super(size, Utils.tryParsePAPI(Common.color(title), player, target));

        this.setAllItems(items, player, target);

        // If auto refresh is enabled
        if(ConfigValue.AUTO_REFRESH_ENABLED){
            // Start the auto refresh task
            HandleTask task = Executor.syncTimer(0L, ConfigValue.AUTO_REFRESH_TICK, () ->
                    this.setAllItems(items, player, target));
            // And remove the task after the inventory closed
            this.addCloseHandler(event -> task.cancel());
        }
        // If distance check is enabled
        if(ConfigValue.DISTANCE_CHECK_ENABLED){
            // Start the check distance task
            HandleTask task = Executor.syncTimer(0L, 20L, () ->
                    checkDistance(player, target));
            // And remove the task after the inventory closed
            this.addCloseHandler(event -> task.cancel());
        }

    }

    private void setAllItems(List<GUIItem> items, Player player, Player target) {
        List<GUIItem> clonedItem = new ArrayList<>(items);
        clonedItem.sort(Comparator.comparingInt(GUIItem::priority));

        // Loop through all items
        clonedItem.forEach(item -> {
            // Get the item stack
            ItemStack stack = ItemManager.createGUIItem(item, player, target);
            // Check if the item use permission to see
            if(item.usePermission()){
                // If the player doesn't have the permission, don't show it
                if(!player.hasPermission(item.permission())) {
                    return;
                }
            }
            // Check if the item is set to only visitor
            if(item.onlyVisitor()){
                // If the player and the target is the same, don't show the item
                if(player.equals(target)) {
                    return;
                }
            }
            // Check if the item is set to only owner
            if(item.onlyOwner()){
                // If the player and the target is not the same, don't show the item
                if(!player.equals(target)) {
                    return;
                }
            }
            // Now, set the item to the inventory
            this.setItems(item.slots(), stack, event -> {
                // Set the click event for the item
                ClickManager.handleInventoryClick(item, player, target, event);
            });
        });
        // After all items being set, we finally set the fill items
        ItemManager.fillItem(this, PlayerProfiles.GUI_DEFAULT.getConfig());
    }

    private void checkDistance(Player player, Player target){
        // If the target is offline, just close the inventory
        if(target == null){
            // Close the inventory
            player.closeInventory();
            // Send a message to the player
            player.sendMessage(Common.color(ConfigValue.DISTANCE_TOO_FAR
                    .replace("{prefix}", ConfigValue.PREFIX))
                    .replace("{player}", target.getName()));
            return;
        }
        if(!player.getWorld().equals(target.getWorld())){
            // Close the inventory
            player.closeInventory();
            // Send a message to the player
            player.sendMessage(Common.color(ConfigValue.DISTANCE_TOO_FAR
                    .replace("{prefix}", ConfigValue.PREFIX))
                    .replace("{player}", target.getName()));
            return;
        }
        // Get the distance between the player and the target
        double distance = player.getLocation().distance(target.getLocation());
        // Check if the distance is greater than the configured maximum distance
        if(distance > ConfigValue.MAXIMUM_DISTANCE){
            // Close the inventory
            player.closeInventory();
            // Send a message to the player
            player.sendMessage(Common.color(ConfigValue.DISTANCE_TOO_FAR
                    .replace("{prefix}", ConfigValue.PREFIX))
                    .replace("{player}", target.getName()));
        }
    }
}
