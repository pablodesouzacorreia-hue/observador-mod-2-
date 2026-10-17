package com.pablo.observador.entity;

import com.pablo.observador.Diagnostics;
import com.pablo.observador.HorrorConfig;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Máquina de estados do Observador.
 *
 *  WATCH  - fica parado olhando. Se for encarado por tempo demais, reage (some, muda de lugar ou persegue).
 *  STALK  - acompanha de longe, em ritmo irregular, e congela quando o jogador olha para ele.
 *  CHASE  - perseguição intensa, com arrancadas e pausas. Perde o rastro se o jogador sumir de vista.
 *  SEARCH - vai até o último lugar onde percebeu o jogador e procura.
 *
 * A criatura NÃO sabe a posição do jogador o tempo todo: só atualiza "lastKnown"
 * por visão direta ou por som de corrida próxima.
 */
public class ObservadorBehaviorGoal extends Goal {
    private enum State { WATCH, STALK, CHASE, SEARCH }

    private static final int MAX_LIFE_TICKS = 4800; // 4 minutos
    private static final double SIGHT_RANGE = 64.0;
    private static final double HEARING_RANGE = 18.0;

    private final ObservadorEntity mob;
    private Player target;

    private State state = State.WATCH;
    private int stateTicks;
    private int stateDuration;
    private int stareTicks;
    private int stareLimit;
    private int lostTrackTicks;
    private int attackCooldown;
    private int lightTicks;
    private int hits;
    private int burstTicks;
    private int repathTicks;
    private int fleeCountdown = -1;
    private double speedMod = 1.0;
    private Vec3 lastKnown;

    public ObservadorBehaviorGoal(ObservadorEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.target = findTarget();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        this.target = findTarget();
        return this.target != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.lastKnown = this.target != null ? this.target.position() : null;
        enterState(State.WATCH);
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
    }

    private Player findTarget() {
        Player player = this.mob.level().getNearestPlayer(this.mob, 96.0);
        if (player == null || player.isSpectator() || !player.isAlive()) {
            return null;
        }
        return player;
    }

    // ------------------------------------------------------------------ utilidades

    private double aggression() {
        double intensity = HorrorConfig.HORROR_INTENSITY.get();
        return HorrorConfig.AGGRESSION.get() * (0.5 + 0.5 * intensity);
    }

    private double chaseChance() {
        if (this.mob.level().getDifficulty() == Difficulty.PEACEFUL) {
            return 0.0;
        }
        double base = 0.04 + this.mob.tension * 0.004;
        return Math.max(0.0, Math.min(0.6, base * aggression()));
    }

    private void vanish(String reason) {
        Diagnostics.log("Observador some (estado={}, motivo={})", this.state, reason);
        this.mob.discard();
    }

    private boolean playerLooksAtMob() {
        double dist = this.mob.distanceTo(this.target);
        if (dist > SIGHT_RANGE) {
            return false;
        }
        return Placement.viewDot(this.target, this.mob.getEyePosition()) > 0.94
                && this.target.hasLineOfSight(this.mob);
    }

    /** Muda a criatura de lugar, fora do campo de visão do jogador. */
    private boolean relocate(double minDist, double maxDist) {
        RandomSource rnd = this.mob.getRandom();
        for (int i = 0; i < 12; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            double dist = minDist + rnd.nextDouble() * (maxDist - minDist);
            double x = this.target.getX() + Math.cos(angle) * dist;
            double z = this.target.getZ() + Math.sin(angle) * dist;
            BlockPos pos = Placement.findStandPos(this.mob.level(), x, this.target.getY(), z);
            if (pos == null) {
                continue;
            }
            Vec3 where = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (Placement.viewDot(this.target, where) > 0.5) {
                continue;
            }
            this.mob.setPos(where.x, where.y, where.z);
            this.mob.getNavigation().stop();
            Diagnostics.log("Observador mudou de posição para {}", pos);
            return true;
        }
        return false;
    }

    private void enterState(State next) {
        RandomSource rnd = this.mob.getRandom();
        this.state = next;
        this.stateTicks = 0;
        this.repathTicks = 0;
        this.burstTicks = 0;
        switch (next) {
            case WATCH -> {
                this.stateDuration = 100 + rnd.nextInt(140);
                this.stareTicks = 0;
                this.stareLimit = 30 + rnd.nextInt(40);
                this.mob.getNavigation().stop();
            }
            case STALK -> {
                this.stateDuration = 500 + rnd.nextInt(700);
                this.stareTicks = 0;
            }
            case CHASE -> {
                this.stateDuration = (int) (500 + 200 * aggression());
                this.hits = 0;
                this.lostTrackTicks = 0;
                this.attackCooldown = 10;
            }
            case SEARCH -> this.stateDuration = 240;
        }
        Diagnostics.log("Observador entrou em {}", next);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        RandomSource rnd = this.mob.getRandom();
        this.stateTicks++;
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.mob.tickCount > MAX_LIFE_TICKS) {
            vanish("tempo de vida esgotado");
            return;
        }

        // Contramedida 1: luz forte por perto faz a criatura desaparecer.
        int light = this.mob.level().getMaxLocalRawBrightness(this.mob.blockPosition());
        if (light >= 10) {
            this.lightTicks++;
        } else if (this.lightTicks > 0) {
            this.lightTicks = Math.max(0, this.lightTicks - 2);
        }
        if (this.lightTicks > 60) {
            vanish("luz forte");
            return;
        }

        // Contramedida 2: ser atingida pelo jogador a faz recuar.
        if (this.mob.consumeHurtFlag() && this.fleeCountdown < 0) {
            if (this.state != State.CHASE || rnd.nextFloat() < 0.6f) {
                this.fleeCountdown = 10 + rnd.nextInt(30);
            }
        }
        if (this.fleeCountdown >= 0) {
            this.fleeCountdown--;
            if (this.fleeCountdown <= 0) {
                vanish("ferida ou recuando");
            } else {
                Vec3 away = this.mob.position().subtract(this.target.position()).normalize().scale(10.0);
                Vec3 dest = this.mob.position().add(away);
                this.mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.6);
            }
            return;
        }

        // Percepção: visão direta ou som de corrida. Nada de saber a posição "magicamente".
        double distSq = this.mob.distanceToSqr(this.target);
        boolean los = distSq < SIGHT_RANGE * SIGHT_RANGE && this.mob.hasLineOfSight(this.target);
        boolean heard = this.target.isSprinting() && distSq < HEARING_RANGE * HEARING_RANGE;
        boolean senses = los || heard;
        if (senses) {
            this.lastKnown = this.target.position();
        }

        switch (this.state) {
            case WATCH -> tickWatch(rnd);
            case STALK -> tickStalk(rnd);
            case CHASE -> tickChase(rnd, senses, los, distSq);
            case SEARCH -> tickSearch(senses, distSq);
        }
    }

    private void tickWatch(RandomSource rnd) {
        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(this.target, 40.0F, 40.0F);

        if (playerLooksAtMob()) {
            this.stareTicks++;
        } else if (this.stareTicks > 0) {
            this.stareTicks--;
        }

        if (this.stareTicks >= this.stareLimit) {
            // O jogador encarou a criatura: ela escolhe como reagir.
            float roll = rnd.nextFloat();
            if (roll < chaseChance()) {
                enterState(State.CHASE);
            } else if (roll < 0.55f) {
                vanish("encarada pelo jogador");
            } else if (relocate(18.0, 32.0)) {
                enterState(State.STALK);
            } else {
                vanish("sem lugar para mudar de posição");
            }
            return;
        }

        if (this.stateTicks >= this.stateDuration) {
            float roll = rnd.nextFloat();
            if (roll < chaseChance() * 0.6) {
                enterState(State.CHASE);
            } else if (roll < 0.40f) {
                vanish("cansou de observar");
            } else {
                enterState(State.STALK);
            }
        }
    }

    private void tickStalk(RandomSource rnd) {
        Vec3 known = this.lastKnown != null ? this.lastKnown : this.target.position();
        Vec3 toMob = this.mob.position().subtract(known);
        double dist = toMob.horizontalDistance();

        // Congela quando o jogador olha: o medo vem de não saber se ela se mexeu.
        if (playerLooksAtMob() && dist < 56.0) {
            this.mob.getNavigation().stop();
            this.mob.getLookControl().setLookAt(this.target, 40.0F, 40.0F);
            this.stareTicks++;
            if (this.stareTicks > 50 + rnd.nextInt(40)) {
                this.stareTicks = 0;
                if (rnd.nextFloat() < 0.5f) {
                    vanish("encarada enquanto seguia");
                } else if (!relocate(20.0, 34.0)) {
                    vanish("sem lugar para mudar de posição");
                }
            }
            return;
        } else if (this.stareTicks > 0) {
            this.stareTicks--;
        }

        // Ritmo irregular: quase parada, de repente uma arrancada curta.
        if (--this.burstTicks <= 0) {
            this.burstTicks = 15 + rnd.nextInt(50);
            this.speedMod = rnd.nextFloat() < 0.25f ? 1.5 : 0.55 + rnd.nextDouble() * 0.25;
        }

        if (--this.repathTicks <= 0) {
            this.repathTicks = 10;
            Vec3 dest = null;
            if (dist > 26.0) {
                dest = known; // aproxima
            } else if (dist < 15.0) {
                dest = this.mob.position().add(toMob.normalize().scale(8.0)); // recua
            }
            if (dest != null) {
                this.mob.getNavigation().moveTo(dest.x, dest.y, dest.z, this.speedMod);
            } else {
                this.mob.getNavigation().stop();
            }
        }

        if (this.mob.tickCount % 40 < 20) {
            this.mob.getLookControl().setLookAt(this.target, 20.0F, 20.0F);
        }

        if (this.stateTicks >= this.stateDuration) {
            // Tensão prolongada: às vezes termina em perseguição, na maioria das vezes só some.
            if (rnd.nextFloat() < chaseChance() * 1.5) {
                enterState(State.CHASE);
            } else {
                vanish("terminou de seguir");
            }
        }
    }

    private void tickChase(RandomSource rnd, boolean senses, boolean los, double distSq) {
        if (senses) {
            this.lostTrackTicks = 0;
        } else {
            this.lostTrackTicks++;
        }

        // Arrancadas e pausas.
        if (--this.burstTicks <= 0) {
            this.burstTicks = 20 + rnd.nextInt(40);
            double base = rnd.nextFloat() < 0.5f ? 1.0 : 1.55;
            this.speedMod = base * Math.max(0.7, Math.min(1.3, 0.85 + 0.15 * aggression()));
        }

        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        if (--this.repathTicks <= 0) {
            this.repathTicks = 4;
            Vec3 dest = senses ? this.target.position() : this.lastKnown;
            if (dest != null) {
                this.mob.getNavigation().moveTo(dest.x, dest.y, dest.z, this.speedMod);
            }
        }

        if (distSq < 2.3 * 2.3 && los && this.attackCooldown <= 0) {
            attack();
        }

        if (this.hits >= 2) {
            // Fair play: depois de dois golpes ela recua.
            this.fleeCountdown = 30;
        } else if (this.lostTrackTicks > 140) {
            enterState(State.SEARCH);
        } else if (this.stateTicks > this.stateDuration) {
            if (rnd.nextFloat() < 0.5f) {
                vanish("perseguição terminou");
            } else {
                enterState(State.SEARCH);
            }
        }
    }

    private void tickSearch(boolean senses, double distSq) {
        if (senses && distSq < 20.0 * 20.0) {
            enterState(State.CHASE);
            return;
        }
        if (--this.repathTicks <= 0) {
            this.repathTicks = 10;
            if (this.lastKnown != null) {
                this.mob.getNavigation().moveTo(this.lastKnown.x, this.lastKnown.y, this.lastKnown.z, 0.9);
            }
        }
        if (this.stateTicks > this.stateDuration) {
            vanish("perdeu o jogador");
        }
    }

    private void attack() {
        this.attackCooldown = 25;
        this.mob.swing(InteractionHand.MAIN_HAND);
        if (this.mob.level() instanceof ServerLevel serverLevel
                && serverLevel.getDifficulty() != Difficulty.PEACEFUL) {
            float damage = (float) (this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * aggression());
            if (this.target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(this.mob), damage)) {
                this.hits++;
                Diagnostics.log("Observador acertou o jogador (dano={}, golpes={})", damage, this.hits);
            }
        }
    }
}
