package ru.liko.tacz_mechanics.freeaim;

import org.junit.jupiter.api.Test;
import ru.liko.tacz_mechanics.client.freeaim.SwaySpring;
import static org.junit.jupiter.api.Assertions.*;

class SwaySpringTest {

    @Test
    void startsAtZero() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.2f, 0.5f, 5f);
        assertEquals(0f, s.getValue(), 1e-6);
    }

    @Test
    void impulseDecaysBackToZero() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.2f, 0.5f, 10f);
        s.addImpulse(3f);
        for (int i = 0; i < 500; i++) {
            s.update(1f);
        }
        assertEquals(0f, s.getValue(), 0.01f);
    }

    @Test
    void doesNotDivergeWithSaneParams() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.3f, 0.6f, 10f);
        s.addImpulse(5f);
        for (int i = 0; i < 1000; i++) {
            s.update(1f);
            assertTrue(Math.abs(s.getValue()) <= 10f + 1e-3,
                    "position must stay within clamp, was " + s.getValue());
        }
    }

    @Test
    void clampsToMaxAngle() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.0f, 0.0f, 2f); // нет возврата, нет демпфирования
        s.addImpulse(100f);
        s.update(1f);
        assertEquals(2f, s.getValue(), 1e-6);
        s.addImpulse(-100f);
        s.update(1f);
        assertEquals(-2f, s.getValue(), 1e-6);
    }

    @Test
    void interpolationBlendsPrevAndCurrent() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.0f, 0.0f, 100f);
        s.addImpulse(10f);
        s.update(1f); // prev=0, current=10
        assertEquals(0f, s.getInterpolated(0f), 1e-6);
        assertEquals(10f, s.getInterpolated(1f), 1e-6);
        assertEquals(5f, s.getInterpolated(0.5f), 1e-6);
    }

    @Test
    void resetZeroesEverything() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.2f, 0.5f, 10f);
        s.addImpulse(5f);
        s.update(1f);
        s.reset();
        assertEquals(0f, s.getValue(), 1e-6);
        assertEquals(0f, s.getInterpolated(1f), 1e-6);
    }
}
