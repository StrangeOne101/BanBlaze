package com.strangeone101.abilities;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TimeUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BanBlaze extends AvatarAbility implements AddonAbility {

    @Attribute(Attribute.COOLDOWN)
    private static long cooldown = 10_000L;

    @Attribute(Attribute.CHARGE_DURATION)
    private static long chargetime = 3_000L;

    @Attribute(Attribute.DAMAGE)
    private static double damage = 50;

    @Attribute(Attribute.RANGE)
    private static int range = 30;

    @Attribute(Attribute.SPEED)
    private static double speed = 0.5;

    private static double particleChance = 15D;

    private static boolean canKill = true;

    private boolean releasing = false;
    private Location startLoc;
    private double distance = 0;

    public BanBlaze(Player player) {
        super(player);

        setValues();

        start();
    }

    @Override
    public void progress() {
        if (!getBendingPlayer().canBendIgnoreCooldowns(this)) {
            remove();
            return;
        }

        if (getPlayer().isSneaking() && !releasing) {
            if (System.currentTimeMillis() > getStartTime() + chargetime) {

                Location loc = getPlayer().getEyeLocation().clone().add(getPlayer().getEyeLocation().getDirection().clone().normalize().multiply(1.5));
                ParticleEffect.SMOKE_NORMAL.display(loc, 1);
            }
            return;
        } else if (!releasing) {
            if (System.currentTimeMillis() > getStartTime() + chargetime) {
                releasing = true;
                startLoc = getPlayer().getLocation();
            } else {
                remove();
            }
            return;
        }

        distance += speed;

        //Loop through the players in the world and check if they are on the edge of the border of this ability
        for (Player player : getPlayer().getWorld().getEntitiesByClass(Player.class)) {
            double distanceMin = distance - (speed / 2);
            double distanceMax = distance + (speed / 2);
            double distanceMin2 = distanceMin * distanceMin;
            double distanceMax2 = distanceMax * distanceMax;

            double x = startLoc.getX();
            double y = startLoc.getY();
            double z = startLoc.getZ();

            if (!player.isDead() && !player.hasPermission("bending.ability.banwave.immune")) {
                Location loc = player.getLocation();
                double px = loc.getX();
                double pz = loc.getZ();
                double py = loc.getY();

                double dx = Math.abs(x - px);
                double dy = Math.abs(y - py);
                double dz = Math.abs(z - pz);

                double dx2 = dx * dx;
                double dz2 = dz * dz;
                double dxz2 = dx2 + dz2;

                if (dxz2 > distanceMin2 && dxz2 < distanceMax2 && dy <= 6) { //Players are on the border of the currently expanding circle
                    getPlayer().getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 1F);
                    getPlayer().getWorld().spawnParticle(Particle.FLASH, player.getLocation(), 3);
                    getPlayer().getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getEyeLocation(), 100, 0.5, 0.5, 0.5);
                    getPlayer().getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 30, 0.5, 0.5, 0.5);
                    DamageHandler.damageEntity(player, getPlayer(), damage, this, true);
                }
            }
        }

        double circ = 2 * Math.PI * distance; //How many blocks in the circumference

        int soundsThisTick = 0;
        for (int i = 0; i < circ; i++) {
            int particle = Math.random() * 100 < particleChance ? 1 : (Math.random() * 100 < particleChance ? 2 : 0);
            if (particle > 0) {
                double angle = 360 / circ * i;
                double r_angle = Math.toRadians(angle);

                double x = Math.cos(r_angle) * distance;
                double z = Math.sin(r_angle) * distance;

                Location l = startLoc.clone().add(x, 0, z);

                for (int j = -5; j < 5; j++) {
                    Block b1 = l.clone().add(0, j, 0).getBlock();
                    Block b2 = l.clone().add(0, j + 1, 0).getBlock();

                    if (b1.getType().isSolid() && !b2.getType().isSolid()) {
                        if (particle == 1) {
                            ParticleEffect.BARRIER.display(b2.getLocation().add(0, 0.5,0), 1);
                        } else {
                            ParticleEffect.FLAME.display(b2.getLocation().add(0.5, 0.5,0.5), 8, 0.25, 0.25, 0.25);
                        }

                    }
                }
                if (Math.random() * 100 < particleChance / 2 && soundsThisTick < 1) {
                    getPlayer().getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 0.01F);
                    soundsThisTick++;
                }
            }
        }

        if (distance > range) {
            remove();
            getBendingPlayer().addCooldown(this);
            return;
        }

    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "BanBlaze";
    }

    @Override
    public Location getLocation() {
        return startLoc == null ? getPlayer().getLocation() : startLoc;
    }

    @Override
    public String getDescription() {
        return "\u00A7cBan all players in a blaze of glory! A true display of admin power abuse!";
    }

    @Override
    public String getInstructions() {
        return "\u00A7cHold Sneak until charged then release to BAN BAN BAN!";
    }

    @Override
    public void load() {
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new BlazeBanListener(), ProjectKorra.plugin);

        ProjectKorra.plugin.getLogger().info(getName() + " v" + getVersion() + " by " + getAuthor() + " loaded!");

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.StrangeOne101.BanBlaze.ChargeTime", chargetime);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.StrangeOne101.BanBlaze.Range", range);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.StrangeOne101.BanBlaze.Speed", speed);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.StrangeOne101.BanBlaze.Cooldown", cooldown);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.StrangeOne101.BanBlaze.Damage", damage);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.StrangeOne101.BanBlaze.ParticleChance", particleChance);

        ConfigManager.defaultConfig.save();

        ConfigManager.languageConfig.get().addDefault("Abilities.Avatar.BanBlaze.DeathMessage", "{victim} was \u00A7cBANNED \u00A7rby {attacker}");

        ConfigManager.languageConfig.save();
    }

    @Override
    public void stop() {

    }

    public void setValues() {
        chargetime = ConfigManager.defaultConfig.get().getLong("ExtraAbilities.StrangeOne101.BanBlaze.ChargeTime");
        range = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.StrangeOne101.BanBlaze.Range");
        damage = ConfigManager.defaultConfig.get().getDouble("ExtraAbilities.StrangeOne101.BanBlaze.Damage");
        speed = ConfigManager.defaultConfig.get().getDouble("ExtraAbilities.StrangeOne101.BanBlaze.Speed");
        cooldown = ConfigManager.defaultConfig.get().getLong("ExtraAbilities.StrangeOne101.BanBlaze.Cooldown");
        particleChance = ConfigManager.defaultConfig.get().getDouble("ExtraAbilities.StrangeOne101.BanBlaze.ParticleChance");
    }

    @Override
    public String getAuthor() {
        return "StrangeOne101";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
