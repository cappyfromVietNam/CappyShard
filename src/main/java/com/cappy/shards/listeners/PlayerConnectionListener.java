package com.cappy.shards.listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.cappy.shards.CappyShards;
import com.cappy.shards.utils.ChatUtils;

public class PlayerConnectionListener implements Listener {

    private final CappyShards plugin;
    private final ConcurrentHashMap<String, AtomicInteger> ipCount = new ConcurrentHashMap<>();
    private YamlConfiguration messages;

    public PlayerConnectionListener(CappyShards plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        File msgFile = new File(plugin.getDataFolder(), "messages.yml");
        if (msgFile.exists()) {
            messages = YamlConfiguration.loadConfiguration(msgFile);
        }
    }
    
    private String getMessage(String key, String def) {
        if (messages == null) return def;
        return messages.getString(key, def);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String ipAddress = event.getAddress().getHostAddress();

        // 1. Anti-Alt check
        if (plugin.getConfig().getBoolean("anti-alt.enabled", true)) {
            int max = plugin.getConfig().getInt("anti-alt.max-accounts-per-ip", 3);
            AtomicInteger count = ipCount.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
            if (count.get() >= max) {
                String kickMsg = getMessage("anti-alt-kick", "<red>Connection rejected: Too many accounts on this IP.</red>");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatUtils.parse(kickMsg));
                return;
            }
        }

        // 2. Anti-VPN check
        if (plugin.getConfig().getBoolean("anti-vpn.enabled", true)) {
            String apiUrl = plugin.getConfig().getString("anti-vpn.api-url", "").replace("%ip%", ipAddress);
            String checkKey = plugin.getConfig().getString("anti-vpn.check-key", "proxy");
            String checkValue = plugin.getConfig().getString("anti-vpn.check-value", "yes");

            if (!apiUrl.isEmpty()) {
                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(3000);
                    con.setReadTimeout(3000);

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    con.disconnect();

                    String response = content.toString();
                    if (response.contains("\"" + checkKey + "\": \"" + checkValue + "\"") ||
                        response.contains("\"" + checkKey + "\":\"" + checkValue + "\"")) {
                        String kickMsg = getMessage("anti-vpn-kick", "<red>Connection rejected: VPN/Proxy detected!</red>");
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatUtils.parse(kickMsg));
                        return;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to check VPN for IP: " + ipAddress + " - " + e.getMessage());
                }
            }
        }
        
        // Add to IP count if allowed
        if (plugin.getConfig().getBoolean("anti-alt.enabled", true)) {
            ipCount.get(ipAddress).incrementAndGet();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().loadData(player.getUniqueId());
        plugin.getAfkRewardManager().addPlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().saveData(player.getUniqueId());
        plugin.getAfkRewardManager().removePlayer(player.getUniqueId());
        
        if (plugin.getConfig().getBoolean("anti-alt.enabled", true)) {
            String ipAddress = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
            if (ipAddress != null) {
                AtomicInteger count = ipCount.get(ipAddress);
                if (count != null) {
                    if (count.decrementAndGet() <= 0) {
                        ipCount.remove(ipAddress);
                    }
                }
            }
        }
    }
}

