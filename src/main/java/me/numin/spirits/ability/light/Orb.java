package me.numin.spirits.ability.light;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import me.numin.spirits.Spirits;
import me.numin.spirits.Methods;
import me.numin.spirits.Methods.SpiritType;
import me.numin.spirits.ability.api.LightAbility;

public class Orb extends LightAbility implements AddonAbility {

    private Location location;
    private Location targetLoc;
    private long duration;
    private long time;
    private long cooldown;
    private long chargeTime;
    private boolean isCharged;
    private boolean checkEntities;
    private boolean registerOrbLoc;
    private boolean progressExplosion;
    private boolean playDormant;
    private boolean requireGround;
    private int plantRange;
    private int blindDuration;
    private int nauseaDuration;
    private int potionAmp;
    private double detonateRange;
    private double effectRange;
    private double damage;

    public Orb(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        setFields();
        time = System.currentTimeMillis();
        start();
    }

    private void setFields() {
        this.cooldown = Spirits.plugin.getConfig().getLong("Abilities.Spirits.LightSpirit.Orb.Cooldown");
        this.chargeTime = Spirits.plugin.getConfig().getLong("Abilities.Spirits.LightSpirit.Orb.ChargeTime");
        this.duration = Spirits.plugin.getConfig().getLong("Abilities.Spirits.LightSpirit.Orb.Duration");
        this.damage = Spirits.plugin.getConfig().getDouble("Abilities.Spirits.LightSpirit.Orb.Damage");
        this.plantRange = Spirits.plugin.getConfig().getInt("Abilities.Spirits.LightSpirit.Orb.PlaceRange");
        this.detonateRange = Spirits.plugin.getConfig().getInt("Abilities.Spirits.LightSpirit.Orb.DetonateRange");
        this.effectRange = Spirits.plugin.getConfig().getInt("Abilities.Spirits.LightSpirit.Orb.EffectRange");
        this.blindDuration = Spirits.plugin.getConfig().getInt("Abilities.Spirits.LightSpirit.Orb.BlindnessDuration");
        this.nauseaDuration = Spirits.plugin.getConfig().getInt("Abilities.Spirits.LightSpirit.Orb.NauseaDuration");
        this.potionAmp = Spirits.plugin.getConfig().getInt("Abilities.Spirits.LightSpirit.Orb.PotionPower");
        this.requireGround = Spirits.plugin.getConfig().getBoolean("Abilities.Spirits.LightSpirit.Orb.RequireGround");
        this.location = player.getLocation();
        this.isCharged = false;
        this.checkEntities = false;
        this.registerOrbLoc = true;
        this.progressExplosion = false;
        this.playDormant = false;
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || GeneralMethods.isRegionProtectedFromBuild(this, location)) {
            remove();
            return;
        }
        if (!isCharged) {
            if (player.isSneaking()) {
                if (System.currentTimeMillis() > time + chargeTime) {
                    isCharged = true;
                }
            } else {
                remove();
                return;
            }
        } else {
            if (player.isSneaking() && !playDormant) {
                Location eyeLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3));
                ParticleEffect.CRIT.display(eyeLoc, 0, 0, 0, 0, 2);
            } else {
                playDormant = true;
                if (registerOrbLoc) {
                    this.targetLoc = GeneralMethods.getTargetedLocation(player, plantRange);
                    if (requireGround) {
                        if (!canSpawn(targetLoc)) {
                            remove();
                            return;
                        }
                    }
                    registerOrbLoc = false;
                }
                displayOrb(targetLoc);
            }
            explodeOrb();
        }
    }

    private void displayOrb(Location location) {
        if (playDormant) {
            progressExplosion = false;
            ParticleEffect.ENCHANTMENT_TABLE.display(location, 3, 1, 3, 0, 1);
            ParticleEffect.END_ROD.display(location, 0, 0, 0, 0, 2);
            ParticleEffect.CRIT_MAGIC.display(location, 0.2F, 0.2F, 0.2F, 0, 3);
            if (player.isSneaking() && hasOrb()) {
                progressExplosion = true;
                playDormant = false;
            }
        }
        if (System.currentTimeMillis() > time + duration) {
            playDormant = false;
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, location, 30, 0, 0, 0, 0.09);
            bPlayer.addCooldown(this);
            remove();
            return;
        } else {
            bPlayer.addCooldown(this, duration);
            checkEntities = true;
        }
    }
    private void explodeOrb() {
        if (checkEntities) {
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(targetLoc, detonateRange)) {
                if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                    progressExplosion = true;
                    playDormant = false;
                }
            }
        }
        if (progressExplosion) {
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, targetLoc, 70, 0.2, 0.2, 0.2, 0.4);
            ParticleEffect.END_ROD.display(targetLoc, 2, 3, 2, 0, 30);
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(targetLoc, effectRange)) {
                if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                    LivingEntity le = (LivingEntity)entity;
                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindDuration, potionAmp));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, nauseaDuration, potionAmp));
                    DamageHandler.damageEntity(entity, damage, this);
                }
            }
            bPlayer.addCooldown(this);
            remove();
            return;
        }
    }

    private boolean canSpawn(Location loc) {

        BlockFace[] faces = { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN };

        for (BlockFace face : faces) {
            if (GeneralMethods.isSolid(loc.getBlock().getRelative(face))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOrb() {
        return bPlayer.getBoundAbility().equals(CoreAbility.getAbility(Orb.class));
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return targetLoc;
    }

    @Override
    public String getName() {
        return "Orb";
    }

    @Override
    public String getDescription() {
        return Methods.setSpiritDescription(SpiritType.LIGHT, "Offense") +
                Spirits.plugin.getConfig().getString("Language.Abilities.LightSpirit.Orb.Description");
    }

    @Override
    public String getInstructions() {
        return Methods.getSpiritColor(SpiritType.LIGHT) +
                Spirits.plugin.getConfig().getString("Language.Abilities.LightSpirit.Orb.Instructions");
    }

    @Override
    public String getAuthor() {

        return Methods.getSpiritColor(SpiritType.LIGHT) + "" +
                Methods.getAuthor();
    }

    @Override
    public String getVersion() {

        return Methods.getSpiritColor(SpiritType.LIGHT) +
                Methods.getVersion();
    }

    @Override
    public boolean isEnabled() {
        return Spirits.plugin.getConfig().getBoolean("Abilities.Spirits.LightSpirit.Orb.Enabled");
    }

    @Override
    public boolean isExplosiveAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isIgniteAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }

}