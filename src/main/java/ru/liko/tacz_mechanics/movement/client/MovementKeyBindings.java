package ru.liko.tacz_mechanics.movement.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import ru.liko.tacz_mechanics.TaczMechanics;

/**
 * Key bindings for movement mechanics.
 */
@EventBusSubscriber(modid = TaczMechanics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class MovementKeyBindings {
    
    public static final String CATEGORY = "key.categories.tacz_mechanics.movement";
    
    public static final KeyMapping SIT_KEY = new KeyMapping(
        "key.tacz_mechanics.sit",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_C,
        CATEGORY
    );
    
    public static final KeyMapping CRAWL_KEY = new KeyMapping(
        "key.tacz_mechanics.crawl",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Z,
        CATEGORY
    );
    
    public static final KeyMapping LEAN_LEFT_KEY = new KeyMapping(
        "key.tacz_mechanics.lean_left",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Q,
        CATEGORY
    );
    
    public static final KeyMapping LEAN_RIGHT_KEY = new KeyMapping(
        "key.tacz_mechanics.lean_right",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_E,
        CATEGORY
    );

    // --- Debug pose-tuning keys (active only when movement.debug = true) ---
    /** Cycle which pose parameter is selected for tuning. */
    public static final KeyMapping TUNE_SELECT_KEY = new KeyMapping(
        "key.tacz_mechanics.tune_select",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        CATEGORY
    );
    /** Increase the selected pose parameter. */
    public static final KeyMapping TUNE_INC_KEY = new KeyMapping(
        "key.tacz_mechanics.tune_inc",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_RIGHT_BRACKET,
        CATEGORY
    );
    /** Decrease the selected pose parameter. */
    public static final KeyMapping TUNE_DEC_KEY = new KeyMapping(
        "key.tacz_mechanics.tune_dec",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_BRACKET,
        CATEGORY
    );
    /** Print all current pose values to chat (copy them back for hardcoding). */
    public static final KeyMapping TUNE_DUMP_KEY = new KeyMapping(
        "key.tacz_mechanics.tune_dump",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_P,
        CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(SIT_KEY);
        event.register(CRAWL_KEY);
        event.register(LEAN_LEFT_KEY);
        event.register(LEAN_RIGHT_KEY);
        event.register(TUNE_SELECT_KEY);
        event.register(TUNE_INC_KEY);
        event.register(TUNE_DEC_KEY);
        event.register(TUNE_DUMP_KEY);
    }
}
