package ru.liko.tacz_mechanics.data.manager;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ru.liko.tacz_mechanics.data.BulletParticles;
import ru.liko.tacz_mechanics.util.BlockDustRgb;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

public class BulletParticlesManager extends BaseDataManager<BulletParticles> {
    
    private static final Comparator<BulletParticles> COMPARATOR = Comparator
        .comparingInt(BulletParticles::getPriority)
        .thenComparing(p -> !p.getTarget().isEmpty() ? 0 : 1)
        .thenComparing(p -> {
            if (p instanceof BulletParticles.BlockParticles bp) {
                return !bp.blocks().isEmpty() ? 0 : 1;
            } else if (p instanceof BulletParticles.EntityParticles ep) {
                return !ep.entities().isEmpty() ? 0 : 1;
            }
            return 1;
        });
    
    public static final BulletParticlesManager INSTANCE = new BulletParticlesManager();
    
    private final List<ParticleEmitter> emitters = new ArrayList<>();
    private final List<DirectedBlockParticleEmitter> directedEmitters = new ArrayList<>();
    
    private BulletParticlesManager() {
        super("bullet_particles", COMPARATOR, BulletParticles.CODEC,
            "particle");
    }
    
    public void onLevelTick(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        Iterator<ParticleEmitter> iterator = emitters.iterator();
        while (iterator.hasNext()) {
            ParticleEmitter emitter = iterator.next();
            if (!emitter.dimension.equals(dimension)) {
                continue;
            }
            emitter.remainingDuration--;
            try {
                level.sendParticles(
                    emitter.options,
                    emitter.x,
                    emitter.y,
                    emitter.z,
                    emitter.count,
                    emitter.deltaX,
                    emitter.deltaY,
                    emitter.deltaZ,
                    emitter.speed
                );
            } catch (Exception e) {
                logger.warn("Failed to send particles", e);
            }
            if (emitter.remainingDuration <= 0) {
                iterator.remove();
            }
        }

        Iterator<DirectedBlockParticleEmitter> dirIterator = directedEmitters.iterator();
        while (dirIterator.hasNext()) {
            DirectedBlockParticleEmitter directed = dirIterator.next();
            if (!directed.dimension.equals(dimension)) {
                continue;
            }
            directed.remainingDuration--;
            try {
                directed.tick(level);
            } catch (Exception e) {
                logger.warn("Failed to send directed block particles", e);
            }
            if (directed.remainingDuration <= 0) {
                dirIterator.remove();
            }
        }
    }
    
    public void handleBlockParticle(BlockParticleType type, ServerLevel level, ResourceLocation weaponId, ResourceLocation ammoId, float damage, BlockHitResult result, BlockState state) {
        var particles = byType(BulletParticles.BlockParticles.class);
        
        BulletParticles.BlockParticles matchingBlockParticles = null;
        for (var entry : particles.entrySet()) {
            BulletParticles.BlockParticles blockParticles = entry.getValue();
            
            // Check target conditions
            if (!matchesTarget(blockParticles.target(), weaponId, ammoId, damage)) continue;
            
            // Check block conditions
            if (!blockParticles.blocks().isEmpty()) {
                boolean matches = blockParticles.blocks().stream()
                    .anyMatch(b -> b.test(level, result.getBlockPos(), state));
                if (!matches) continue;
            }
            
            matchingBlockParticles = blockParticles;
            logger.debug("Using block bullet particles: {}", entry.getKey());
            break;
        }

        if (matchingBlockParticles == null) return;
            
        // Get particles for this type
        List<BulletParticles.BlockParticleEntry> particleEntries = type.getParticles(matchingBlockParticles);
        
        for (var particleEntry : particleEntries) {
            if (!matchesTarget(particleEntry.target(), weaponId, ammoId, damage)) continue;
            
            if (!particleEntry.blocks().isEmpty()) {
                boolean matches = particleEntry.blocks().stream()
                    .anyMatch(b -> b.test(level, result.getBlockPos(), state));
                if (!matches) continue;
            }
            
            String particleString = particleEntry.particle();
            // Replace {block} placeholder with actual block ID
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            particleString = particleString.replace("{block}", blockId.toString());
            particleString = particleString.replace("%s", blockId.toString());
            if (particleString.equals("minecraft:block")) {
                particleString = "minecraft:block " + blockId;
            }
            particleString = expandBlockHitPlaceholders(level, result.getBlockPos(), state, particleString);

            if (particleEntry.delta().type() == BulletParticles.CoordinatesType.NORMAL) {
                summonDirectedBlockParticles(level, result, particleEntry, particleString, state);
            } else {
                summonParticle(level, result.getLocation(), particleEntry.position(),
                    particleEntry.delta(), particleEntry.speed(), particleEntry.count(),
                    particleEntry.duration(), particleString, state);
            }
        }
    }
    
    public void handleEntityParticle(EntityParticleType type, ServerLevel level, ResourceLocation weaponId, ResourceLocation ammoId, float damage, Vec3 location, Entity target) {
        var particles = byType(BulletParticles.EntityParticles.class);
        
        BulletParticles.EntityParticles matchingEntityParticles = null;
        for (var entry : particles.entrySet()) {
            BulletParticles.EntityParticles entityParticles = entry.getValue();
            
            // Check target conditions
            if (!matchesTarget(entityParticles.target(), weaponId, ammoId, damage)) continue;
            
            // Check entity conditions
            if (!entityParticles.entities().isEmpty()) {
                boolean matches = entityParticles.entities().stream()
                    .anyMatch(e -> e.test(target));
                if (!matches) continue;
            }
            
            matchingEntityParticles = entityParticles;
            logger.debug("Using entity bullet particles: {}", entry.getKey());
            break;
        }

        if (matchingEntityParticles == null) return;
            
        // Get particles for this type
        List<BulletParticles.EntityParticleEntry> particleEntries = type.getParticles(matchingEntityParticles);
        
        for (var particleEntry : particleEntries) {
            if (!matchesTarget(particleEntry.target(), weaponId, ammoId, damage)) continue;
            
            if (!particleEntry.entities().isEmpty()) {
                boolean matches = particleEntry.entities().stream()
                    .anyMatch(e -> e.test(target));
                if (!matches) continue;
            }
            
            summonParticle(level, location, particleEntry.position(),
                particleEntry.delta(), particleEntry.speed(), particleEntry.count(),
                particleEntry.duration(), particleEntry.particle(), null);
        }
    }
    
    /**
     * Block debris along the impact face normal. Uses {@code count=0} particle packets so velocity matches the face normal
     * (see {@link net.minecraft.client.multiplayer.ClientPacketListener#handleParticleEvent}).
     */
    private void summonDirectedBlockParticles(
        ServerLevel level,
        BlockHitResult result,
        BulletParticles.BlockParticleEntry particleEntry,
        String particleString,
        BlockState hitState
    ) {
        try {
            ParticleOptions options = parseParticle(particleString, level, hitState);
            if (options == null) {
                return;
            }
            Vec3 pos = calculateCoordinates(result.getLocation(), particleEntry.position());
            Vec3 outward = Vec3.atLowerCornerOf(result.getDirection().getNormal());
            pos = pos.add(outward.scale(0.06));
            BulletParticles.Coordinates d = particleEntry.delta();
            double speedMul = particleEntry.speed() > 1.0e-6 ? particleEntry.speed() : 1.0;
            double forward = d.x() * speedMul;
            double spreadU = d.y();
            double spreadV = d.z();
            DirectedBlockParticleEmitter emitter = new DirectedBlockParticleEmitter(
                level.dimension(),
                options,
                pos.x,
                pos.y,
                pos.z,
                result.getDirection(),
                forward,
                spreadU,
                spreadV,
                particleEntry.count(),
                particleEntry.duration()
            );
            directedEmitters.add(emitter);
        } catch (Exception e) {
            logger.warn("Failed to spawn directed block particles: {}", particleString, e);
        }
    }

    private void summonParticle(ServerLevel level, Vec3 position, BulletParticles.Coordinates posCoords,
                                BulletParticles.Coordinates deltaCoords, double speed, int count,
                                int duration, String particleString, @Nullable BlockState hitBlockState) {
        try {
            ParticleOptions options = parseParticle(particleString, level, hitBlockState);
            if (options == null) return;

            Vec3 coords = calculateCoordinates(position, posCoords);
            Vec3 delta = calculateDelta(deltaCoords);

            emitters.add(new ParticleEmitter(
                level.dimension(),
                options,
                coords.x, coords.y, coords.z,
                delta.x, delta.y, delta.z,
                speed, count, duration
            ));
        } catch (Exception e) {
            logger.warn("Failed to parse particle: {}", particleString, e);
        }
    }
    
    private static String expandBlockHitPlaceholders(ServerLevel level, BlockPos pos, BlockState state, String particleString) {
        if (!particleString.contains("%")) return particleString;
        String s = particleString;
        if (s.contains("%r") || s.contains("%g") || s.contains("%b") || s.contains("%pr")) {
            float[] rgb = BlockDustRgb.rgb(level, pos, state);
            float[] puff = BlockDustRgb.puffRgb(rgb);
            s = s.replace("%r", fmt(rgb[0]))
                .replace("%g", fmt(rgb[1]))
                .replace("%b", fmt(rgb[2]))
                .replace("%pr", fmt(puff[0]))
                .replace("%pg", fmt(puff[1]))
                .replace("%pb", fmt(puff[2]));
        }
        return s;
    }

    private static String fmt(float v) {
        return String.format(Locale.US, "%.5f", v);
    }

    private ParticleOptions parseParticle(String particleString, ServerLevel level, @Nullable BlockState hitBlockState) {
        // Legacy: "minecraft:block <namespace:id>" используем только из bundled JSON после подстановки %s
        String blockPrefix = "minecraft:block ";
        if (particleString.startsWith(blockPrefix)) {
            String blockIdStr = particleString.substring(blockPrefix.length()).trim();
            ResourceLocation parsedId = ResourceLocation.tryParse(blockIdStr.split("\\s")[0]);
            if (parsedId != null) {
                Block block = BuiltInRegistries.BLOCK.get(parsedId);
                if (block != null && block != Blocks.AIR) {
                    if (hitBlockState != null && hitBlockState.getBlock() == block) {
                        return new BlockParticleOption(ParticleTypes.BLOCK, hitBlockState);
                    }
                    return new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());
                }
            }
        }
        try {
            return ParticleArgument.readParticle(new StringReader(particleString), level.registryAccess());
        } catch (CommandSyntaxException e) {
            logger.warn("Failed to parse particle string: {}", particleString, e);
            return null;
        }
    }
    
    private Vec3 calculateCoordinates(Vec3 base, BulletParticles.Coordinates coords) {
        return switch (coords.type()) {
            case ABSOLUTE -> new Vec3(coords.x(), coords.y(), coords.z());
            case RELATIVE, LOCAL, NORMAL -> new Vec3(base.x + coords.x(), base.y + coords.y(), base.z + coords.z());
        };
    }
    
    private Vec3 calculateDelta(BulletParticles.Coordinates coords) {
        if (coords.type() == BulletParticles.CoordinatesType.NORMAL) {
            logger.warn("Particle delta type 'normal' only applies to block impacts; using zero spread");
            return Vec3.ZERO;
        }
        return new Vec3(coords.x(), coords.y(), coords.z());
    }

    private static Vec3 randomVelocityAlongNormal(RandomSource random, Direction face, double forward, double spreadU, double spreadV) {
        Vec3 n = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 t = n.y * n.y < 1.0e-8 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 u = n.cross(t).normalize();
        Vec3 v = n.cross(u).normalize();
        double gu = (random.nextDouble() - 0.5) * 2.0 * spreadU;
        double gv = (random.nextDouble() - 0.5) * 2.0 * spreadV;
        return n.scale(forward).add(u.scale(gu)).add(v.scale(gv));
    }
    
    public enum BlockParticleType {
        HIT(BulletParticles.BlockParticles::hit),
        PIERCE(BulletParticles.BlockParticles::pierce),
        BREAK(BulletParticles.BlockParticles::breakParticle),
        RICOCHET(BulletParticles.BlockParticles::ricochet);
        
        private final Function<BulletParticles.BlockParticles, List<BulletParticles.BlockParticleEntry>> getter;
        
        BlockParticleType(Function<BulletParticles.BlockParticles, List<BulletParticles.BlockParticleEntry>> getter) {
            this.getter = getter;
        }
        
        public List<BulletParticles.BlockParticleEntry> getParticles(BulletParticles.BlockParticles particles) {
            return getter.apply(particles);
        }
    }
    
    public enum EntityParticleType {
        HIT(BulletParticles.EntityParticles::hit),
        PIERCE(BulletParticles.EntityParticles::pierce),
        KILL(BulletParticles.EntityParticles::kill);
        
        private final Function<BulletParticles.EntityParticles, List<BulletParticles.EntityParticleEntry>> getter;
        
        EntityParticleType(Function<BulletParticles.EntityParticles, List<BulletParticles.EntityParticleEntry>> getter) {
            this.getter = getter;
        }
        
        public List<BulletParticles.EntityParticleEntry> getParticles(BulletParticles.EntityParticles particles) {
            return getter.apply(particles);
        }
    }
    
    private static final class DirectedBlockParticleEmitter {
        final ResourceKey<Level> dimension;
        final ParticleOptions options;
        final double x;
        final double y;
        final double z;
        final Direction face;
        final double forward;
        final double spreadU;
        final double spreadV;
        final int countPerTick;
        int remainingDuration;

        DirectedBlockParticleEmitter(
            ResourceKey<Level> dimension,
            ParticleOptions options,
            double x,
            double y,
            double z,
            Direction face,
            double forward,
            double spreadU,
            double spreadV,
            int countPerTick,
            int durationTicks
        ) {
            this.dimension = dimension;
            this.options = options;
            this.x = x;
            this.y = y;
            this.z = z;
            this.face = face;
            this.forward = forward;
            this.spreadU = spreadU;
            this.spreadV = spreadV;
            this.countPerTick = countPerTick;
            this.remainingDuration = durationTicks;
        }

        void tick(ServerLevel level) {
            RandomSource random = level.random;
            for (int i = 0; i < this.countPerTick; i++) {
                Vec3 vel = randomVelocityAlongNormal(random, this.face, this.forward, this.spreadU, this.spreadV);
                level.sendParticles(this.options, this.x, this.y, this.z, 0, vel.x, vel.y, vel.z, 1.0);
            }
        }
    }

    private static class ParticleEmitter {
        final ResourceKey<Level> dimension;
        final ParticleOptions options;
        final double x, y, z;
        final double deltaX, deltaY, deltaZ;
        final double speed;
        final int count;
        int remainingDuration;

        ParticleEmitter(ResourceKey<Level> dimension, ParticleOptions options, double x, double y, double z,
                        double deltaX, double deltaY, double deltaZ,
                        double speed, int count, int duration) {
            this.dimension = dimension;
            this.options = options;
            this.x = x;
            this.y = y;
            this.z = z;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.deltaZ = deltaZ;
            this.speed = speed;
            this.count = count;
            this.remainingDuration = duration;
        }
    }
}
