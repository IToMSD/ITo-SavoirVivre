package pl.polsatgranie.itomsd.savoirVivre;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SavoirVivre extends JavaPlugin implements Listener {

    private HashMap<UUID, UUID> joinedPlayers = new HashMap<>();
    private HashMap<UUID, UUID> deathTimestamps = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        new Metrics(this, 22938);
        this.getLogger().info("""
                
                ------------------------------------------------------------
                |                                                          |
                |      _  _______        __     __    _____   ____         |
                |     | ||___ ___|      |  \\   /  |  / ____| |  _ \\        |
                |     | |   | |   ___   | |\\\\ //| | | (___   | | \\ \\       |
                |     | |   | |  / _ \\  | | \\_/ | |  \\___ \\  | |  ) )      |
                |     | |   | | | (_) | | |     | |  ____) | | |_/ /       |
                |     |_|   |_|  \\___/  |_|     |_| |_____/  |____/        |
                |                                                          |
                |                                                          |
                ------------------------------------------------------------
                |                 +==================+                     |
                |                 |    SavoirVivre   |                     |
                |                 |------------------|                     |
                |                 |        1.0       |                     |
                |                 |------------------|                     |
                |                 |  PolsatGraniePL  |                     |
                |                 +==================+                     |
                ------------------------------------------------------------
                """);
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (config.getBoolean("joinplayer.enabled")) {
            Player player = event.getPlayer();
            if (config.getBoolean("joinplayer.must_first_join") && player.hasPlayedBefore()) {
                return;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!Objects.equals(p.getUniqueId(), player.getUniqueId()))
                    joinedPlayers.put(p.getUniqueId(), player.getUniqueId());
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        joinedPlayers.remove(p.getUniqueId());
                    }
                }
            }.runTaskLater(this, config.getInt("joinplayer.time") * 20L);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (config.getBoolean("deathplayer.enabled")) {
            Player player = event.getEntity();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!Objects.equals(p.getUniqueId(), player.getUniqueId()))
                    deathTimestamps.put(p.getUniqueId(), player.getUniqueId());

            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        deathTimestamps.remove(p.getUniqueId());
                    }
                }
            }.runTaskLater(this, config.getInt("deathplayer.time") * 20L);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();

        if (config.getBoolean("joinplayer.enabled") && joinedPlayers.containsKey(player.getUniqueId())) {
            List<String> messages = config.getStringList("joinplayer.messages");
            if (messages.contains(message)) {
                String command = config.getString("joinplayer.command").replace("%player%", player.getName());
                executeCommand(command);
                joinedPlayers.remove(player.getUniqueId());
            }
        }

        if (config.getBoolean("deathplayer.enabled") && deathTimestamps.containsKey(player.getUniqueId())) {
            List<String> messages = config.getStringList("deathplayer.messages");
            if (messages.contains(message)) {
                String command = config.getString("deathplayer.command").replace("%player%", player.getName());
                executeCommand(command);
                deathTimestamps.remove(player.getUniqueId());
            }
        }
    }

    private void executeCommand(String command) {
        Bukkit.getScheduler().runTask(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        joinedPlayers.remove(event.getPlayer().getUniqueId());
        deathTimestamps.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("svreload") || command.getName().equalsIgnoreCase("savoirvivrereload")) {
            if (!sender.hasPermission("itomsd.savoirvivre.admin")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no_permission")));
                return true;
            }
            reloadConfig();
            config = getConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("plugin_reloaded")));
        }
        return false;
    }
}
