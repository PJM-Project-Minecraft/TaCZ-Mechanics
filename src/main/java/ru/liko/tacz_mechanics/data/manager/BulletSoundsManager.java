package ru.liko.tacz_mechanics.data.manager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ru.liko.tacz_mechanics.data.BulletSounds;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class BulletSoundsManager extends BaseDataManager<BulletSounds> {

    private static final Comparator<BulletSounds> COMPARATOR = Comparator
            .comparingInt(BulletSounds::getPriority)
            .thenComparing(s -> !s.getTarget().isEmpty() ? 0 : 1)
            .thenComparing(s -> {
                if (s instanceof BulletSounds.BlockSound bs) {
                    return !bs.blocks().isEmpty() ? 0 : 1;
                } else if (s instanceof BulletSounds.EntitySound es) {
                    return !es.entities().isEmpty() ? 0 : 1;
                }
                return 1;
            });

    public static final BulletSoundsManager INSTANCE = new BulletSoundsManager();

    private BulletSoundsManager() {
        super("bullet_sounds", COMPARATOR, BulletSounds.CODEC,
            "dirt", "metal", "stone", "wood");
    }

    /**
     * Checks if ricochet sound is defined for this block (metal, stone, etc.).
     * Used to only allow ricochet from hard surfaces, not dirt/wood.
     */
    public boolean hasRicochetForBlock(Level level, ResourceLocation weaponId, ResourceLocation ammoId,
            float damage, BlockHitResult result, BlockState state) {
        return findMatchingBlockSound(BlockSoundType.RICOCHET, level, weaponId, ammoId, damage, result, state) != null;
    }

    private BulletSounds.BlockSound findMatchingBlockSound(BlockSoundType type, Level level,
            ResourceLocation weaponId, ResourceLocation ammoId, float damage, BlockHitResult result, BlockState state) {
        var sounds = byType(BulletSounds.BlockSound.class);
        BulletSounds.BlockSound matchingBlockSound = null;
        for (var entry : sounds.entrySet()) {
            BulletSounds.BlockSound blockSound = entry.getValue();

            // Check target conditions
            if (!matchesTarget(blockSound.target(), weaponId, ammoId, damage))
                continue;

            // Check block conditions
            if (!blockSound.blocks().isEmpty()) {
                boolean matches = blockSound.blocks().stream()
                        .anyMatch(b -> b.test(level, result.getBlockPos(), state));
                if (!matches)
                    continue;
            }

            // Skip if this BlockSound has no sounds for the requested type
            if (type.getSounds(blockSound).isEmpty())
                continue;

            matchingBlockSound = blockSound;
            logger.debug("Using block bullet sounds: {}", entry.getKey());
            break;
        }

        return matchingBlockSound;
    }

    public void handleBlockSound(BlockSoundType type, Level level, ResourceLocation weaponId,
            ResourceLocation ammoId, float damage, BlockHitResult result, BlockState state) {
        BulletSounds.BlockSound matchingBlockSound = findMatchingBlockSound(type, level, weaponId, ammoId, damage, result, state);
        if (matchingBlockSound == null) {
            // Fallback only for plain impacts: if a block has no configured hit sound,
            // play its own vanilla hit sound so no surface is ever silent. Ricochet/pierce/
            // break are intentionally limited to configured materials, so they get no fallback.
            if (type == BlockSoundType.HIT) {
                playBlockHitFallback(level, result.getLocation(), state);
            }
            return;
        }

        // Get sounds for this type
        List<BulletSounds.BlockSoundEntry> soundEntries = type.getSounds(matchingBlockSound);

        for (var soundEntry : soundEntries) {
            if (!matchesTarget(soundEntry.target(), weaponId, ammoId, damage))
                continue;

            if (!soundEntry.blocks().isEmpty()) {
                boolean matches = soundEntry.blocks().stream()
                        .anyMatch(b -> b.test(level, result.getBlockPos(), state));
                if (!matches)
                    continue;
            }

            playSound(level, result.getLocation(), soundEntry.sound(), soundEntry.volume(), soundEntry.pitch(),
                    soundEntry.range());
        }
    }

    public void handleEntitySound(EntitySoundType type, ServerLevel level, ResourceLocation weaponId,
            ResourceLocation ammoId, float damage, Vec3 location, Entity target) {
        var sounds = byType(BulletSounds.EntitySound.class);

        BulletSounds.EntitySound matchingEntitySound = null;
        for (var entry : sounds.entrySet()) {
            BulletSounds.EntitySound entitySound = entry.getValue();

            // Check target conditions
            if (!matchesTarget(entitySound.target(), weaponId, ammoId, damage))
                continue;

            // Check entity conditions
            if (!entitySound.entities().isEmpty()) {
                boolean matches = entitySound.entities().stream()
                        .anyMatch(e -> e.test(target));
                if (!matches)
                    continue;
            }

            matchingEntitySound = entitySound;
            logger.debug("Using entity bullet sounds: {}", entry.getKey());
            break;
        }

        if (matchingEntitySound == null) return;

        // Get sounds for this type
        List<BulletSounds.EntitySoundEntry> soundEntries = type.getSounds(matchingEntitySound);

        for (var soundEntry : soundEntries) {
            if (!matchesTarget(soundEntry.target(), weaponId, ammoId, damage))
                continue;

            if (!soundEntry.entities().isEmpty()) {
                boolean matches = soundEntry.entities().stream()
                        .anyMatch(e -> e.test(target));
                if (!matches)
                    continue;
            }

            playSound(level, location, soundEntry.sound(), soundEntry.volume(), soundEntry.pitch(),
                    soundEntry.range());
        }
    }

    /** Namespace of this mod's bundled impact sounds (generic.hit.*). */
    private static final String FALLBACK_SOUND_NS = "tacz_mechanics";

    /**
     * Generic bullet-impact fallback for blocks with no {@link BulletSounds.BlockSound} config.
     * Picks one of the mod's own {@code generic.hit.*} sounds by the block's vanilla material
     * ({@link SoundType}) and plays it through the normal path, so every surface produces a loud,
     * material-appropriate gunshot impact — not the near-inaudible vanilla block "thunk".
     */
    private void playBlockHitFallback(Level level, Vec3 position, BlockState state) {
        String category = fallbackCategory(state.getSoundType());
        ResourceLocation soundId = ResourceLocation.fromNamespaceAndPath(FALLBACK_SOUND_NS, "generic.hit." + category);
        playSound(level, position, soundId, 1.0f, 1.0f, java.util.Optional.of(8.0f));
    }

    /** Maps a vanilla {@link SoundType} to one of the mod's generic.hit.* categories. */
    private static String fallbackCategory(net.minecraft.world.level.block.SoundType type) {
        net.minecraft.world.level.block.SoundType t = type;
        if (t == net.minecraft.world.level.block.SoundType.WOOD
                || t == net.minecraft.world.level.block.SoundType.BAMBOO
                || t == net.minecraft.world.level.block.SoundType.BAMBOO_WOOD
                || t == net.minecraft.world.level.block.SoundType.CHERRY_WOOD
                || t == net.minecraft.world.level.block.SoundType.NETHER_WOOD
                || t == net.minecraft.world.level.block.SoundType.LADDER
                || t == net.minecraft.world.level.block.SoundType.SCAFFOLDING) {
            return "wood";
        }
        if (t == net.minecraft.world.level.block.SoundType.METAL
                || t == net.minecraft.world.level.block.SoundType.COPPER
                || t == net.minecraft.world.level.block.SoundType.CHAIN
                || t == net.minecraft.world.level.block.SoundType.ANVIL
                || t == net.minecraft.world.level.block.SoundType.LANTERN
                || t == net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK) {
            return "metal";
        }
        if (t == net.minecraft.world.level.block.SoundType.GLASS
                || t == net.minecraft.world.level.block.SoundType.AMETHYST) {
            return "glass";
        }
        if (t == net.minecraft.world.level.block.SoundType.GRAVEL) {
            return "gravel";
        }
        if (t == net.minecraft.world.level.block.SoundType.SAND) {
            return "sand";
        }
        if (t == net.minecraft.world.level.block.SoundType.SNOW
                || t == net.minecraft.world.level.block.SoundType.POWDER_SNOW) {
            return "snow";
        }
        if (t == net.minecraft.world.level.block.SoundType.MUD
                || t == net.minecraft.world.level.block.SoundType.SOUL_SAND
                || t == net.minecraft.world.level.block.SoundType.SOUL_SOIL) {
            return "mud";
        }
        if (t == net.minecraft.world.level.block.SoundType.GRASS
                || t == net.minecraft.world.level.block.SoundType.WET_GRASS
                || t == net.minecraft.world.level.block.SoundType.WOOL
                || t == net.minecraft.world.level.block.SoundType.MOSS
                || t == net.minecraft.world.level.block.SoundType.ROOTS
                || t == net.minecraft.world.level.block.SoundType.CROP
                || t == net.minecraft.world.level.block.SoundType.NETHER_SPROUTS
                || t == net.minecraft.world.level.block.SoundType.HONEY_BLOCK
                || t == net.minecraft.world.level.block.SoundType.SLIME_BLOCK) {
            return "grass";
        }
        // Stone-likes (stone, deepslate, calcite, tuff, netherrack, basalt, bricks, ...) and
        // everything unrecognised fall back to the stone impact.
        return "stone";
    }

    private void playSound(Level level, Vec3 position, ResourceLocation soundId, float volume, float pitch,
            java.util.Optional<Float> range) {
        SoundEvent soundEvent;
        if (range.isPresent()) {
            soundEvent = SoundEvent.createFixedRangeEvent(soundId, range.get());
        } else {
            soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId);
            if (soundEvent == null) {
                logger.warn("Sound not registered: {}, check sounds.json", soundId);
                soundEvent = SoundEvent.createVariableRangeEvent(soundId);
            }
        }
        level.playSound(null, position.x, position.y, position.z, soundEvent, SoundSource.BLOCKS, volume, pitch);
    }

    public enum BlockSoundType {
        HIT(BulletSounds.BlockSound::hit),
        RICOCHET_IMPACT(BulletSounds.BlockSound::ricochetImpact),
        RICOCHET(BulletSounds.BlockSound::ricochet),
        PIERCE(BulletSounds.BlockSound::pierce),
        BREAK(BulletSounds.BlockSound::breakSound);

        private final Function<BulletSounds.BlockSound, List<BulletSounds.BlockSoundEntry>> getter;

        BlockSoundType(Function<BulletSounds.BlockSound, List<BulletSounds.BlockSoundEntry>> getter) {
            this.getter = getter;
        }

        public List<BulletSounds.BlockSoundEntry> getSounds(BulletSounds.BlockSound sound) {
            return getter.apply(sound);
        }
    }

    public enum EntitySoundType {
        HIT(BulletSounds.EntitySound::hit),
        PIERCE(BulletSounds.EntitySound::pierce),
        KILL(BulletSounds.EntitySound::kill);

        private final Function<BulletSounds.EntitySound, List<BulletSounds.EntitySoundEntry>> getter;

        EntitySoundType(Function<BulletSounds.EntitySound, List<BulletSounds.EntitySoundEntry>> getter) {
            this.getter = getter;
        }

        public List<BulletSounds.EntitySoundEntry> getSounds(BulletSounds.EntitySound sound) {
            return getter.apply(sound);
        }
    }
}
