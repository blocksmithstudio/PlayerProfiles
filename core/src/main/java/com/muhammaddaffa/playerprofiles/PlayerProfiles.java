package com.muhammaddaffa.playerprofiles;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.playerprofiles.commands.*;

import com.muhammaddaffa.playerprofiles.inventory.InventoryManager;
import com.muhammaddaffa.playerprofiles.listeners.PlayerInteract;
import com.muhammaddaffa.playerprofiles.manager.DependencyManager;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUIManager;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGuiCreator;
import com.muhammaddaffa.playerprofiles.manager.profile.ProfileManager;
import com.muhammaddaffa.playerprofiles.metrics.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerProfiles extends JavaPlugin {

    private static final int BSTATS_ID = 7049;

    private static PlayerProfiles instance;

    public static Config DATA_DEFAULT, CONFIG_DEFAULT, GUI_DEFAULT, GUI_CREATOR;

    private final InventoryManager inventoryManager = new InventoryManager(this);
    private final CustomGUIManager customGUIManager = new CustomGUIManager(this);
    private final ProfileManager profileManager = new ProfileManager();
    private final CustomGuiCreator customGuiCreator = new CustomGuiCreator(this);

    @Override
    public void onLoad() {
        MDLib.inject(this);
        MDLib.registerWorldGuard();
    }

    @Override
    public void onEnable(){
        instance = this;
        // Injecting the libs
        MDLib.onEnable(this);
        // Check the dependency
        DependencyManager.checkDependency();
        // Initialize all config
        initializeConfig();
        // Initialize all custom gui creator
        customGuiCreator.createCustomGui();
        // Initialize all config values
        ConfigValue.initialize();
        // Load all items for the inventory
        inventoryManager.initialize();
        // Load all custom guis
        customGUIManager.loadCustomGUI();
        // Load all profile data
        profileManager.loadProfileData();
        // Register the player interact event
        Bukkit.getPluginManager().registerEvents(new PlayerInteract(this), this);
        // Register all commands
        registerCommands();
        // Add bstats metrics
        new Metrics(this, BSTATS_ID);
    }

    @Override
    public void onDisable(){
        // Plugin shutdown logic
        MDLib.shutdown();
        // Save all profile data
        profileManager.saveProfileData();
    }

    private void initializeConfig() {
        CONFIG_DEFAULT      = new Config("config.yml", null, true);
        GUI_DEFAULT         = new Config("gui.yml", null, true);
        DATA_DEFAULT        = new Config("data.yml", null, false);
        GUI_CREATOR         = new Config("gui-creator.yml", null, true);

        CONFIG_DEFAULT.setShouldUpdate(true);
        Config.updateConfigs();
        Config.reload();
    }

    public void reloadAllThing(){
        // Reload all configuration
        Config.reload();
        // Re-initialize the config value
        ConfigValue.initialize();
        // Reload all items for the profile inventory
        inventoryManager.reInitialize();
        // Reload all custom gui creator
        customGuiCreator.createCustomGui();
        // Reload all custom guis
        customGUIManager.reloadCustomGUI();
    }

    private void registerCommands(){
        // Register /playerprofiles command
        new MainCommand(this).registerThisCommand();
        // Register /profile command
        new ProfileCommand(this).registerThisCommand();
        // Register /lockprofile command
        new LockCommand(this).registerThisCommand();
        // Register /unlockprofile command
        new UnlockCommand(this).registerThisCommand();
        // Register /toggleprofile command
        new ToggleCommand(this).registerThisCommand();
    }

    public static PlayerProfiles getInstance() {
        return instance;
    }

    public InventoryManager getInventoryManager(){
        return inventoryManager;
    }

    public CustomGUIManager getCustomGUIManager() {
        return customGUIManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }
}
