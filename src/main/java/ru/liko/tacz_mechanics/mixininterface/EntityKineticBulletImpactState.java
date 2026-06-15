package ru.liko.tacz_mechanics.mixininterface;

/**
 * Tracks per-bullet block impact counters used by {@code BulletBlockImpactMixin}.
 * Replaces the old skip-flag protocol (which was unreliable because mixin's
 * {@code @Inject(cancellable=true)} short-circuits subsequent HEAD injections).
 */
public interface EntityKineticBulletImpactState {
    int taczMechanics$getRicochetCount();

    void taczMechanics$incrementRicochetCount();

    int taczMechanics$getPierceCount();

    void taczMechanics$incrementPierceCount();
}
