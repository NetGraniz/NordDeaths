package com.nordfjell.norddeaths;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class NordDeathsPlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("NordDeaths enabled. NordChat can still apply per-player death-message visibility.");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        if (!event.getShowDeathMessages() || event.deathMessage() == null) return;
        DeathContext context = classify(event.getPlayer());
        List<String> options = getConfig().getStringList("messages." + context.category());
        if (options.isEmpty()) options = getConfig().getStringList("messages.generic");
        if (options.isEmpty()) options = List.of("{player} died.");
        String template = options.get(ThreadLocalRandom.current().nextInt(options.size()));
        String resolved = template
                .replace("{player}", event.getPlayer().getName())
                .replace("{killer}", context.killer());
        event.deathMessage(Component.text(resolved, NamedTextColor.RED));
    }

    private DeathContext classify(Player player) {
        EntityDamageEvent damage = player.getLastDamageCause();
        if (damage == null) return new DeathContext("generic", "unknown");
        if (damage instanceof EntityDamageByEntityEvent entityDamage) {
            Entity attacker = resolveAttacker(entityDamage.getDamager());
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

    private Entity resolveAttacker(Entity damager) {
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity entity) return entity;
        }
        return damager;
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
            reloadConfig();
            sender.sendMessage(Component.text("NordDeaths configuration reloaded."));
        } else {
            sender.sendMessage(Component.text("Usage: /norddeaths reload"));
        }
        return true;
    }

    private record DeathContext(String category, String killer) {}
}
