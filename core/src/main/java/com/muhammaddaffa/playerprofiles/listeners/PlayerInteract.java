package com.muhammaddaffa.playerprofiles.listeners;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerInteract implements Listener {
    private final PlayerProfiles plugin;
    private final Map<UUID, Long> mapCooldown = new HashMap<>();

    public PlayerInteract(PlayerProfiles plugin) { this.plugin = plugin; }

    public void clearCooldowns() { mapCooldown.clear(); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        mapCooldown.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRightClickEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        if (ConfigValue.MUST_SHIFT_CLICK && !player.isSneaking()) return;
        if (!plugin.getInventoryManager().canView(player, target)) return;
        if (!ConfigValue.COOLDOWN_ENABLED) {
            mapCooldown.clear();
        } else {
            long now = System.currentTimeMillis();
            mapCooldown.values().removeIf(expiry -> expiry <= now);
            long remaining = mapCooldown.getOrDefault(player.getUniqueId(), 0L) - now;
            if (remaining > 0) {
                player.sendMessage(Common.color(ConfigValue.COOLDOWN_MESSAGE
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{time}", Long.toString((remaining + 999) / 1000))));
                return;
            }
        }
        if (plugin.getInventoryManager().openInventory(null, player, target, false)) {
            if (ConfigValue.COOLDOWN_ENABLED) {
                mapCooldown.put(player.getUniqueId(), System.currentTimeMillis() + ConfigValue.COOLDOWN_TIME * 1000L);
            }
            Utils.playSound(player, "onProfileOpen");
        }
    }
}
