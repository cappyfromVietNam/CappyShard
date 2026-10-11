package com.cappy.shards.listeners;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    // Cache for Anti-VPN checks (IP -> isVPN)
    private final ConcurrentHashMap<String, Boolean> vpnCache = new ConcurrentHashMap<>();

    public PlayerConnectionListener(CappyShards plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String ipAddress = event.getAddress().getHostAddress();

        // 1. Anti-Alt check
        if (plugin.getConfig().getBoolean("anti-alt.enabled", true)) {
            int max = plugin.getConfig().getInt("anti-alt.max-accounts-per-ip", 3);
            AtomicInteger count = ipCount.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
            if (count.get() >= max) {
                String kickMsg = plugin.getMessage("anti-alt-kick", "<red>Connection rejected: Too many accounts on this IP.</red>");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatUtils.parse(kickMsg));
                return;
            }
        }

        // 2. Anti-VPN check
        if (plugin.getConfig().getBoolean("anti-vpn.enabled", true)) {
            Boolean cachedIsVpn = vpnCache.get(ipAddress);
            if (cachedIsVpn != null) {
                if (cachedIsVpn) {
                    String kickMsg = plugin.getMessage("anti-vpn-kick", "<red>Connection rejected: VPN/Proxy detected!</red>");
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatUtils.parse(kickMsg));
                    return;
                }
            } else {
                String apiUrl = plugin.getConfig().getString("anti-vpn.api-url", "").replace("%ip%", ipAddress);
                String checkKey = plugin.getConfig().getString("anti-vpn.check-key", "proxy");
                String checkValue = plugin.getConfig().getString("anti-vpn.check-value", "yes");

                if (!apiUrl.isEmpty()) {
                    try {
                        URL url = new URL(apiUrl);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");
                        con.setConnectTimeout(2500);
                        con.setReadTimeout(2500);

                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuilder content = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                        con.disconnect();

                        String response = content.toString();
                        boolean isVpn = response.contains("\"" + checkKey + "\": \"" + checkValue + "\"") ||
                                        response.contains("\"" + checkKey + "\":\"" + checkValue + "\"");

                        vpnCache.put(ipAddress, isVpn);

                        if (isVpn) {
                            String kickMsg = plugin.getMessage("anti-vpn-kick", "<red>Connection rejected: VPN/Proxy detected!</red>");
                            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatUtils.parse(kickMsg));
                            return;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to check VPN for IP: " + ipAddress + " - " + e.getMessage());
                    }
                }
            }
        }

        // Add to IP count if allowed
        if (plugin.getConfig().getBoolean("anti-alt.enabled", true)) {
            ipCount.computeIfAbsent(ipAddress, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().loadData(player.getUniqueId()).thenAccept(data -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getOfflineRewardManager().processOfflineReward(player, data.getLastQuit());
                }
            });
        });
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
