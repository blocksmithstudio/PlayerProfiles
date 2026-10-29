package com.muhammaddaffa.playerprofiles.inventory;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.hooks.combatlogx.HCombatLogX;
import com.muhammaddaffa.playerprofiles.hooks.deluxecombat.HDeluxeCombat;
import com.muhammaddaffa.playerprofiles.inventory.items.ItemsLoader;
import com.muhammaddaffa.playerprofiles.manager.DependencyManager;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUI;
import com.muhammaddaffa.playerprofiles.worldguardwrapper.WorldGuardWrapper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class InventoryManager {
    private final PlayerProfiles plugin;
    private final ItemsLoader itemsLoader;

    public InventoryManager(PlayerProfiles plugin) {
        this.plugin = plugin;
        itemsLoader = new ItemsLoader(plugin);
    }

    public void initialize() { itemsLoader.loadItems(); }
    public void reInitialize() { itemsLoader.reloadItems(); }

    public void closeInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof ProfileInventory) {
                player.closeInventory();
            }
        }
    }

    public void openInventory(@Nullable CustomGUI customGUI, Player player, Player target) {
        openInventory(customGUI, player, target, false);
    }

    /** Admin forced-open commands can explicitly bypass the normal viewing policy. */
    public boolean openInventory(@Nullable CustomGUI customGUI, Player player, Player target, boolean forced) {
        if (player == null || !player.isOnline()) return false;
        if (target == null || !target.isOnline()) {
            tell(player, ConfigValue.INVALID_PLAYER.replace("{player}", target == null ? "unknown" : target.getName()));
            return false;
        }
        if (!forced && !canView(player, target)) return false;
        FileConfiguration config = customGUI == null ? PlayerProfiles.GUI_DEFAULT.getConfig() : customGUI.config();
        try {
            ItemsLoader.validateMenu(config);
        } catch (IllegalArgumentException ex) {
            Logger.severe("Cannot open a menu: " + ex.getMessage());
            tell(player, PlayerProfiles.CONFIG_DEFAULT.getConfig().getString("messages.menuUnavailable",
                    "{prefix} &cThis menu is unavailable. Please contact an administrator."));
            return false;
        }
        ProfileInventory inventory = new ProfileInventory(customGUI == null ? itemsLoader.getMainMenuItems() : customGUI.items(),
                player, target, config.getInt("size"), config.getString("title", "Inventory"), config, forced);
        inventory.open(player);
        return true;
    }

    public boolean canView(Player player, Player target) {
        if (player == null || target == null || !player.isOnline() || !target.isOnline()) return false;
        if (!player.canSee(target) || (ConfigValue.DISABLE_NPC_PROFILE && target.hasMetadata("NPC"))) return false;
        String permission = player.equals(target) ? "playerprofiles.profile" : "playerprofiles.profile.others";
        if (!player.hasPermission(permission)) {
            tell(player, ConfigValue.NO_PERMISSION.replace("{permission}", permission));
            return false;
        }
        // Owners must still be able to open their own menu to unlock their profile.
        if (!player.equals(target) && plugin.getProfileManager().isProfileLocked(target)) {
            tell(player, ConfigValue.LOCKED_PROFILE);
            return false;
        }
        if (ConfigValue.DISABLED_WORLDS.contains(player.getWorld().getName())) {
            tell(player, ConfigValue.DISABLED_WORLD_MESSAGE);
            return false;
        }
        if (DependencyManager.WORLD_GUARD && Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            var finder = WorldGuardWrapper.getInstance().getRegionFinder();
            if (finder.getRegions(player.getLocation()).stream().anyMatch(ConfigValue.DISABLED_REGIONS::contains)) {
                tell(player, ConfigValue.PLAYER_DISABLED_REGIONS);
                return false;
            }
            if (finder.getRegions(target.getLocation()).stream().anyMatch(ConfigValue.DISABLED_REGIONS::contains)) {
                tell(player, ConfigValue.TARGET_DISABLED_REGIONS);
                return false;
            }
        }
        if (ConfigValue.DISABLE_IN_COMBAT_ENABLED &&
                ((DependencyManager.COMBAT_LOG_X && HCombatLogX.isInCombat(player)) ||
                 (DependencyManager.DELUXE_COMBAT && HDeluxeCombat.isInCombat(player)))) {
            tell(player, ConfigValue.DISABLE_IN_COMBAT_MESSAGE);
            return false;
        }
        return true;
    }

    private void tell(Player player, String message) {
        player.sendMessage(Common.color(message.replace("{prefix}", ConfigValue.PREFIX)));
    }
}
