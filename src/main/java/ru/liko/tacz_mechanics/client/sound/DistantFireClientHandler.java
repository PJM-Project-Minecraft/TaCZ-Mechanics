package ru.liko.tacz_mechanics.client.sound;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.client.ClientDistantFireSettings;
import ru.liko.tacz_mechanics.network.DistantFireSoundPacket;

/**
 * Plays distant-fire sounds on the receiving client. The server has already
 * resolved which layer(s) to play and at what volume, so this side is just a
 * thin player.
 */
@OnlyIn(Dist.CLIENT)
public final class DistantFireClientHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DistantFireClientHandler() {
    }

    public static void handleDistantFireSound(DistantFireSoundPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Server already gated on Config.DistantFire.enabled before sending: if a
        // packet arrived, the player must hear it. Don't second-guess via local flags
        // (sync race, dedicated server without local config, etc.).
        Vec3 shotPos = new Vec3(packet.x(), packet.y(), packet.z());
        double distanceBlocks = mc.player.position().distanceTo(shotPos);
        int propagationDelayTicks = ClientDistantFireSettings.propagationDelayTicks(distanceBlocks);

        if (Config.debug) {
            LOGGER.info("[DistantFire Client] play pos=({}, {}, {}) dist={} delayTicks={} primary={} vol={} secondary={} secondaryVol={} range={}",
                packet.x(), packet.y(), packet.z(),
                distanceBlocks, propagationDelayTicks,
                packet.primarySound(), packet.primaryVolume(),
                packet.secondarySound().orElse(null), packet.secondaryVolume(),
                packet.soundRange());
        }

        if (packet.primaryVolume() > 0.01f) {
            playSound(mc, packet.primarySound(),
                packet.x(), packet.y(), packet.z(),
                packet.primaryVolume(), packet.pitch(), packet.soundRange(), propagationDelayTicks);
        }
        packet.secondarySound().ifPresent(loc -> {
            if (packet.secondaryVolume() > 0.01f) {
                playSound(mc, loc,
                    packet.x(), packet.y(), packet.z(),
                    packet.secondaryVolume(), packet.pitch(), packet.soundRange(), propagationDelayTicks);
            }
        });
    }

    /** Boost the server-resolved volume so distant fire stays audible against louder near-shot sounds. */
    private static final float CLIENT_VOLUME_BOOST = 1.5f;

    private static void playSound(Minecraft mc, ResourceLocation soundLoc, double x, double y, double z,
                                   float volume, float pitch, float maxRange, int delayTicks) {
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundLoc);
        if (soundEvent == null) {
            // Sounds defined only in sounds.json are not in BuiltInRegistries; we still
            // need a fixed range so vanilla doesn't auto-pick a tiny variable range
            // (FilteredSoundInstance overrides attenuation to NONE anyway).
            soundEvent = SoundEvent.createFixedRangeEvent(soundLoc, maxRange);
        }

        float effectiveVolume = Math.min(1.0f, volume * CLIENT_VOLUME_BOOST);

        FilteredSoundInstance soundInstance = new FilteredSoundInstance(
            soundEvent,
            SoundSource.PLAYERS,
            effectiveVolume,
            pitch,
            x, y, z,
            0.0f,
            delayTicks
        );

        mc.getSoundManager().play(soundInstance);

        if (Config.debug) {
            LOGGER.info("[DistantFire Client] dispatched sound={} vol={} (boosted from {}) range={} delayTicks={}",
                soundLoc, effectiveVolume, volume, maxRange, delayTicks);
        }
    }
}
