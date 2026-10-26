package com.nordfjell.norddeaths;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class NordDeathsPlugin extends JavaPlugin implements Listener {
    private static final long DAMAGE_CONTEXT_MAX_AGE_NANOS = 10_000_000_000L;
    private volatile Map<String,List<String>> messages = Map.of();
    private final Map<UUID, RecentDamage> recentDamage = new ConcurrentHashMap<>();
    private void loadMessages() {
        reloadConfig();
        Map<String,List<String>> next = new HashMap<>();
        var section = getConfig().getConfigurationSection("messages");
        if (section != null) for (String key : section.getKeys(false))
            next.put(key,List.copyOf(getConfig().getStringList("messages." + key)));
        messages = Map.copyOf(next);
    }
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("NordDeaths enabled. NordChat can still apply per-player death-message visibility.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getFinalDamage() <= 0) return;
        recentDamage.put(player.getUniqueId(), new RecentDamage(classify(event), System.nanoTime()));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        RecentDamage recent = recentDamage.remove(event.getPlayer().getUniqueId());
        if (!event.getShowDeathMessages() || event.deathMessage() == null) return;
        DeathContext context = recent != null && System.nanoTime() - recent.observedAtNanos() <= DAMAGE_CONTEXT_MAX_AGE_NANOS
                ? recent.context() : new DeathContext("generic", "unknown");
        Map<String,List<String>> current = messages;
        List<String> options = current.getOrDefault(context.category(),List.of());
        if (options.isEmpty()) options = current.getOrDefault("generic",List.of());
        if (options.isEmpty()) options = List.of("{player} died.");
        String template = options.get(ThreadLocalRandom.current().nextInt(options.size()));
        String resolved = template
                .replace("{player}", event.getPlayer().getName())
                .replace("{killer}", context.killer());
        event.deathMessage(Component.text(resolved, NamedTextColor.RED));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        recentDamage.remove(event.getPlayer().getUniqueId());
    }

    private DeathContext classify(EntityDamageEvent damage) {
        if (damage instanceof EntityDamageByEntityEvent entityDamage) {
            Entity attacker = entityDamage.getDamageSource().getCausingEntity();
            if (attacker == null) attacker = entityDamage.getDamager();
            if (attacker instanceof Player killer) {
                return new DeathContext("player", killer.getName());
            }
            String mob = readable(attacker.getType().getKey().getKey());
            return new DeathContext("mob", mob);
        }
        return switch (damage.getCause()) {
            case FALL -> new DeathContext("fall", "gravity");
            case FIRE, FIRE_TICK, HOT_FLOOR, CAMPFIRE -> new DeathContext("fire", "fire");
            case LAVA -> new DeathContext("lava", "lava");
            case DROWNING -> new DeathContext("drowning", "water");
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> new DeathContext("explosion", "an explosion");
            case VOID -> new DeathContext("void", "the void");
            case SUFFOCATION -> new DeathContext("suffocation", "a wall");
            case FREEZE -> new DeathContext("freezing", "the cold");
            default -> new DeathContext("generic", "unknown");
        };
    }

    private String readable(String key) {
        String[] words = key.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(word);
        }
        return result.toString();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            getServer().getGlobalRegionScheduler().execute(this,() -> {
                loadMessages();
                Component message=Component.text("NordDeaths configuration reloaded.");
                if (sender instanceof Player player) player.getScheduler().execute(this,() -> player.sendMessage(message),null,1L);
                else sender.sendMessage(message);
            });
        } else {
            sender.sendMessage(Component.text("Usage: /norddeaths reload"));
        }
        return true;
    }

    private record DeathContext(String category, String killer) {}
    private record RecentDamage(DeathContext context, long observedAtNanos) {}
}
