package ru.liko.tacz_mechanics.client.freeaim;

/**
 * Spring-damper physics for one rotation axis (degrees).
 * Semi-implicit Euler integration for stability. No Minecraft dependencies (unit-testable).
 */
public final class SwaySpring {

    private float position;
    private float velocity;
    private float prevPosition;

    private float stiffness = 0.2f;
    private float damping = 0.5f;
    private float maxAngle = 5f;

    public void setParams(float stiffness, float damping, float maxAngle) {
        this.stiffness = stiffness;
        this.damping = damping;
        this.maxAngle = maxAngle;
    }

    public void addImpulse(float impulse) {
        velocity += impulse;
    }

    public void update(float dt) {
        prevPosition = position;
        float accel = -stiffness * position - damping * velocity;
        velocity += accel * dt;
        position += velocity * dt;
        if (position > maxAngle) {
            position = maxAngle;
            velocity = 0;
        } else if (position < -maxAngle) {
            position = -maxAngle;
            velocity = 0;
        }
    }

    public float getValue() {
        return position;
    }

    public float getInterpolated(float pt) {
        return prevPosition + (position - prevPosition) * pt;
    }

    public void reset() {
        position = 0f;
        velocity = 0f;
        prevPosition = 0f;
    }
}
