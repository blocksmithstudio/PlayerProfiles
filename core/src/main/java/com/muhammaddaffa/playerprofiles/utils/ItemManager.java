package com.muhammaddaffa.playerprofiles.utils;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.xseries.XMaterial;
import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class ItemManager {

    public static ItemStack createItem(GUIItem item, Player player, Player target){
        ItemBuilder builder = null;
        // Check if the item material contains ';'
        if(item.material().contains(";")){
            // That means this item is a base64 head item
            // First, we split the ';' to get the head value
            String[] split = item.material().split(";");
            String identifier = split[0];
            // If the identifier is SLOTS, take the player inventory slot as the item
            if(identifier.equalsIgnoreCase("SLOTS") || identifier.equalsIgnoreCase("SLOT")){
                int slot = Integer.parseInt(split[1]);
                ItemStack stack = target.getInventory().getItem(slot);
                if(stack == null){
                    return new ItemStack(Material.AIR);
                } else {
                    builder = new ItemBuilder(stack);
                }
            }
            // Head Item
            if(identifier.equalsIgnoreCase("HEAD") || identifier.equalsIgnoreCase("HEADS")){
                // Get the head value
                String headValue = split[1]
                        .replace("{player}", player.getName())
                        .replace("{target}", target.getName());
                // And now build the ItemStack using ItemBuilder
                ItemBuilder base = new ItemBuilder(XMaterial.PLAYER_HEAD.parseItem())
                        .name(Utils.tryParsePAPI(item.name(), player, target))
                        .lore(Utils.tryParsePAPI(item.lore(), player, target))
                        .amount(Math.max(1, item.amount()))
                        .customModelData(item.customModelData());
                try {
                    base.skull(headValue); // bisa jadi nama player
                } catch (Exception e) {
                    // fallback ke Steve (atau custom skin base64)
                    base.skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJl...");
                }
                builder = base;
            }

        } else {
            // If the item doesn't contains ';', that means the item is not a head
            // First of all, we check if the item is exist or valid
            Optional<XMaterial> optionalMaterial = XMaterial.matchXMaterial(item.material());
            // We check if the item is not valid
            if(!optionalMaterial.isPresent()){
                // If so, instead throwing an error, just return error item
                return errorItem(item);
            }
            // Finally create the item stack using the item builder
            builder = new ItemBuilder(optionalMaterial.get().parseItem())
                    .name(Utils.tryParsePAPI(item.name(), player, target))
                    .lore(Utils.tryParsePAPI(item.lore(), player, target))
                    .amount(Math.max(1, item.amount()))
                    .customModelData(item.customModelData());
        }
        // Add hide attributes item flag if it's enabled
        if(item.hideAttributes()) builder.flags(ItemFlag.HIDE_ATTRIBUTES);
        // Add random enchant and hide enchant attributes if item set to glowing
        if(item.glowing()) builder.enchant(Enchantment.UNBREAKING).flags(ItemFlag.HIDE_ENCHANTS);
        // Finally build the item stack
        ItemStack stack = builder.build();
        // Create ItemMeta
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        // Add the item model if it's not null or empty
        String modelItemString = item.itemModel();
        if (isVersionAtLeast(1, 21, 2)) {
            if (modelItemString != null) {
                String[] parts = modelItemString.split(":", 2);
                if (parts.length == 2) {
                    NamespacedKey modelItem = NamespacedKey.fromString(parts[0] + ":" + parts[1]);
                    meta.setItemModel(modelItem);
                    stack.setItemMeta(meta);
                }
            }
        }
        return stack;
    }

    public static ItemStack createGUIItem(GUIItem item, Player player, Player target){
        if(item.type() == null)
            return ItemManager.createItem(item, player, target);

        if(item.type().contains(";")){
            String[] split = item.type().split(";");
            String identifier = split[0];
            // Inventory slot item
            if(identifier.equalsIgnoreCase("SLOTS")){
                int slot = Integer.parseInt(split[1]);
                ItemStack stack = target.getInventory().getItem(slot);
                return stack == null ? new ItemStack(Material.AIR) : stack;
            }
            // Head Item
            if(identifier.equalsIgnoreCase("HEAD") || identifier.equalsIgnoreCase("HEADS")){
                String headValue = split[1]
                        .replace("{player}", player.getName())
                        .replace("{target}", target.getName());
                ItemBuilder builder = new ItemBuilder(XMaterial.PLAYER_HEAD.parseItem())
                        .name(Utils.tryParsePAPI(item.name(), player, target))
                        .lore(Utils.tryParsePAPI(item.lore(), player, target))
                        .amount(Math.max(1, item.amount()))
                        .customModelData(item.customModelData())
                        .skull(headValue);
                return builder.build();
            }
        }

        switch(item.type()){
            case "HELMET":
                return createArmorItem(item, player, target, target.getInventory().getHelmet());
            case "CHESTPLATE":
                return createArmorItem(item, player, target, target.getInventory().getChestplate());
            case "LEGGINGS":
                return createArmorItem(item, player, target, target.getInventory().getLeggings());
            case "BOOTS":
                return createArmorItem(item, player, target, target.getInventory().getBoots());
            case "MAIN_HAND":
                return createArmorItem(item, player, target, target.getItemInHand());
            case "OFF_HAND":{
                if(Utils.hasOffHand()){
                    return createArmorItem(item, player, target, target.getInventory().getItemInOffHand());
                }
                return new ItemStack(Material.AIR);
            }
            default:
                return ItemManager.createItem(item, player, target);
        }
    }

    public static void fillItem(FastInv inventory, FileConfiguration config){
        if(!config.getBoolean("fillItems.enabled")) return;
        // Get the Optional XMaterial
        Optional<XMaterial> optional = XMaterial.matchXMaterial(config.getString("fillItems.material"));
        // If the XMaterial isn't present, just return
        if(!optional.isPresent()) return;
        // Get the ItemStack if the XMaterial is present
        ItemStack stack = optional.get().parseItem();
        // Item builder boiss.
        ItemBuilder builder = new ItemBuilder(stack)
                .name(Common.color(config.getString("fillItems.name")))
                .lore(Common.color(config.getStringList("fillItems.lore")))
                .customModelData(config.getInt("fillItems.customModelData"));
        // Get the final item stack
        ItemStack finalStack = builder.build();
        // Loop through all inventory slots
        for (int i = 0; i < inventory.getInventory().getSize(); i++) {
            // Get the item stack from the slot
            ItemStack slotStack = inventory.getInventory().getItem(i);
            // Skip if the slot is not null and the type is not material AIR
            if(slotStack != null) continue;
            // If all going well, we set the slot to the fillter item stack
            inventory.setItem(i, finalStack);
        }
    }

    private static ItemStack createArmorItem(GUIItem item, Player player, Player target, ItemStack stack){
        if(stack == null || stack.getType() == Material.AIR) return createItem(item, player, target);
        return stack;
    }

    private static ItemStack errorItem(GUIItem item){

        return new ItemBuilder(XMaterial.BARRIER.parseItem())
                .name("&cInvalid Material!")
                .lore("&7Please check your configuration for item '{item}'".replace("{item}", item.name()), " ", "&7Additional Information:", "&7Material: {material}".replace("{material}", item.material()))
                .build();
    }

    public static boolean isVersionAtLeast(int major, int minor, int patch) {
        String version = Bukkit.getBukkitVersion().split("-")[0];
        String[] parts = version.split("\\.");

        try {
            int maj = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            int pat = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            if (maj != major) return maj > major;
            if (min != minor) return min > minor;
            return pat >= patch;
        } catch (NumberFormatException e) {
            return false; // fallback
        }
    }


}
