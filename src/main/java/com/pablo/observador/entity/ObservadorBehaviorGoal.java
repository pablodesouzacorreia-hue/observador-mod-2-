package com.pablo.observador.entity;

import com.pablo.observador.Diagnostics;
import com.pablo.observador.HorrorConfig;
import com.pablo.observador.sound.ModSounds;
import com.pablo.observador.system.FearManager;
import com.pablo.observador.system.ThreatTalk;
import java.util.EnumSet;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
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
    private int stepTicks;
    private int breathTicks = 60;
    private int chaseSoundTicks;
    private int voiceTicks = 200;
    private int watchTalks;
    private boolean provoked;
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
        return HorrorConfig.AGGRESSION.get() * (0.5 + 0.5 * intensity) * contextBoost();
    }

    /** Mais agressiva de noite, dentro de cavernas e quando o jogador está com medo. */
    private double contextBoost() {
        if (this.target == null) {
            return 1.0;
        }
        double boost = 1.0;
        BlockPos pos = this.target.blockPosition();
        boolean sky = this.mob.level().canSeeSky(pos);
        if (!sky && pos.getY() < 55) {
            boost += 0.5; // caverna
        } else if (sky && this.mob.level().getMaxLocalRawBrightness(pos) <= 7) {
            boost += 0.35; // noite ao ar livre
        }
        return boost + FearManager.get(this.target) / 100.0 * 0.6;
    }

    private double fearScale() {
        return this.target == null ? 1.0 : 1.0 - 0.45 * FearManager.get(this.target) / 100.0;
    }

    private boolean canChase() {
        return this.mob.level().getDifficulty() != Difficulty.PEACEFUL;
    }

    /** Chance de ela partir para a perseguição quando perde a paciência. Cresce a cada encontro. */
    private double chaseChance() {
        if (!canChase()) {
            return 0.0;
        }
        double base = 0.18 + this.mob.tension * 0.012;
        return Math.max(0.0, Math.min(0.9, base * aggression()));
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
                this.stareLimit = 20; // some 1 segundo depois de ser vista
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
                chaseCue(rnd);
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
        if (this.mob.mirage) {
            tickMirage();
            return;
        }
        if (this.mob.forceChase) {
            this.mob.forceChase = false;
            this.provoked = true;
            this.lightTicks = 0;
            this.fleeCountdown = -1;
            enterState(State.CHASE);
        }
        this.stateTicks++;
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        ambientSounds(rnd);

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
        if (this.lightTicks > 60 && !this.mob.testMode) {
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

        // Correr na direção dela é provocação: na maioria das vezes ela deixa de fugir e parte para cima.
        if (!this.provoked && (this.state == State.WATCH || this.state == State.STALK)
                && distSq < 12.0 * 12.0 && this.target.isSprinting() && canChase()) {
            this.provoked = true;
            if (rnd.nextFloat() < 0.6f) {
                enterState(State.CHASE);
                return;
            }
        }

        switch (this.state) {
            case WATCH -> tickWatch(rnd);
            case STALK -> tickStalk(rnd);
            case CHASE -> tickChase(rnd, senses, los, distSq);
            case SEARCH -> tickSearch(senses, distSq);
        }
    }

    private void tickWatch(RandomSource rnd) {
        if (this.watchTalks < 2 && this.stateTicks == 40 + this.watchTalks * 60) {
            this.watchTalks++;
            ThreatTalk.say(this.target, ThreatTalk.Mood.WATCH, rnd);
        }
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
            } else if (roll < 0.85f) {
                ThreatTalk.say(this.target, ThreatTalk.Mood.VANISH, rnd);
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
            if (this.stareTicks > 20) {
                this.stareTicks = 0;
                if (rnd.nextFloat() < chaseChance() * 0.7) {
                    enterState(State.CHASE);
                } else if (rnd.nextFloat() < 0.7f) {
                    ThreatTalk.say(this.target, ThreatTalk.Mood.VANISH, rnd);
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
            double k = fearScale();
            if (dist > 26.0 * k) {
                dest = known; // aproxima
            } else if (dist < 15.0 * k) {
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
            this.speedMod = base * Math.max(0.7, Math.min(1.5, 0.85 + 0.15 * aggression()));
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

    /** Miragem: só fica parada olhando e some um instante depois de ser encarada. */
    private void tickMirage() {
        this.stateTicks++;
        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(this.target, 40.0F, 40.0F);
        if (playerLooksAtMob()) {
            this.stareTicks++;
        }
        if (this.stareTicks > 14 || this.stateTicks > 500) {
            Diagnostics.log("Miragem desfeita (olhada={}, tempo={})", this.stareTicks, this.stateTicks);
            this.mob.discard();
        }
    }

    /** Passos irregulares enquanto anda e respiração distorcida de vez em quando. */
    private void ambientSounds(RandomSource rnd) {
        boolean chasing = this.state == State.CHASE;
        if (this.mob.getNavigation().isInProgress()) {
            if (--this.stepTicks <= 0) {
                this.stepTicks = (chasing ? 5 : 10) + rnd.nextInt(chasing ? 3 : 6);
                ModSounds.play(this.mob.level(), this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        ModSounds.STEP, SoundSource.HOSTILE, chasing ? 1.2F : 0.8F, 0.8F + rnd.nextFloat() * 0.3F);
            }
        }
        // Durante a perseguição: o lamento alto e distorcido se repete vindo da criatura.
        if (chasing && --this.chaseSoundTicks <= 0) {
            this.chaseSoundTicks = 55 + rnd.nextInt(35);
            ModSounds.play(this.mob.level(), this.mob.getX(), this.mob.getEyeY(), this.mob.getZ(),
                    ModSounds.CHASE, SoundSource.HOSTILE, 2.2F, 0.95F + rnd.nextFloat() * 0.1F);
        }
        if (HorrorConfig.VOICE_ENABLED.get() && !chasing && --this.voiceTicks <= 0) {
            double fear = FearManager.get(this.target) / 100.0;
            this.voiceTicks = (int) ((500 + rnd.nextInt(700)) * (1.0 - 0.6 * fear));
            if (this.mob.distanceToSqr(this.target) < 45.0 * 45.0) {
                ModSounds.play(this.mob.level(), this.mob.getX(), this.mob.getEyeY(), this.mob.getZ(),
                        ModSounds.VOICE, SoundSource.HOSTILE, 1.5F, 0.95F + rnd.nextFloat() * 0.1F);
            }
        }
        if (--this.breathTicks <= 0) {
            this.breathTicks = 160 + rnd.nextInt(240);
            if (this.mob.distanceToSqr(this.target) < 26.0 * 26.0) {
                ModSounds.play(this.mob.level(), this.mob.getX(), this.mob.getEyeY(), this.mob.getZ(),
                        ModSounds.BREATH, SoundSource.HOSTILE, 0.7F, 0.9F + rnd.nextFloat() * 0.2F);
            }
        }
    }

    /**
     * Susto no início da perseguição: o lamento alto toca na posição do próprio jogador (sem direção,
     * como se estivesse dentro da cabeça dele). Se os jumpscares estiverem ligados, um grito ou
     * estímulo extra sai da criatura. O volume geral é controlado por audio.intensidadeSons.
     */
    private void chaseCue(RandomSource rnd) {
        this.chaseSoundTicks = 60 + rnd.nextInt(30);
        ThreatTalk.say(this.target, ThreatTalk.Mood.CHASE, rnd, true);
        ModSounds.play(this.mob.level(), this.target.getX(), this.target.getEyeY(), this.target.getZ(),
                ModSounds.CHASE, SoundSource.HOSTILE, 2.5F, 1.0F);

        double jumpscare = HorrorConfig.JUMPSCARE_FREQUENCY.get();
        if (jumpscare > 0.0 && rnd.nextDouble() < Math.min(0.9, 0.5 * jumpscare + 0.2)) {
            boolean scream = rnd.nextBoolean();
            ModSounds.play(this.mob.level(), this.mob.getX(), this.mob.getEyeY(), this.mob.getZ(),
                    scream ? ModSounds.SCREAM : ModSounds.STINGER, SoundSource.HOSTILE, scream ? 1.5F : 1.0F, 1.0F);
        }
        Diagnostics.log("Observador iniciou perseguição");
    }

    private void attack() {
        this.attackCooldown = 25;
        if (this.mob.level() instanceof ServerLevel serverLevel
                && serverLevel.getDifficulty() != Difficulty.PEACEFUL) {
            float damage = (float) (this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * aggression());
            if (this.target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(this.mob), damage)) {
                this.hits++;
                if (this.mob.getRandom().nextDouble() < Math.min(1.0, HorrorConfig.JUMPSCARE_FREQUENCY.get() * 1.5)) {
                    ModSounds.play(serverLevel, this.target.getX(), this.target.getEyeY(), this.target.getZ(),
                            ModSounds.JUMP, SoundSource.HOSTILE, 2.5F, 1.0F);
                }
                Diagnostics.log("Observador acertou o jogador (dano={}, golpes={})", damage, this.hits);
            }
        }
    }
}
