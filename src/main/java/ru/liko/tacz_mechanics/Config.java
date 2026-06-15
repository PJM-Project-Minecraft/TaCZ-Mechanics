package ru.liko.tacz_mechanics;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = TaczMechanics.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue DEBUG = SERVER_BUILDER
            .comment("Enable debug logging for all mechanics (spams logs, use only for testing)")
            .define("debug", false);

    public static boolean debug;

    public static final class Tweaks {
        private static final ModConfigSpec.BooleanValue ALWAYS_FILTER_BY_HAND = SERVER_BUILDER
                .comment("Always enable filter by hand option in the gun smith table")
                .define("tweaks.alwaysFilterByHand", true);
        private static final ModConfigSpec.BooleanValue SUPPRESS_HEAD_HIT_SOUNDS = SERVER_BUILDER
                .comment("Suppresses the sound that plays when you land a headshot")
                .define("tweaks.suppressHeadHitSounds", false);
        private static final ModConfigSpec.BooleanValue SUPPRESS_FLESH_HIT_SOUNDS = SERVER_BUILDER
                .comment("Suppresses the sound that plays when you land a shot that is not a headshot")
                .define("tweaks.suppressFleshHitSounds", false);
        private static final ModConfigSpec.BooleanValue SUPPRESS_KILL_SOUNDS = SERVER_BUILDER
                .comment("Suppresses the sound that plays when you kill an entity with a gun")
                .define("tweaks.suppressKillSounds", false);
        private static final ModConfigSpec.BooleanValue HIDE_HIT_MARKERS = SERVER_BUILDER
                .comment("Hides hit markers when hitting entities")
                .define("tweaks.hideHitMarkers", false);
        private static final ModConfigSpec.BooleanValue HIDE_GUN_CROSSHAIR = SERVER_BUILDER
                .comment("Completely hides the gun crosshair (TACZ crosshair)")
                .define("tweaks.hideGunCrosshair", false);

        public static boolean alwaysFilterByHand;
        public static boolean suppressHeadHitSounds;
        public static boolean suppressFleshHitSounds;
        public static boolean suppressKillSounds;
        public static boolean hideHitMarkers;
        public static boolean hideGunCrosshair;

        private static void load() {
            alwaysFilterByHand = ALWAYS_FILTER_BY_HAND.get();
            suppressHeadHitSounds = SUPPRESS_HEAD_HIT_SOUNDS.get();
            suppressFleshHitSounds = SUPPRESS_FLESH_HIT_SOUNDS.get();
            suppressKillSounds = SUPPRESS_KILL_SOUNDS.get();
            hideHitMarkers = HIDE_HIT_MARKERS.get();
            hideGunCrosshair = HIDE_GUN_CROSSHAIR.get();
        }

        static void init() {
        }

        private Tweaks() {
        }
    }

    public static final class DistantFire {
        private static final ModConfigSpec.BooleanValue ENABLED = SERVER_BUILDER
                .comment("Enable distant fire sounds (hear gunshots from far away)")
                .define("distantFire.enabled", true);
        private static final ModConfigSpec.IntValue MIN_DISTANCE = SERVER_BUILDER
                .comment("Minimum distance (blocks) for distant fire effect to start")
                .defineInRange("distantFire.minDistance", 64, 16, 500);
        private static final ModConfigSpec.IntValue MAX_DISTANCE = SERVER_BUILDER
                .comment("Maximum distance (blocks) to hear distant fire")
                .defineInRange("distantFire.maxDistance", 300, 100, 1000);
        private static final ModConfigSpec.DoubleValue VOLUME_MULTIPLIER = SERVER_BUILDER
                .comment("Volume multiplier for distant fire sounds")
                .defineInRange("distantFire.volumeMultiplier", 0.7, 0.1, 1.0);
        private static final ModConfigSpec.BooleanValue INCLUDE_SHOOTER = SERVER_BUILDER
                .comment(
                    "If true, the shooting player also receives distant fire packets (solo testing; may overlap with normal TaCZ shot sounds)")
                .define("distantFire.includeShooter", false);
        private static final ModConfigSpec.IntValue CLOSE_MAX_DISTANCE = SERVER_BUILDER
                .comment(
                    "Upper bound (blocks) for the \"close\" distant layer (above vanilla TaCZ range). Sent to clients; should stay <= maxDistance for audible far tiers.")
                .defineInRange("distantFire.closeMaxDistance", 100, 16, 1000);
        private static final ModConfigSpec.IntValue MID_MAX_DISTANCE = SERVER_BUILDER
                .comment("Upper bound (blocks) for the \"mid\" distant layer")
                .defineInRange("distantFire.midMaxDistance", 200, 32, 1500);
        private static final ModConfigSpec.IntValue FAR_MAX_DISTANCE = SERVER_BUILDER
                .comment(
                    "Upper bound (blocks) for the \"far\" layer; beyond this uses very_far sound if defined. Prefer maxDistance >= this value.")
                .defineInRange("distantFire.farMaxDistance", 400, 64, 2000);
        private static final ModConfigSpec.IntValue TRANSITION_BLOCKS = SERVER_BUILDER
                .comment("Half-width in blocks for crossfading between distant layers")
                .defineInRange("distantFire.transitionBlocks", 20, 1, 80);
        private static final ModConfigSpec.IntValue NEAR_SOUND_RANGE = SERVER_BUILDER
                .comment(
                    "Within this radius (blocks) from the shot, TaCZ handles the main gunshot tail; distant layer packets are not sent to closer listeners. Also the distance at which low-pass muffle on TaCZ 3P sounds starts.")
                .defineInRange("distantFire.nearSoundRange", 32, 8, 128);
        private static final ModConfigSpec.IntValue ANTI_SPAM_TICKS = SERVER_BUILDER
                .comment(
                    "Minimum server ticks between distant-fire packets for the same listener+shooter pair (reduces stacking on full auto). 0 = off.")
                .defineInRange("distantFire.antiSpamTicks", 2, 0, 40);
        private static final ModConfigSpec.BooleanValue SOUND_PROPAGATION = SERVER_BUILDER
                .comment(
                    "Delay distant-fire playback on the client by distance / sound speed (1 block treated as 1 m). "
                        + "Synced to clients on dedicated servers.")
                .define("distantFire.soundPropagation", true);
        private static final ModConfigSpec.DoubleValue SOUND_SPEED_BLOCKS_PER_SEC = SERVER_BUILDER
                .comment("Effective speed of sound in blocks per second (air ~343). Used only if soundPropagation is true.")
                .defineInRange("distantFire.soundSpeedBlocksPerSecond", 343.0, 1.0, 2000.0);
        private static final ModConfigSpec.IntValue SOUND_PROPAGATION_MAX_DELAY_TICKS = SERVER_BUILDER
                .comment(
                    "Max delay in game ticks before distant fire plays (20 ticks = 1 s). 0 = no cap. "
                        + "Prevents extreme ranges from lagging audio far behind.")
                .defineInRange("distantFire.soundPropagationMaxDelayTicks", 120, 0, 1200);

        public static boolean enabled;
        public static int minDistance;
        public static int maxDistance;
        public static double volumeMultiplier;
        public static boolean includeShooter;
        public static int closeMaxDistance;
        public static int midMaxDistance;
        public static int farMaxDistance;
        public static int transitionBlocks;
        public static int nearSoundRange;
        public static int antiSpamTicks;
        public static boolean soundPropagation;
        public static double soundSpeedBlocksPerSecond;
        public static int soundPropagationMaxDelayTicks;

        private static void load() {
            enabled = ENABLED.get();
            minDistance = MIN_DISTANCE.get();
            maxDistance = MAX_DISTANCE.get();
            volumeMultiplier = VOLUME_MULTIPLIER.get();
            includeShooter = INCLUDE_SHOOTER.get();
            closeMaxDistance = CLOSE_MAX_DISTANCE.get();
            midMaxDistance = MID_MAX_DISTANCE.get();
            farMaxDistance = FAR_MAX_DISTANCE.get();
            transitionBlocks = TRANSITION_BLOCKS.get();
            nearSoundRange = NEAR_SOUND_RANGE.get();
            antiSpamTicks = ANTI_SPAM_TICKS.get();
            soundPropagation = SOUND_PROPAGATION.get();
            soundSpeedBlocksPerSecond = SOUND_SPEED_BLOCKS_PER_SEC.get();
            soundPropagationMaxDelayTicks = SOUND_PROPAGATION_MAX_DELAY_TICKS.get();
        }

        static void init() {
        }

        private DistantFire() {
        }
    }

    public static final class Whizz {
        private static final ModConfigSpec.BooleanValue ENABLED = SERVER_BUILDER
                .comment("Enable bullet whizz sounds (hear bullets passing by)")
                .define("whizz.enabled", true);
        private static final ModConfigSpec.DoubleValue MAX_DISTANCE = SERVER_BUILDER
                .comment("Maximum distance (blocks) at which a bullet flying past the player can produce a whizz sound. If the bullet passes farther than this, no sound is played.")
                .defineInRange("whizz.maxDistance", 8.0, 0.5, 64.0);

        public static boolean enabled;
        public static double maxDistance;

        private static void load() {
            enabled = ENABLED.get();
            maxDistance = MAX_DISTANCE.get();
        }

        static void init() {
        }

        private Whizz() {
        }
    }

    public static final class Suppression {
        private static final ModConfigSpec.BooleanValue ENABLED = SERVER_BUILDER
                .comment("Enable suppression visual effect when bullets fly near or impact nearby")
                .define("suppression.enabled", true);
        private static final ModConfigSpec.DoubleValue DETECTION_RADIUS = SERVER_BUILDER
                .comment("Maximum distance (blocks) for suppression detection")
                .defineInRange("suppression.detectionRadius", 10.0, 1.0, 50.0);
        private static final ModConfigSpec.DoubleValue FLYBY_INTENSITY = SERVER_BUILDER
                .comment("Base intensity added per bullet fly-by at closest range (0.0-1.0)")
                .defineInRange("suppression.flybyIntensity", 0.25, 0.01, 1.0);
        private static final ModConfigSpec.DoubleValue IMPACT_INTENSITY_MULTIPLIER = SERVER_BUILDER
                .comment("Intensity multiplier for bullet impacts near player (relative to fly-by)")
                .defineInRange("suppression.impactIntensityMultiplier", 0.7, 0.1, 2.0);
        private static final ModConfigSpec.DoubleValue SHAKE_INTENSITY = SERVER_BUILDER
                .comment("Camera shake intensity multiplier (0.0-3.0)")
                .defineInRange("suppression.shakeIntensity", 1.0, 0.0, 3.0);
        private static final ModConfigSpec.DoubleValue SHAKE_SPEED = SERVER_BUILDER
                .comment("Camera shake speed (frequency of shakes)")
                .defineInRange("suppression.shakeSpeed", 1.0, 0.0, 3.0);
        private static final ModConfigSpec.DoubleValue DECAY_RATE = SERVER_BUILDER
                .comment("Suppression decay per tick (how fast the effect fades)")
                .defineInRange("suppression.decayRate", 0.015, 0.001, 0.2);
        private static final ModConfigSpec.DoubleValue MAX_INTENSITY = SERVER_BUILDER
                .comment("Maximum suppression intensity (0.0-1.0)")
                .defineInRange("suppression.maxIntensity", 1.0, 0.1, 1.0);
        private static final ModConfigSpec.DoubleValue BLUR_STRENGTH = SERVER_BUILDER
                .comment("Blur effect strength multiplier")
                .defineInRange("suppression.blurStrength", 1.0, 0.0, 3.0);
        private static final ModConfigSpec.DoubleValue VIGNETTE_STRENGTH = SERVER_BUILDER
                .comment("Vignette darkening strength multiplier")
                .defineInRange("suppression.vignetteStrength", 1.0, 0.0, 3.0);
        private static final ModConfigSpec.DoubleValue DESATURATION_STRENGTH = SERVER_BUILDER
                .comment("Color desaturation strength multiplier")
                .defineInRange("suppression.desaturationStrength", 1.0, 0.0, 3.0);

        public static boolean enabled;
        public static double detectionRadius;
        public static double flybyIntensity;
        public static double impactIntensityMultiplier;
        public static double shakeIntensity;
        public static double shakeSpeed;
        public static double decayRate;
        public static double maxIntensity;
        public static double blurStrength;
        public static double vignetteStrength;
        public static double desaturationStrength;

        private static void load() {
            enabled = ENABLED.get();
            detectionRadius = DETECTION_RADIUS.get();
            flybyIntensity = FLYBY_INTENSITY.get();
            impactIntensityMultiplier = IMPACT_INTENSITY_MULTIPLIER.get();
            decayRate = DECAY_RATE.get();
            shakeIntensity = SHAKE_INTENSITY.get();
            shakeSpeed = SHAKE_SPEED.get();
            maxIntensity = MAX_INTENSITY.get();
            blurStrength = BLUR_STRENGTH.get();
            vignetteStrength = VIGNETTE_STRENGTH.get();
            desaturationStrength = DESATURATION_STRENGTH.get();
        }

        static void init() {
        }

        private Suppression() {
        }
    }

    public static final class Ricochet {
        private static final ModConfigSpec.BooleanValue ENABLED = SERVER_BUILDER
                .comment("Enable bullet ricochet off blocks")
                .define("ricochet.enabled", true);
        private static final ModConfigSpec.BooleanValue DEMO_PRESET = SERVER_BUILDER
                .comment("Enable demo ricochet preset (forces 100% ricochet-friendly values)")
                .define("ricochet.demoPreset", false);
        private static final ModConfigSpec.BooleanValue DEBUG = SERVER_BUILDER
                .comment("Enable debug logging for ricochet decisions")
                .define("ricochet.debug", false);
        private static final ModConfigSpec.DoubleValue MIN_SPEED = SERVER_BUILDER
                .comment("Minimum bullet speed required to ricochet")
                .defineInRange("ricochet.minSpeed", 1.0, 0.05, 100.0);
        private static final ModConfigSpec.DoubleValue MIN_ANGLE = SERVER_BUILDER
                .comment("Minimum incidence angle (degrees) from surface normal to allow ricochet")
                .defineInRange("ricochet.minAngle", 60.0, 10.0, 89.0);
        private static final ModConfigSpec.IntValue MAX_BOUNCES = SERVER_BUILDER
                .comment("Maximum number of ricochets per bullet")
                .defineInRange("ricochet.maxBounces", 1, 0, 10);
        private static final ModConfigSpec.DoubleValue SPEED_MULTIPLIER = SERVER_BUILDER
                .comment("Speed multiplier applied after ricochet (real-world: ~35% loss = 0.65)")
                .defineInRange("ricochet.speedMultiplier", 0.65, 0.1, 1.0);
        private static final ModConfigSpec.DoubleValue FLATTEN_REFLECTION = SERVER_BUILDER
                .comment("How much the reflection flattens along the surface (0=perfect mirror, 0.2=realistic)")
                .defineInRange("ricochet.flattenReflection", 0.15, 0.0, 0.5);
        private static final ModConfigSpec.DoubleValue CHANCE = SERVER_BUILDER
                .comment("Chance of ricochet when conditions are met (0.0-1.0)")
                .defineInRange("ricochet.chance", 0.5, 0.0, 1.0);

        public static boolean enabled;
        public static boolean demoPreset;
        public static boolean debug;
        public static double minSpeed;
        public static double minAngle;
        public static int maxBounces;
        public static double speedMultiplier;
        public static double flattenReflection;
        public static double chance;

        private static void load() {
            enabled = ENABLED.get();
            demoPreset = DEMO_PRESET.get();
            debug = DEBUG.get();
            if (demoPreset) {
                minSpeed = 0.05;
                minAngle = 10.0;
                maxBounces = 3;
                speedMultiplier = 0.8;
                flattenReflection = 0.0;
                chance = 1.0;
            } else {
                minSpeed = MIN_SPEED.get();
                minAngle = MIN_ANGLE.get();
                maxBounces = MAX_BOUNCES.get();
                speedMultiplier = SPEED_MULTIPLIER.get();
                flattenReflection = FLATTEN_REFLECTION.get();
                chance = CHANCE.get();
            }
        }

        static void init() {
        }

        private Ricochet() {
        }
    }

    public static final class Pierce {
        private static final ModConfigSpec.BooleanValue ENABLED = SERVER_BUILDER
                .comment("Enable bullet block penetration (pierce) system")
                .define("pierce.enabled", true);
        private static final ModConfigSpec.BooleanValue DEBUG = SERVER_BUILDER
                .comment("Enable debug logging for pierce decisions")
                .define("pierce.debug", false);
        private static final ModConfigSpec.IntValue MAX_PIERCES = SERVER_BUILDER
                .comment("Maximum number of blocks a single bullet can pierce. 0 = unlimited.")
                .defineInRange("pierce.maxPierces", 4, 0, 64);
        private static final ModConfigSpec.DoubleValue MIN_SPEED = SERVER_BUILDER
                .comment("Minimum bullet speed required to attempt block pierce")
                .defineInRange("pierce.minSpeed", 0.5, 0.0, 100.0);

        public static boolean enabled;
        public static boolean debug;
        public static int maxPierces;
        public static double minSpeed;

        private static void load() {
            enabled = ENABLED.get();
            debug = DEBUG.get();
            maxPierces = MAX_PIERCES.get();
            minSpeed = MIN_SPEED.get();
        }

        static void init() {
        }

        private Pierce() {
        }
    }

    public static final class FreeAim {
        private static final ModConfigSpec.BooleanValue ENABLED = SERVER_BUILDER
                .comment("Enable free aim - gun direction lags behind camera movement")
                .define("freeAim.enabled", true);
        private static final ModConfigSpec.DoubleValue MAX_ANGLE = SERVER_BUILDER
                .comment("Maximum angle (degrees) the gun can deviate from view direction")
                .defineInRange("freeAim.maxAngle", 2.5, 0.5, 25.0);
        private static final ModConfigSpec.DoubleValue LERP_SPEED = SERVER_BUILDER
                .comment("Speed at which the gun catches up to view direction (0.0-1.0, higher = faster)")
                .defineInRange("freeAim.lerpSpeed", 0.15, 0.01, 1.0);
        private static final ModConfigSpec.BooleanValue DISABLE_WHEN_AIMING = SERVER_BUILDER
                .comment("Disable free aim when aiming down sights")
                .define("freeAim.disableWhenAiming", true);
        private static final ModConfigSpec.DoubleValue CROSSHAIR_SCALE = SERVER_BUILDER
                .comment("Scale factor for converting free aim angle to screen pixels")
                .defineInRange("freeAim.crosshairScale", 10.0, 1.0, 50.0);
        private static final ModConfigSpec.BooleanValue DISABLE_CROSSHAIR_MOVEMENT = SERVER_BUILDER
                .comment("Disable crosshair movement with free aim (crosshair stays centered)")
                .define("freeAim.disableCrosshairMovement", false);

        public static boolean enabled;
        public static double maxAngle;
        public static double lerpSpeed;
        public static boolean disableWhenAiming;
        public static double crosshairScale;
        public static boolean disableCrosshairMovement;

        private static void load() {
            enabled = ENABLED.get();
            maxAngle = MAX_ANGLE.get();
            lerpSpeed = LERP_SPEED.get();
            disableWhenAiming = DISABLE_WHEN_AIMING.get();
            crosshairScale = CROSSHAIR_SCALE.get();
            disableCrosshairMovement = DISABLE_CROSSHAIR_MOVEMENT.get();
        }

        static void init() {
        }

        private FreeAim() {
        }
    }

    public static final class Movement {
        private static final ModConfigSpec.BooleanValue ENABLED = SERVER_BUILDER
                .comment("Enable advanced movement mechanics (sitting, crawling, leaning, sliding)")
                .define("movement.enabled", true);
        private static final ModConfigSpec.BooleanValue SIT_ENABLED = SERVER_BUILDER
                .comment("Enable sitting/crouching pose")
                .define("movement.sitEnabled", true);
        private static final ModConfigSpec.BooleanValue CRAWL_ENABLED = SERVER_BUILDER
                .comment("Enable crawling/prone pose")
                .define("movement.crawlEnabled", true);
        private static final ModConfigSpec.BooleanValue SLIDE_ENABLED = SERVER_BUILDER
                .comment("Enable sliding when sprinting + sit key")
                .define("movement.slideEnabled", true);
        private static final ModConfigSpec.DoubleValue SLIDE_MAX_FORCE = SERVER_BUILDER
                .comment("Maximum sliding force")
                .defineInRange("movement.slideMaxForce", 1.0, 0.1, 3.0);
        private static final ModConfigSpec.BooleanValue LEAN_AUTO_HOLD = SERVER_BUILDER
                .comment("Auto-hold lean position (toggle mode)")
                .define("movement.leanAutoHold", false);
        private static final ModConfigSpec.BooleanValue LEAN_MOUSE_CORRECTION = SERVER_BUILDER
                .comment("Correct mouse input when leaning")
                .define("movement.leanMouseCorrection", true);
        private static final ModConfigSpec.BooleanValue CRAWL_BLOCK_VIEW = SERVER_BUILDER
                .comment("Limit view angle when crawling")
                .define("movement.crawlBlockView", true);
        private static final ModConfigSpec.DoubleValue CRAWL_BLOCK_ANGLE = SERVER_BUILDER
                .comment("Maximum view angle when crawling (in degrees)")
                .defineInRange("movement.crawlBlockAngle", 90.0, 30.0, 180.0);
        private static final ModConfigSpec.DoubleValue SIT_COOLDOWN = SERVER_BUILDER
                .comment("Cooldown between sit actions (seconds)")
                .defineInRange("movement.sitCooldown", 0.75, 0.0, 5.0);
        private static final ModConfigSpec.DoubleValue CRAWL_COOLDOWN = SERVER_BUILDER
                .comment("Cooldown between crawl actions (seconds)")
                .defineInRange("movement.crawlCooldown", 0.75, 0.0, 5.0);
        private static final ModConfigSpec.DoubleValue LEAN_COOLDOWN = SERVER_BUILDER
                .comment("Cooldown between lean actions (seconds)")
                .defineInRange("movement.leanCooldown", 0.0, 0.0, 5.0);
        private static final ModConfigSpec.BooleanValue SIT_AUTO_HOLD = SERVER_BUILDER
                .comment("Auto-hold sit position (toggle mode)")
                .define("movement.sitAutoHold", true);
        // NOTE: all pose geometry (sit/crawl hitbox sizes, eye heights, view/render offsets) is now
        // hardcoded in MovementPosture, not exposed here — serverconfig values are "sticky" and would
        // not pick up new defaults, so we tune in code via the movement.debug hitbox logs.
        private static final ModConfigSpec.BooleanValue DEBUG = SERVER_BUILDER
                .comment("Log the player's actual hitbox (bounding box, dimensions, eye position) when",
                    "entering/leaving sit & crawl poses, on both client and server. For diagnosing pose alignment.",
                    "Default ON for the current tuning session — set false once alignment is dialed in.")
                .define("movement.debug", true);

        public static boolean enabled;
        public static boolean sitEnabled;
        public static boolean crawlEnabled;
        public static boolean slideEnabled;
        public static double slideMaxForce;
        public static boolean leanAutoHold;
        public static boolean leanMouseCorrection;
        public static boolean crawlBlockView;
        public static double crawlBlockAngle;
        public static double sitCooldown;
        public static double crawlCooldown;
        public static double leanCooldown;
        public static boolean sitAutoHold;
        public static boolean debug;

        private static void load() {
            enabled = ENABLED.get();
            sitEnabled = SIT_ENABLED.get();
            crawlEnabled = CRAWL_ENABLED.get();
            slideEnabled = SLIDE_ENABLED.get();
            slideMaxForce = SLIDE_MAX_FORCE.get();
            leanAutoHold = LEAN_AUTO_HOLD.get();
            leanMouseCorrection = LEAN_MOUSE_CORRECTION.get();
            crawlBlockView = CRAWL_BLOCK_VIEW.get();
            crawlBlockAngle = CRAWL_BLOCK_ANGLE.get();
            sitCooldown = SIT_COOLDOWN.get();
            crawlCooldown = CRAWL_COOLDOWN.get();
            leanCooldown = LEAN_COOLDOWN.get();
            sitAutoHold = SIT_AUTO_HOLD.get();
            debug = DEBUG.get();
        }

        static void init() {
        }

        private Movement() {
        }
    }

    public static final class ScopeFlare {
        private static final ModConfigSpec.BooleanValue ENABLED = SERVER_BUILDER
                .comment("Enable scope flare effect")
                .define("scopeFlare.enabled", true);
        private static final ModConfigSpec.DoubleValue MIN_ZOOM = SERVER_BUILDER
                .comment("Minimum zoom level for a scope to produce a flare. Scopes with zoom greater than this will have a flare.")
                .defineInRange("scopeFlare.minZoom", 1.5, 1.0, 20.0);
        private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> WHITELISTED_SCOPES = SERVER_BUILDER
                .comment("List of specific scope IDs that will ALWAYS produce a flare, regardless of their zoom level. (e.g. \"tacz:scope_acog_ta31\")")
                .defineList("scopeFlare.whitelistedScopes", java.util.List.of(), obj -> obj instanceof String);
        private static final ModConfigSpec.DoubleValue FORWARD_OFFSET = SERVER_BUILDER
                .comment("Distance (blocks) to render the flare in front of the player's eyes.")
                .defineInRange("scopeFlare.forwardOffset", 0.6, 0.0, 5.0);
        private static final ModConfigSpec.DoubleValue FADE_MIN_DISTANCE = SERVER_BUILDER
                .comment("Distance (blocks) at which the flare starts fading out when getting closer to the player.")
                .defineInRange("scopeFlare.fadeMinDistance", 3.0, 0.0, 50.0);
        private static final ModConfigSpec.DoubleValue FADE_MAX_DISTANCE = SERVER_BUILDER
                .comment("Distance (blocks) at which the flare is fully visible. Between min and max it will smoothly fade.")
                .defineInRange("scopeFlare.fadeMaxDistance", 10.0, 1.0, 100.0);

        public static boolean enabled;
        public static double minZoom;
        public static java.util.List<? extends String> whitelistedScopes;
        public static double forwardOffset;
        public static double fadeMinDistance;
        public static double fadeMaxDistance;

        private static void load() {
            enabled = ENABLED.get();
            minZoom = MIN_ZOOM.get();
            whitelistedScopes = WHITELISTED_SCOPES.get();
            forwardOffset = FORWARD_OFFSET.get();
            fadeMinDistance = FADE_MIN_DISTANCE.get();
            fadeMaxDistance = Math.max(FADE_MAX_DISTANCE.get(), fadeMinDistance + 0.1);
        }

        static void init() {
        }

        private ScopeFlare() {
        }
    }

    static {
        Tweaks.init();
        DistantFire.init();
        Whizz.init();
        Suppression.init();
        Ricochet.init();
        Pierce.init();
        FreeAim.init();
        Movement.init();
        ScopeFlare.init();
    }

    public static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) {
            return;
        }
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            debug = DEBUG.get();
            Tweaks.load();
            DistantFire.load();
            Whizz.load();
            Ricochet.load();
            Pierce.load();
            Suppression.load();
            FreeAim.load();
            Movement.load();
            ScopeFlare.load();
        }
    }
}
