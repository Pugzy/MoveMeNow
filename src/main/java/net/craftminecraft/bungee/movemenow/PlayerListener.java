package net.craftminecraft.bungee.movemenow;

import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Comparator;
import java.util.Map;

public class PlayerListener implements Listener {

    MoveMeNow plugin;

    public PlayerListener(MoveMeNow plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerKickEvent(ServerKickEvent ev) {
        // Protection against NullPointerException

        ServerInfo kickedFrom = null;

        if (ev.getPlayer().getServer() != null) {
            kickedFrom = ev.getPlayer().getServer().getInfo();
        } else if (this.plugin.getProxy().getReconnectHandler() != null) {// If first server and recohandler
            kickedFrom = this.plugin.getProxy().getReconnectHandler().getServer(ev.getPlayer());
        } else { // If first server and no recohandler
            kickedFrom = AbstractReconnectHandler.getForcedHost(ev.getPlayer().getPendingConnection());
            if (kickedFrom == null) // Can still be null if vhost is null...
            {
                kickedFrom = ProxyServer.getInstance().getServerInfo(ev.getPlayer().getPendingConnection().getListener().getDefaultServer());
            }
        }

        // Custom check for regex matching server or fallback
        Map<String, ServerInfo> servers = ProxyServer.getInstance().getServers();
        ServerInfo kickTo = servers.entrySet()
                .stream()
                .filter(e -> e.getValue().getName().matches(plugin.getConfig().getString("server_regex")))
                .sorted(Comparator.comparingInt(s -> s.getValue().getPlayers().size()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(this.plugin.getProxy().getServerInfo(plugin.getConfig().getString("server_fallback")));

        // Avoid the loop
        if (kickedFrom != null && kickedFrom.equals(kickTo)) {
            return;
        }

        ev.setCancelled(true);
        ev.setCancelServer(kickTo);
    }
}