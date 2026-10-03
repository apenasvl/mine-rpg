package com.rpgstats.combat;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Payoffs de especializacoes do Assassino que precisam de um proc real no evento de dano.
 *
 * A maior parte da identidade continua em ClassMechanics. Esta camada e propositalmente pequena:
 * efeitos de fim de build so aparecem depois dos nodes de especializacao correspondentes e todos
 * os procs ofensivos possuem limite/ICD para que o Assassino continue sendo burst preparado, nao
 * dano permanente acima do Guerreiro.
 */
@Mod.EventBusSubscriber(modid = RPGStatsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AssassinSpecializationHandler {
    private static final ThreadLocal<Boolean> PROC_GUARD = ThreadLocal.withInitial(() -> false);
    /** Opening precisa sobreviver ate ClassMechanics.onHit; limpamos somente no LOWEST do mesmo hit. */
    private static final Map<UUID, UUID> OPENING_HIT_PENDING = new HashMap<>();

    /**
     * Antes do pipeline principal registramos a janela preparada e gastamos Combo uma unica vez.
     * Antes do node Risk (45), Combo melhora economia/rotacao em vez de conceder dano bruto gratis.
     * O bonus ofensivo de Risk ja e calculado no pipeline principal antes deste evento.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void beforeClassPipeline(LivingHurtEvent event) {
        if (PROC_GUARD.get() || ProcDamageQueue.isApplying() || event.getEntity().getWorld().isClient) return;
        LivingEntity target = event.getEntity();
        if (!(event.getSource().getAttacker() instanceof ServerPlayerEntity player)) return;
        if (event.getSource().getSource() != player) return; // somente golpe corpo a corpo direto

        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.ASSASSINO) return;
        CombatState state = CombatState.get(player.getUuid());
        CombatState.ClassTargetState ts = state.classTarget(target.getUuid());

        boolean opening = state.timer("ass_opening") > 0 || ts.openingTicks > 0;
        if (opening) OPENING_HIT_PENDING.put(player.getUuid(), target.getUuid());
        else OPENING_HIT_PENDING.remove(player.getUuid());

        // Cada uma destas especializacoes tem um verbo proprio. Nenhuma delas adiciona um
        // multiplicador universal: o payoff depende da janela que a propria build preparou.
        counterblade(player, target, stats, state);
        voidwalker(player, target, stats, state);

        boolean prepared = opening
                || state.timer("ass_riposte") > 0
                || state.timer("ass_soul_strike") > 0
                || state.timer("ass_execute_window") > 0
                || state.timer("ass_void_debt") > 0;
        float combo = state.gauge("ass_combo");
        if (prepared && combo >= 3f && state.timer("internal_ass_combo_spend") <= 0) {
            float spent = Math.min(1.5f, combo);
            state.setGauge("ass_combo", combo - spent, 5f);
            state.startTimer("internal_ass_combo_spend", 4);
            refundResource(player, stats, spent * .70f);
        }

        soulknife(player, target, stats, state);
    }

    /**
     * Contra-Lamina nao ganha dano passivo. A identidade vem de converter uma Riposte real em
     * controle do alvo + uma janela curtissima de resistencia. Conversion (40) devolve um pouco
     * de Energia, recompensando precisao defensiva sem transformar o parry em spam.
     */
    private static void counterblade(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (stats.specialization != RPGSpecialization.COUNTERBLADE) return;
        if (!stats.unlockedNodes.contains("ass_duel_counter_engine")) return;
        if (state.timer("ass_riposte") <= 0 || state.timer("internal_ass_counterblade") > 0) return;

        int controlTicks = com.rpgstats.boss.BossScaler.getTier(target) > 0 ? 20 : 42;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, controlTicks, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 24, 0));
        state.startTimer("internal_ass_counterblade", 14);

        if (stats.unlockedNodes.contains("ass_duel_counter_conversion"))
            refundResource(player, stats, .65f);
    }

    /**
     * Lâmina da Alma e a rota de dano mistico: Ecos sao consumidos para um corte magico separado,
     * pequeno e com ICD. Conversion transforma parte desse ciclo em sustain de Energia.
     */
    private static void soulknife(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (stats.specialization != RPGSpecialization.SOULKNIFE) return;
        if (!stats.unlockedNodes.contains("ass_myst_soul_engine")) return;
        if (state.timer("ass_soul_strike") <= 0 || state.timer("internal_ass_soulknife") > 0) return;
        float available = state.gauge("ass_echo");
        if (available < .75f) return;

        float echo = Math.min(2f, available);
        state.setGauge("ass_echo", available - echo, 6f);
        state.startTimer("internal_ass_soulknife", 12);
        float extra = Math.min(3.0f, 1.5f + echo * 0.75f);
        procDamageAfterHit(player, target, extra);

        if (stats.unlockedNodes.contains("ass_myst_soul_conversion"))
            refundResource(player, stats, .30f + echo * .15f);
    }

    /**
     * Andarilho do Vazio assume vulnerabilidade enquanto carrega Void Debt. Ao conectar o golpe
     * depois do blink, ele converte esse risco em reposicionamento/controle, nao em outro nuke.
     * Conversion (40) adiciona ruptura curta: Fraqueza no alvo e resistencia minima no usuario.
     */
    private static void voidwalker(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (stats.specialization != RPGSpecialization.VOIDWALKER) return;
        if (!stats.unlockedNodes.contains("ass_myst_void_engine")) return;
        if (state.timer("ass_void_debt") <= 0 || state.timer("internal_ass_void_breach") > 0) return;

        int slowTicks = com.rpgstats.boss.BossScaler.getTier(target) > 0 ? 14 : 30;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 30, 0));
        state.startTimer("internal_ass_void_breach", 14);

        if (stats.unlockedNodes.contains("ass_myst_void_conversion")) {
            int weaknessTicks = com.rpgstats.boss.BossScaler.getTier(target) > 0 ? 14 : 30;
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, weaknessTicks, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 16, 0));
        }
    }

    /**
     * Demolitionist precisa rodar depois do pipeline principal, pois e ClassMechanics que armazena
     * pressao em ClassTargetState.stored durante a janela ass_demolition. Wiremaster tambem usa o
     * Slow que o Sabotador acabou de aplicar no alvo.
     */
    /**
     * Chamado pelo CombatHandler somente depois de LivingEntity.damage confirmar perda real de HP.
     * O antigo LivingHurtEvent LOWEST ainda acontecia antes do RETURN do damage(), portanto limpava
     * Opening antes de ClassMechanics/resourceGainOnHit terminarem o mesmo golpe.
     */
    public static void onConfirmedMeleeHit(ServerPlayerEntity player, LivingEntity target, float damage) {
        if (damage <= 0f) return;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.ASSASSINO) return;
        CombatState state = CombatState.get(player.getUuid());

        UUID pendingTarget = OPENING_HIT_PENDING.remove(player.getUuid());
        if (pendingTarget != null && pendingTarget.equals(target.getUuid())) {
            nightbladeEscape(player, stats, state);
            state.setTimer("ass_opening", 0);
            state.classTarget(target.getUuid()).openingTicks = 0;
        }

        // Estes payoffs dependem do estado produzido por ClassMechanics.onHit no golpe confirmado.
        if (stats.specialization == RPGSpecialization.DEMOLITIONIST)
            demolitionist(player, target, stats, state);
        else if (stats.specialization == RPGSpecialization.WIREMASTER)
            wiremaster(target, stats, state);
    }

    /**
     * Lâmina Noturna e o assassino de emboscada puro. Engine (30) transforma uma Opening consumida
     * em uma retirada curta; Conversion (40) recupera Energia. Nao existe segundo hit gratuito.
     */
    private static void nightbladeEscape(ServerPlayerEntity player, PlayerStats stats, CombatState state) {
        if (stats.specialization != RPGSpecialization.NIGHTBLADE) return;
        if (!stats.unlockedNodes.contains("ass_shadow_night_engine")) return;
        if (state.timer("internal_ass_nightblade_escape") > 0) return;

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 42, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 18, 0));
        state.startTimer("ass_escape", 42);
        state.startTimer("internal_ass_nightblade_escape", 30);

        if (stats.unlockedNodes.contains("ass_shadow_night_conversion"))
            refundResource(player, stats, .80f);
    }

    /**
     * Demolidor e a unica destas cinco builds cujo payoff principal e explosao real: armazena
     * pressao, detona uma vez e, no lvl 40+, espalha uma parte limitada para ate tres alvos.
     */
    private static void demolitionist(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (!stats.unlockedNodes.contains("ass_sabo_demo_engine")) return;
        if (state.timer("ass_demolition") <= 0 || state.timer("internal_ass_demo_detonation") > 0) return;

        CombatState.ClassTargetState ts = state.classTarget(target.getUuid());
        float stored = ts.stored;
        if (stored < 2.5f) return;

        boolean conversion = stats.unlockedNodes.contains("ass_sabo_demo_conversion");
        float release = conversion
                ? Math.min(4.5f, stored * 0.75f)
                : Math.min(3.5f, stored * 0.60f);
        ts.stored = 0f;
        state.startTimer("internal_ass_demo_detonation", 28);
        procDamageAfterHit(player, target, release);

        // Conversion (nivel 40) transforma parte da detonacao em area; nunca duplica no alvo original.
        if (!conversion) return;
        float splash = Math.min(2.25f, release * 0.45f);
        Box area = target.getBoundingBox().expand(3.25);
        int hit = 0;
        for (LivingEntity extra : player.getServerWorld().getEntitiesByClass(LivingEntity.class, area,
                e -> validExtraTarget(player, target, e))) {
            procDamage(player, extra, splash);
            extra.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 25, 0));
            if (++hit >= 3) break;
        }
    }

    private static void wiremaster(LivingEntity target, PlayerStats stats, CombatState state) {
        if (!stats.unlockedNodes.contains("ass_sabo_wire_engine")) return;
        if (!target.hasStatusEffect(StatusEffects.SLOWNESS) || state.timer("internal_ass_wiremaster") > 0) return;

        boolean conversion = stats.unlockedNodes.contains("ass_sabo_wire_conversion");
        int duration = conversion ? 55 : 38;
        // Bosses recebem janela curta de controle; a identidade do Wiremaster nao depende de hard-CC.
        if (com.rpgstats.boss.BossScaler.getTier(target) > 0) duration = Math.max(16, duration / 2);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0));
        state.startTimer("internal_ass_wiremaster", 14);
    }

    private static void refundResource(ServerPlayerEntity player, PlayerStats stats, float amount) {
        if (amount <= .001f || stats.resource >= stats.resourceMax) return;
        float before = stats.resource;
        stats.resource = Math.min(stats.resourceMax, stats.resource + amount);
        if (stats.resource > before + .001f) StatsManager.saveAndSync(player, stats);
    }

    private static boolean validExtraTarget(ServerPlayerEntity player, LivingEntity original, LivingEntity target) {
        if (!target.isAlive() || target == original || target == player || target instanceof PlayerEntity) return false;
        if (target instanceof PassiveEntity) return false;
        return !(target instanceof TameableEntity tameable) || tameable.getOwner() != player;
    }

    private static void procDamageAfterHit(ServerPlayerEntity player, LivingEntity target, float amount) {
        ProcDamageQueue.queueSameTarget(player, target, amount);
    }

    private static void procDamage(ServerPlayerEntity player, LivingEntity target, float amount) {
        if (amount <= .01f || !target.isAlive()) return;
        try {
            PROC_GUARD.set(true);
            target.damage(player.getDamageSources().indirectMagic(player, player), amount);
        } finally {
            PROC_GUARD.set(false);
        }
    }

    private AssassinSpecializationHandler() {}
}
