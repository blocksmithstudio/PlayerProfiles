package com.muhammaddaffa.playerprofiles;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigValue {

    public static String PREFIX;

    // Auto Refresh
    public static boolean AUTO_REFRESH_ENABLED;
    public static int AUTO_REFRESH_TICK;

    // Distance Check
    public static boolean DISTANCE_CHECK_ENABLED;
    public static double MAXIMUM_DISTANCE;
    public static String DISTANCE_TOO_FAR;

    // Options
    public static boolean DISABLE_NPC_PROFILE;
    public static boolean MUST_SHIFT_CLICK;
    public static boolean DISABLE_IN_COMBAT_ENABLED;
    public static String DISABLE_IN_COMBAT_MESSAGE;

    // Disabled worlds
    public static List<String> DISABLED_WORLDS;
    public static String DISABLED_WORLD_MESSAGE;

    // Disabled regions
    public static List<String> DISABLED_REGIONS;
    public static String PLAYER_DISABLED_REGIONS;
    public static String TARGET_DISABLED_REGIONS;

    // Interact Cooldown
    public static boolean COOLDOWN_ENABLED;
    public static int COOLDOWN_TIME;
    public static String COOLDOWN_MESSAGE;

    // Messages
    public static String NO_PERMISSION;
    public static String RELOAD;
    public static String INVALID_PLAYER;
    public static String LOCKED_PROFILE;
    public static String LOCK_PROFILE;
    public static String LOCK_PROFILE_OTHERS;
    public static String UNLOCK_PROFILE;
    public static String UNLOCK_PROFILE_OTHERS;
    public static String INVALID_GUI_NAME;
    public static String LIST_GUI;

    // Messages List
    public static List<String> HELP_MESSAGES;

    public static void initialize(){
        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        PREFIX = config.getString("messages.prefix");

        AUTO_REFRESH_ENABLED = config.getBoolean("autoRefresh.enabled");
        AUTO_REFRESH_TICK = Math.max(1, config.getInt("autoRefresh.refreshEvery", 20));

        DISTANCE_CHECK_ENABLED = config.getBoolean("distanceCheck.enabled");
        MAXIMUM_DISTANCE = config.getDouble("distanceCheck.distance", 30);
        if (!Double.isFinite(MAXIMUM_DISTANCE) || MAXIMUM_DISTANCE < 0) MAXIMUM_DISTANCE = 30;
        DISTANCE_TOO_FAR = config.getString("distanceCheck.tooFarMessage");

        DISABLE_NPC_PROFILE = config.getBoolean("options.disableNPC");
        MUST_SHIFT_CLICK = config.getBoolean("options.shiftClick");
        DISABLE_IN_COMBAT_ENABLED = config.getBoolean("options.disableInCombat.enabled");
        DISABLE_IN_COMBAT_MESSAGE = config.getString("options.disableInCombat.message");

        DISABLED_WORLDS = config.getStringList("disabledWorlds.worlds");
        DISABLED_WORLD_MESSAGE = config.getString("disabledWorlds.message");

        DISABLED_REGIONS = config.getStringList("disabledRegions.regions");
        PLAYER_DISABLED_REGIONS = config.getString("disabledRegions.playerInDisabledRegionMessage");
        TARGET_DISABLED_REGIONS = config.getString("disabledRegions.targetInDisabledRegionMessage");

        COOLDOWN_ENABLED = config.getBoolean("cooldown.enabled");
        COOLDOWN_TIME = Math.max(0, config.getInt("cooldown.duration"));
        COOLDOWN_MESSAGE = config.getString("cooldown.message");

        NO_PERMISSION = config.getString("messages.noPermission");
        RELOAD = config.getString("messages.reload");
        INVALID_PLAYER = config.getString("messages.invalidPlayer");
        LOCKED_PROFILE = config.getString("messages.targetProfileLocked");
        LOCK_PROFILE = config.getString("messages.lockProfile");
        LOCK_PROFILE_OTHERS = config.getString("messages.lockProfileOthers");
        UNLOCK_PROFILE = config.getString("messages.unlockProfile");
        UNLOCK_PROFILE_OTHERS = config.getString("messages.unlockProfileOthers");
        INVALID_GUI_NAME = config.getString("messages.invalidGUIName");
        LIST_GUI = config.getString("messages.listGUI");

        HELP_MESSAGES = config.getStringList("messages.help");
    }

    public static String template() {
        return """
            #############################################################
            #                                                           #
            #                   Player Profiles - Template GUI
            #                        DO NOT DELETE THIS GUI!
            #############################################################
            
            # List of placeholder builtin you can use!
            # And it supports PlaceholderAPI
            # {target} = The target player
            # {player} = The player who opened the inventory
            # {target_status} = The target profile status (locked or unlocked)
            # {player_status} = The player profile status (locked or unlocked)
            # {target_health} = The target health
            # {player_health} = The player health
            # {target_exp} = The target experience
            # {player_exp} = The player experience
            # {target_level} = The target level
            # {player_level} = The player level
            # {target_uuid} = The target UUID
            # {player_uuid} = The player UUID
            # {target_world} = The target world
            # {player_world} = The player world
            
            title: "Default Menu" # Inventory title
            size: 27 # Inventory size
            
            # We do support fill item automatically but if you want to be more customizeable,
            # you can disable it and create your own fill item on the items section
            fillItems:
              enabled: true
              material: BLACK_STAINED_GLASS_PANE
              name: "&f"
              lore: []
            
            items:
              # Create your own item by following the example below
              1:
                # The material can be anything but if you want to get the player head
                # You can do by doing it like below
                material: head;{target}
                # You can also get the player head by doing it like below
                name: "&6{target}'s Information"
                # Slots can be anything but keep it surrounded by [] and separated by comma if you want
                # to put the item on multiple slots
                slots: [ 12 ]
                # You can also get the player head by doing it like below
                glowing: false
                # You can also get the player head by doing it like below
                hideAttributes: false
                # You can set the item custom model data
                customModelData: 0
                # You can set the item model data
                # itemModel: minecraft:dirt
                # You can set the item to only be seen by the visitor
                # Or only be seen by the owner
                # Or both
                onlyVisitor: false
                onlyOwner: false
                # You can set the item to only be seen by the player who has the permission
                # If you don't want to use it just skip it or remove it.
                usePermission: false
                permission: "custom.permission"
                # You can set the item priority
                # The higher the priority, the more it will be prioritized
                # If the priority is the same, it will be based on the order of the item
                priority: 0
                # You can set the item lore
                lore:
                  - ""
                  - " * &7Profile Status: {target_status}"
                  - " * &7Health: &c{target_health}"
                  - " * &7Level: &6{target_level}"
                  - " * &7Experience: &6{target_exp}"
                  - " * &7World: &6{target_world}"
                  - ""
                  - "&bMore with PlaceholderAPI"
                  - " * &7Is Flying: &6%player_is_flying%"
                  - " * &7Is Sneaking: &6%player_is_sneaking%"
                  - " * &7Is Sprinting: &6%player_is_sprinting%"
                  - " * &7Is OP: &6%player_is_op%"
                  - " * &7Ping: &a%player_ping%ms"
                # You can set the item commands
                # The command can be anything but keep it surrounded by []
                # You can use the commands either by sending via console or player just by doing this
                # [CONSOLE] /command
                # [PLAYER] /command
                # [MESSAGEPLAYER] &aMessage
                # [MESSAGETARGET] &aMessage
                # [OPENGUIMENU] gui-name.yml
                # [CLOSE]
                leftClickCommands: []
                rightClickCommands: []
              2:
                material: DIRT
                name: "&6Complement {target}!"
                slots: [ 14 ]
                glowing: false
                hideAttributes: true
                customModelData: 0
                # You can set the item model data
                # itemModel: minecraft:dirt
                onlyVisitor: false
                onlyOwner: false
                lore:
                  - "&7Complement {target}!"
                leftClickCommands:
                  - "[PLAYER] msg {target} i like your hair :)"
                  - "[MESSAGE] &8[&6Profile&8] &aYou have been complemented by &e{player}"
                  - "[CLOSE]"
                rightClickCommands:
                  - "[PLAYER] msg {target} i like your hair :)"
                  - "[MESSAGE] &8[&6Profile&8] &aYou have been complemented by &e{player}"
                  - "[CLOSE]"
            """;
    }


}
