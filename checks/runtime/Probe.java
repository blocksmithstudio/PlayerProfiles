package verification;

import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.ProfileInventory;
import com.muhammaddaffa.playerprofiles.inventory.items.ItemsLoader;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUI;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.command.*;
import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import com.muhammaddaffa.playerprofiles.utils.ItemManager;
import com.muhammaddaffa.playerprofiles.manager.profile.ProfileManager;
import com.muhammaddaffa.playerprofiles.listeners.PlayerInteract;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUIManager;

public class Probe extends JavaPlugin {
    private int checks;
    private final StringBuilder results = new StringBuilder();

    @Override public void onEnable() {
        if (!Boolean.getBoolean("playerprofiles.regression")) {
            getLogger().severe("This test plugin requires an isolated server and -Dplayerprofiles.regression=true.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try { runChecks(); }
            catch (Throwable error) {
                results.append("FAIL: ").append(error.getClass().getName()).append(": ")
                        .append(error.getMessage()).append('\n');
                error.printStackTrace();
            } finally {
                try { Files.writeString(Path.of("probe-results.txt"), results.toString()); }
                catch (Exception ignored) { }
                Bukkit.shutdown();
            }
        }, 1L);
    }

    private void check(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
        results.append("PASS: ").append(description).append('\n');
        checks++;
    }

    private void runChecks() throws Exception {
        PlayerProfiles plugin = (PlayerProfiles) Bukkit.getPluginManager().getPlugin("PlayerProfiles");
        check(plugin != null && plugin.isEnabled(), "PlayerProfiles enables on Paper " + Bukkit.getBukkitVersion());
        check(plugin.getDescription().getVersion().equals("8.0.4"), "plugin descriptor matches Maven version");
        for (String alias : List.of("playerprofiles", "profile", "lockprofile", "unlockprofile", "toggleprofile",
                "playerprofile", "playerp", "pp", "viewprofile", "p", "profilelock", "lock", "profileunlock", "unlock")) {
            check(Bukkit.getPluginCommand(alias) == plugin.getCommand(Bukkit.getPluginCommand(alias).getName()),
                    "registered command/alias " + alias);
        }
        Viewer viewer = new Viewer("ProfileCheck", "00000000-0000-0000-0000-000000000001");
        Viewer other = new Viewer("TargetCheck", "00000000-0000-0000-0000-000000000002");
        Player player = viewer.player;
        Player target = other.player;
        if (Boolean.getBoolean("playerprofiles.restart")) {
            check(plugin.getProfileManager().isProfileLocked(player), "locked state restored after actual server restart");
        }
        plugin.getProfileManager().lockProfile(player);
        check(plugin.getProfileManager().isProfileLocked(player), "profile lock changes in-memory state");
        check(YamlConfiguration.loadConfiguration(PlayerProfiles.DATA_DEFAULT.getFile())
                .getBoolean("data." + player.getUniqueId()), "lock persisted immediately without shutdown");
        check(PlayerProfiles.DATA_DEFAULT.getConfig().getBoolean("data." + player.getUniqueId()), "profile lock serializes to existing YAML format");
        plugin.reloadAllThing();
        check(plugin.getProfileManager().isProfileLocked(player), "configuration reload preserves profile lock");
        plugin.getProfileManager().unlockProfile(player);
        check(!plugin.getProfileManager().isProfileLocked(player), "profile unlock changes in-memory state");
        // Suppress periodic tasks for these synthetic viewers; the real server still renders every item.
        ConfigValue.AUTO_REFRESH_ENABLED = false;
        ConfigValue.DISTANCE_CHECK_ENABLED = false;
        var mainItems = ItemsLoader.readItems(PlayerProfiles.GUI_DEFAULT.getConfig());
        ProfileInventory ownerMenu = new ProfileInventory(mainItems, player, player, 54, "Owner {target}");
        check(ownerMenu.getInventory().getSize() == 54, "owner menu renders on Paper 26.3");
        check(ownerMenu.getInventory().getItem(51) != null, "owner lock action is displayed");
        check(ownerMenu.getInventory().getItem(53) != null, "owner unlock action is displayed");
        check(ownerMenu.getInventory().getItem(24).getType() == Material.EMERALD, "visitor action retains configured slot/material");
        ProfileInventory visitorMenu = new ProfileInventory(mainItems, player, target, 54, "Visitor {target}");
        check(visitorMenu.getInventory().getSize() == 54, "visitor menu renders on Paper 26.3");
        check(visitorMenu.getInventory().getItem(51).getType() == Material.BLACK_STAINED_GLASS_PANE,
                "owner-only action is hidden from visitors");
        check(!plugin.getCustomGUIManager().getListName().isEmpty(), "custom GUIs load");
        for (String name : plugin.getCustomGUIManager().getListName()) {
            CustomGUI gui = plugin.getCustomGUIManager().getByName(name);
            ProfileInventory menu = new ProfileInventory(gui.items(), player, target, gui.size(), gui.title());
            check(menu.getInventory().getSize() == gui.size(), "custom GUI renders: " + name);
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playerprofiles listgui");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playerprofiles reload");
        check(plugin.isEnabled(), "listgui/reload console commands complete");
        check(Class.forName("com.muhammaddaffa.playerprofiles.mclibs.MDLib", false,
                plugin.getClass().getClassLoader()) != null, "relocated MDLib is available");
        regressions(plugin, viewer, other);
        plugin.getProfileManager().lockProfile(player);
        results.append("TOTAL: ").append(checks).append(" checks passed\n");
    }

    private class Viewer {
        final Set<String> denied = new HashSet<>();
        final List<String> messages = new ArrayList<>();
        boolean online = true, visible = true, npc;
        int closed, opened;
        Inventory lastInventory;
        final Player player;
        final ItemStack held = new ItemStack(Material.DIAMOND_SWORD);

        Viewer(String name, String id) {
            UUID uuid = UUID.fromString(id);
            World world = Bukkit.getWorlds().getFirst();
            PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getSize" -> 41;
                        case "getItem", "getItemInMainHand" -> held;
                        default -> defaultValue(method.getReturnType());
                    });
            player = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getName" -> name;
                        case "getUniqueId" -> uuid;
                        case "getWorld" -> world;
                        case "getLocation" -> new Location(world, 0, 64, 0);
                        case "getInventory" -> inventory;
                        case "getServer" -> Bukkit.getServer();
                        case "getHealth" -> 20.0;
                        case "getExp" -> 0.5f;
                        case "getLevel" -> 7;
                        case "hasPermission" -> !denied.contains(args[0]);
                        case "canSee" -> visible;
                        case "hasMetadata" -> npc;
                        case "isOnline" -> online;
                        case "isSneaking" -> true;
                        case "openInventory" -> { opened++; lastInventory = (Inventory) args[0]; yield null; }
                        case "closeInventory" -> { closed++; yield null; }
                        case "sendMessage" -> { if (args[0] instanceof String text) messages.add(text); yield null; }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> uuid.hashCode();
                        case "toString" -> name;
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

    private GUIItem item(String type, String material) {
        return new GUIItem(type, material, 1, "Test", List.of(0), false, false, false, "",
                List.of(), List.of(), List.of(), 0, "", false, false, 1);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Consumer<InventoryClickEvent>> handlers(ProfileInventory menu) throws Exception {
        var field = menu.getClass().getSuperclass().getDeclaredField("itemHandlers");
        field.setAccessible(true);
        return (Map<Integer, Consumer<InventoryClickEvent>>) field.get(menu);
    }

    private org.bukkit.inventory.InventoryView view(Inventory top, Player player) {
        return (org.bukkit.inventory.InventoryView) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{org.bukkit.inventory.InventoryView.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTopInventory" -> top;
                    case "getBottomInventory" -> player.getInventory();
                    case "getPlayer" -> player;
                    case "getType" -> InventoryType.CHEST;
                    case "getTitle", "getOriginalTitle" -> "Test";
                    default -> defaultValue(method.getReturnType());
                });
    }

    @SuppressWarnings("unchecked")
    private void regressions(PlayerProfiles plugin, Viewer viewer, Viewer other) throws Exception {
        Player player = viewer.player, target = other.player;
        ConfigValue.AUTO_REFRESH_ENABLED = false;
        ConfigValue.DISTANCE_CHECK_ENABLED = false;
        ConfigValue.DISABLED_WORLDS = List.of();
        var manager = plugin.getInventoryManager();
        plugin.getProfileManager().unlockProfile(target);
        check(manager.canView(player, target), "normal permitted profile is accessible");
        viewer.denied.add("playerprofiles.profile.others");
        check(!manager.canView(player, target), "right-click/command policy enforces existing others permission");
        viewer.denied.clear(); viewer.visible = false;
        check(!manager.canView(player, target), "hidden targets cannot be viewed normally");
        viewer.visible = true; other.npc = true;
        check(!manager.canView(player, target), "NPC targets obey disableNPC");
        other.npc = false; plugin.getProfileManager().lockProfile(target);
        check(!manager.canView(player, target), "normal commands cannot bypass profile locking");
        plugin.getProfileManager().lockProfile(player);
        check(manager.canView(player, player), "owners can open their own locked profile");
        ConfigValue.DISABLED_WORLDS = List.of(player.getWorld().getName());
        viewer.visible = false; viewer.denied.add("playerprofiles.profile.others");
        check(manager.openInventory(null, player, target, true), "admin force-open bypasses locks/world/permission/visibility");
        YamlConfiguration navigationConfig = new YamlConfiguration(); navigationConfig.set("size", 9);
        GUIItem navigation = new GUIItem(null, "STONE", 1, "Navigate", List.of(0), false, false, false, "",
                List.of(), List.of("[OPENGUIMENU] punish-gui.yml"), List.of(), 0, "", false, false, 1);
        ProfileInventory forcedMenu = new ProfileInventory(List.of(navigation), player, target, 9, "Forced", navigationConfig, true);
        int beforeNavigation = viewer.opened;
        handlers(forcedMenu).get(0).accept(new InventoryClickEvent(view(forcedMenu.getInventory(), player),
                InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        check(viewer.opened == beforeNavigation + 1, "admin forced-open bypass survives menu navigation");
        ConfigValue.DISABLED_WORLDS = List.of(); viewer.visible = true; viewer.denied.clear();
        plugin.getProfileManager().unlockProfile(target);
        ConfigValue.DISABLED_WORLDS = List.of(player.getWorld().getName());
        check(!manager.canView(player, target), "disabled worlds apply to regular commands");
        ConfigValue.DISABLED_WORLDS = List.of();
        ProfileInventory normalMenu = new ProfileInventory(List.of(navigation), player, target, 9, "Normal", navigationConfig, false);
        plugin.getProfileManager().lockProfile(target); beforeNavigation = viewer.opened;
        handlers(normalMenu).get(0).accept(new InventoryClickEvent(view(normalMenu.getInventory(), player),
                InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        check(viewer.opened == beforeNavigation, "normal menu navigation cannot bypass a newly locked profile");
        plugin.getProfileManager().unlockProfile(target);
        check(!manager.openInventory(null, null, target, true), "null recipient fails safely even for forced opens");
        check(!manager.openInventory(null, player, null, true), "null target fails safely even for forced opens");
        other.online = false;
        check(!manager.openInventory(null, player, target, true), "disconnected target fails safely");
        other.online = true;

        for (String spec : List.of("SLOT;", "SLOT;no", "SLOT;-1", "SLOT;41", "SLOTS;999999999999", "HEAD;", "UNKNOWN;x")) {
            check(ItemManager.createGUIItem(item(null, spec), player, target).getType() == Material.BARRIER,
                    "malformed material returns error item: " + spec);
            check(ItemManager.createGUIItem(item(spec, "STONE"), player, target).getType() == Material.BARRIER,
                    "malformed type returns error item: " + spec);
        }
        check(ItemManager.createGUIItem(item(null, ""), player, target).getType() == Material.BARRIER,
                "blank materials return an error item");
        check(ItemManager.createGUIItem(item(null, "AIR"), player, target).getType().isAir(),
                "AIR items render without requesting item metadata");
        check(ItemManager.createGUIItem(item(null, "HEAD;not-a-valid-texture!"), player, target).getType() == Material.PLAYER_HEAD,
                "invalid head texture falls back to a valid player head");
        ItemStack copy = ItemManager.createGUIItem(item("SLOTS;0", "STONE"), player, target);
        check(copy.equals(other.held) && copy != other.held, "inventory previews clone original items");
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("size: 9\nfillItems: {enabled: true, material: DIAMOND, name: Test}");
        ProfileInventory custom = new ProfileInventory(List.of(), player, target, 9, "Custom", config, false);
        check(custom.getInventory().getItem(0).getType() == Material.DIAMOND, "custom menu uses its own filler");
        for (String filler : List.of("AIR", "")) {
            config.set("fillItems.material", filler);
            ProfileInventory emptyFill = new ProfileInventory(List.of(), player, target, 9, "Empty fill", config, false);
            check(emptyFill.getInventory().getItem(0) == null, "empty/air filler is safely ignored");
        }
        config.set("fillItems.enabled", false);
        GUIItem restricted = new GUIItem(null, "STONE", 1, "Restricted", List.of(0), false, false, true,
                "test.item", List.of(), List.of("[MESSAGEPLAYER] action-fired"), List.of(), 0, "", false, false, 1);
        ProfileInventory menu = new ProfileInventory(List.of(restricted), player, target, 9, "Permission", config, false);
        Consumer<InventoryClickEvent> old = handlers(menu).get(0);
        check(old != null, "permitted item has a click handler");
        viewer.denied.add("test.item");
        old.accept(new InventoryClickEvent(view(menu.getInventory(), player), InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        check(!handlers(menu).containsKey(0) && menu.getInventory().getItem(0) == null,
                "revocation clears both stale item and click handler immediately");
        check(viewer.messages.stream().noneMatch(m -> m.contains("action-fired")), "revoked click never executes its command");
        viewer.denied.clear();
        var distance = ProfileInventory.class.getDeclaredMethod("checkDistance", Player.class, Player.class);
        distance.setAccessible(true);
        int closed = viewer.closed;
        distance.invoke(menu, player, null);
        other.online = false; distance.invoke(menu, player, target); other.online = true;
        check(viewer.closed == closed + 2, "null and disconnected targets close safely without dereferencing null");

        var listenerField = PlayerProfiles.class.getDeclaredField("playerInteract");
        listenerField.setAccessible(true);
        PlayerInteract listener = (PlayerInteract) listenerField.get(plugin);
        ConfigValue.MUST_SHIFT_CLICK = false; ConfigValue.COOLDOWN_ENABLED = true; ConfigValue.COOLDOWN_TIME = 60;
        listener.onPlayerRightClickEntity(new PlayerInteractAtEntityEvent(player, target, new org.bukkit.util.Vector(), org.bukkit.inventory.EquipmentSlot.HAND));
        var cooldowns = PlayerInteract.class.getDeclaredField("mapCooldown"); cooldowns.setAccessible(true);
        check(((Map<?, ?>) cooldowns.get(listener)).containsKey(player.getUniqueId()), "cooldown keys are UUIDs");
        listener.onQuit(new PlayerQuitEvent(player, ""));
        check(((Map<?, ?>) cooldowns.get(listener)).isEmpty(), "quit removes cooldown state");
        listener.onPlayerRightClickEntity(new PlayerInteractAtEntityEvent(player, target, new org.bukkit.util.Vector(), org.bukkit.inventory.EquipmentSlot.HAND));
        ConfigValue.COOLDOWN_ENABLED = false; int opened = viewer.opened;
        listener.onPlayerRightClickEntity(new PlayerInteractAtEntityEvent(player, target, new org.bukkit.util.Vector(), org.bukkit.inventory.EquipmentSlot.HAND));
        check(viewer.opened == opened + 1 && ((Map<?, ?>) cooldowns.get(listener)).isEmpty(), "disabling cooldown ignores and clears old entries");
        Path settings = PlayerProfiles.CONFIG_DEFAULT.getFile().toPath();
        String settingsBefore = Files.readString(settings);
        try {
            PlayerProfiles.CONFIG_DEFAULT.getConfig().set("cooldown.enabled", false);
            PlayerProfiles.CONFIG_DEFAULT.saveConfig();
            ((Map<UUID, Long>) cooldowns.get(listener)).put(player.getUniqueId(), System.currentTimeMillis() + 60000);
            plugin.reloadAllThing();
            check(((Map<?, ?>) cooldowns.get(listener)).isEmpty(), "disabling cooldown during reload clears state without another interaction");
        } finally {
            Files.writeString(settings, settingsBefore);
            plugin.reloadAllThing();
            ConfigValue.AUTO_REFRESH_ENABLED = false;
            ConfigValue.DISTANCE_CHECK_ENABLED = false;
        }
        customFiles(plugin);
        persistence(plugin, player);
        for (String command : List.of("playerprofiles openprofile MissingTarget MissingRecipient",
                "playerprofiles opengui MissingRecipient MissingTarget punish-gui.yml")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            check(plugin.isEnabled(), "invalid admin player lookup returns safely");
        }
        plugin.reloadAllThing();
    }

    private void customFiles(PlayerProfiles plugin) throws Exception {
        Path dir = plugin.getDataFolder().toPath().resolve("custom-gui");
        Path text = dir.resolve("ignored.txt"), folder = dir.resolve("folder.yml"), bad = dir.resolve("invalid.yml");
        Files.writeString(text, "ignored"); Files.createDirectories(folder); Files.writeString(bad, "size: [broken");
        CustomGUIManager menus = new CustomGUIManager(plugin); menus.loadCustomGUI();
        check(menus.getByName("ignored.txt") == null && menus.getByName("folder.yml") == null && menus.getByName("invalid.yml") == null,
                "non-YAML files, directories and malformed YAML are skipped");
        Files.delete(text); Files.delete(folder); Files.delete(bad);
        var creatorConfig = PlayerProfiles.GUI_CREATOR.getConfig(); String original = creatorConfig.saveToString();
        try {
            creatorConfig.set("menu", null); new com.muhammaddaffa.playerprofiles.manager.customgui.CustomGuiCreator(plugin).createCustomGui();
            creatorConfig.set("menu.bad.fileName", "../escaped-check.yml");
            new com.muhammaddaffa.playerprofiles.manager.customgui.CustomGuiCreator(plugin).createCustomGui();
            check(!Files.exists(plugin.getDataFolder().toPath().resolve("escaped-check.yml")), "missing creator menu is safe and path traversal is rejected");
            Path real = plugin.getDataFolder().toPath().resolve("real-custom-gui");
            Files.move(dir, real); Files.writeString(dir, "not a directory");
            try { new CustomGUIManager(plugin).loadCustomGUI(); check(true, "unavailable GUI directory returns safely"); }
            finally { Files.delete(dir); Files.move(real, dir); }
        } finally { creatorConfig.loadFromString(original); }
    }

    private void persistence(PlayerProfiles plugin, Player player) throws Exception {
        var manager = plugin.getProfileManager();
        manager.lockProfile(player);
        Path data = PlayerProfiles.DATA_DEFAULT.getFile().toPath();
        String original = Files.readString(data);
        Files.writeString(data, "data: [broken");
        try {
            ProfileManager fresh = new ProfileManager(); boolean rejected = false;
            try { fresh.loadProfileData(); } catch (IllegalStateException expected) { rejected = true; }
            fresh.saveProfileData();
            check(rejected && Files.readString(data).equals("data: [broken"), "corrupt profile data is rejected and never overwritten");
            try { manager.loadProfileData(); } catch (IllegalStateException expected) { }
            manager.saveProfileData();
            check(Files.readString(data).equals("data: [broken"), "failed reload also prevents a previously loaded manager from overwriting corrupt data");
        } finally { Files.writeString(data, original); manager.loadProfileData(); }
        Path saved = data.resolveSibling("data-check-backup.yml"); Files.move(data, saved); Files.createDirectory(data);
        try {
            boolean rejected = false;
            try { manager.unlockProfile(player); } catch (IllegalStateException expected) { rejected = true; }
            check(rejected, "failed save reports failure");
            check(manager.isProfileLocked(player), "failed save restores the managed profile lock");
            check(Files.readString(saved).equals(original), "failed save preserves the previous file");
            check(PlayerProfiles.DATA_DEFAULT.getConfig().getBoolean("data." + player.getUniqueId()),
                    "failed save restores a navigable configuration section");
        } finally { Files.delete(data); Files.move(saved, data); }
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }
}
