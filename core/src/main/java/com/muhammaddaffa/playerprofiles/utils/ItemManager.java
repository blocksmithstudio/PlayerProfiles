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

    public static ItemStack createItem(GUIItem item, Player player, Player target) {
        if (item.material().isBlank()) return errorItem(item);
        ItemBuilder builder;
        if (item.material().contains(";")) {
            String[] parts = item.material().split(";", 2);
            if (parts[1].isBlank()) return errorItem(item);
            switch (parts[0].toUpperCase(java.util.Locale.ROOT)) {
                case "SLOT", "SLOTS" -> {
                    ItemStack stack = slotItem(item, target, parts[1]);
                    if (stack.getType() == Material.AIR) return stack;
                    builder = new ItemBuilder(stack);
                }
                case "HEAD", "HEADS" -> builder = headBuilder(item, player, target, parts[1]);
                default -> { return errorItem(item); }
            }
        } else {
            Optional<XMaterial> material = XMaterial.matchXMaterial(item.material());
            ItemStack stack = material.map(XMaterial::parseItem).orElse(null);
            if (stack == null) return errorItem(item);
            if (stack.getType().isAir()) return stack;
            if (stack.getItemMeta() == null) return errorItem(item);
            builder = configuredBuilder(item, player, target, stack);
        }
        if (item.hideAttributes()) builder.flags(ItemFlag.HIDE_ATTRIBUTES);
        if (item.glowing()) builder.enchant(Enchantment.UNBREAKING).flags(ItemFlag.HIDE_ENCHANTS);
        ItemStack stack = builder.build();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        String model = item.itemModel();
        if (isVersionAtLeast(1, 21, 2) && model != null && !model.isEmpty()) {
            NamespacedKey key = NamespacedKey.fromString(model);
            if (key == null) return errorItem(item);
            meta.setItemModel(key);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemBuilder configuredBuilder(GUIItem item, Player player, Player target, ItemStack stack) {
        return new ItemBuilder(stack)
                .name(Utils.tryParsePAPI(item.name(), player, target))
                .lore(Utils.tryParsePAPI(item.lore(), player, target))
                .amount(Math.max(1, item.amount()))
                .customModelData(item.customModelData());
    }

    private static ItemBuilder headBuilder(GUIItem item, Player player, Player target, String value) {
        ItemBuilder builder = configuredBuilder(item, player, target, new ItemStack(Material.PLAYER_HEAD));
        String identifier = value.replace("{player}", player.getName()).replace("{target}", target.getName());
        try {
            if (identifier.equals(player.getName())) builder.skull(player);
            else if (identifier.equals(target.getName())) builder.skull(target);
            else builder.skull(identifier);
        } catch (RuntimeException ex) {
            // An untextured player head is a valid fallback for an invalid texture.
            builder = configuredBuilder(item, player, target, new ItemStack(Material.PLAYER_HEAD));
        }
        return builder;
    }

    private static ItemStack slotItem(GUIItem item, Player target, String value) {
        try {
            int slot = Integer.parseInt(value);
            if (slot < 0 || slot >= target.getInventory().getSize()) return errorItem(item);
            ItemStack stack = target.getInventory().getItem(slot);
            return stack == null ? new ItemStack(Material.AIR) : stack.clone();
        } catch (NumberFormatException ex) {
            return errorItem(item);
        }
    }

    public static ItemStack createGUIItem(GUIItem item, Player player, Player target) {
        if (item.type().contains(";")) {
            String[] parts = item.type().split(";", 2);
            if (parts[1].isBlank()) return errorItem(item);
            return switch (parts[0].toUpperCase(java.util.Locale.ROOT)) {
                case "SLOT", "SLOTS" -> slotItem(item, target, parts[1]);
                case "HEAD", "HEADS" -> headBuilder(item, player, target, parts[1]).build();
                default -> errorItem(item);
            };
        }
        return switch (item.type()) {
            case "HELMET" -> createArmorItem(item, player, target, target.getInventory().getHelmet());
            case "CHESTPLATE" -> createArmorItem(item, player, target, target.getInventory().getChestplate());
            case "LEGGINGS" -> createArmorItem(item, player, target, target.getInventory().getLeggings());
            case "BOOTS" -> createArmorItem(item, player, target, target.getInventory().getBoots());
            case "MAIN_HAND" -> createArmorItem(item, player, target, target.getInventory().getItemInMainHand());
            case "OFF_HAND" -> Utils.hasOffHand()
                    ? createArmorItem(item, player, target, target.getInventory().getItemInOffHand())
                    : new ItemStack(Material.AIR);
            default -> createItem(item, player, target);
        };
    }

    public static void fillItem(FastInv inventory, FileConfiguration config){
        if(!config.getBoolean("fillItems.enabled")) return;
        // Get the Optional XMaterial
        String material = config.getString("fillItems.material", "AIR");
        if (material.isBlank()) return;
        Optional<XMaterial> optional = XMaterial.matchXMaterial(material);
        // If the XMaterial isn't present, just return
        if(!optional.isPresent()) return;
        // Get the ItemStack if the XMaterial is present
        ItemStack stack = optional.get().parseItem();
        if (stack == null || stack.getItemMeta() == null) return;
        // Item builder boiss.
        ItemBuilder builder = new ItemBuilder(stack)
                .name(Common.color(config.getString("fillItems.name", "")))
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
        return stack.clone();
    }

    private static ItemStack errorItem(GUIItem item){

        return new ItemBuilder(XMaterial.BARRIER.parseItem())
                .name("&cInvalid Material!")
                .lore("&7Please check your configuration for item '{item}'".replace("{item}", item.name()), " ", "&7Additional Information:", "&7Material: {material}".replace("{material}", item.material()))
                .build();
    }

    public static boolean isVersionAtLeast(int major, int minor, int patch) {
        return isVersionAtLeast(Bukkit.getBukkitVersion(), major, minor, patch);
    }

    public static boolean isVersionAtLeast(String bukkitVersion, int major, int minor, int patch) {
        String version = bukkitVersion.split("-")[0];
        String[] parts = version.split("\\.");

        if (parts.length < 2) return false;
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
