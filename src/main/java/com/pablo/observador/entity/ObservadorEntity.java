package com.pablo.observador.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * O Observador: humanoide alto e magro que observa de longe, segue sem ser visto
 * e só às vezes persegue. Todo o comportamento fica em {@link ObservadorBehaviorGoal}.
 *
 * Contramedidas que o jogador pode descobrir:
 *  - luz forte por perto faz a criatura desaparecer;
 *  - acertá-la com um golpe a faz recuar;
 *  - ela só "sente" o jogador por visão direta ou por som de corrida.
 *
 * Uma "miragem" é a mesma silhueta, mas inofensiva: nasce nas bordas da visão do jogador
 * com sanidade baixa e some em um segundo quando ele olha para ela.
 */
public class ObservadorEntity extends PathfinderMob {
    /** Quanto o jogador já encontrou a criatura (0 a 100). Deixa a IA mais ousada. */
    public int tension = 0;

    /** Miragem: não ataca, não anda, some quando observada. */
    public boolean mirage = false;

    /** Usado pelo comando /observador perseguir: faz a IA iniciar uma perseguição no próximo tick. */
    public boolean forceChase = false;
    /** Criada por comando de teste: a luz do dia não a faz sumir. */
    public boolean testMode = false;

    /** Usado pelo comando /observador chase: nasce já perseguindo. */
    public boolean startInChase = false;

    private boolean hurtByPlayer = false;

    public ObservadorEntity(EntityType<? extends ObservadorEntity> type, Level level) {
        super(type, level);
        // Os sons dela são tocados pelo mod (ModSounds); os do jogo base ficam desligados.
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ObservadorBehaviorGoal(this));
    }

    /** Sem nome flutuante sobre a cabeça: o jogador não deve saber o que é. */
    @Override
    public boolean shouldShowName() {
        return false;
    }

    /** Nome vazio: qualquer lugar que exibir o nome da criatura (etiqueta, mira, mensagens) mostra nada. */
    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.mirage) {
            return false; // a miragem não existe de verdade
        }
        boolean result = super.hurtServer(level, source, amount);
        if (result && source.getEntity() instanceof Player) {
            this.hurtByPlayer = true;
        }
        return result;
    }

    /** Devolve true uma única vez depois que um jogador acertou a criatura. */
    public boolean consumeHurtFlag() {
        boolean flag = this.hurtByPlayer;
        this.hurtByPlayer = false;
        return flag;
    }
}
