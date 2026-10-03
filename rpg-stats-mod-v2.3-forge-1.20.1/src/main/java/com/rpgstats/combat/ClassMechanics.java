package com.rpgstats.combat;

import com.rpgstats.ability.ClassAbilityRegistry;
import com.rpgstats.ability.SkillEffect;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.UUID;

/**
 * Loops server-authoritative das quatro classes nao-Mago.
 *
 * Os ids antigos das arvores sao preservados para saves, mas cada node agora participa do loop
 * real da Casa/Especializacao. Foundation habilita o verbo, Discipline melhora consistencia,
 * Setup cria preparacao, Reaction muda resposta defensiva/posicionamento, Synergy conecta a tecnica;
 * Initiation/Engine/Conversion/Risk fazem o mesmo dentro da fantasia exclusiva da especializacao.
 */
public final class ClassMechanics {
    private static final float GAUGE_CAP = 10f;

    private ClassMechanics() {}

    public static boolean handles(PlayerStats stats) {
        return stats != null && stats.clazz != null && stats.clazz != RPGClass.MAGO;
    }

    public static float resourceGainOnHit(ServerPlayerEntity player, LivingEntity target, PlayerStats stats,
                                          boolean projectile, boolean magic) {
        if (!handles(stats) || magic) return 0f;
        CombatState state = CombatState.get(player.getUuid());
        double distance = Math.sqrt(player.squaredDistanceTo(target));
        float base = switch (stats.clazz) {
            case GUERREIRO -> projectile ? .2f : 1.15f;
            case ARQUEIRO -> projectile ? (distance >= 9 && distance <= 27 ? 2.7f : .9f) : .25f;
            case ASSASSINO -> projectile ? .45f : (isBehind(player, target) || state.timer("ass_opening") > 0 ? 2.2f : 1.15f);
            case MAGO -> 0f;
        };
        if (coreHas(stats, "flow")) base *= 1.25f;
        if (coreHas(stats, "tempo") && state.timer("class_tempo") > 0) {
            if (stats.clazz == RPGClass.ARQUEIRO && projectile) base += .45f;
            if (stats.clazz == RPGClass.ASSASSINO && !projectile) base += .45f;
        }
        if (coreHas(stats, "combo") && state.gauge("core_sequence") >= 2f) {
            base += .6f;
            state.setGauge("core_sequence", 0f, 3f);
        } else state.addGauge("core_sequence", 1f, 3f);
        return base;
    }

    public static float resourceGainOnHurt(PlayerStats stats) {
        if (!handles(stats)) return 0f;
        float base = switch (stats.clazz) {
            case GUERREIRO -> 1.1f;
            default -> 0f;
        };
        if (coreHas(stats, "flow")) base *= 1.2f;
        if (pathScale(stats, RPGPath.WAR_BERSERKER) > 0) base += .8f * pathScale(stats, RPGPath.WAR_BERSERKER);
        return base;
    }

    public static float resourceGainOnKill(PlayerStats stats, LivingEntity victim) {
        if (!handles(stats)) return 0f;
        boolean boss = BossScaler.getTier(victim) > 0;
        float base = switch (stats.clazz) {
            case GUERREIRO -> boss ? 4f : 1.3f;
            case ARQUEIRO -> boss ? 3.5f : 1.0f;
            case ASSASSINO -> boss ? 3.5f : 1.4f;
            case MAGO -> 0f;
        };
        if (coreHas(stats, "sustain")) base *= 1.35f;
        return base;
    }

    public static float modifyOutgoingDamage(ServerPlayerEntity player, Entity rawTarget, PlayerStats stats,
                                             float amount, DamageSource source, boolean projectile, boolean magic) {
        if (!handles(stats) || !(rawTarget instanceof LivingEntity target) || amount <= 0f) return amount;
        CombatState state = CombatState.get(player.getUuid());
        CombatState.ClassTargetState ts = state.classTarget(target.getUuid());
        float bonus = 0f;

        switch (stats.clazz) {
            case GUERREIRO -> {
                if (!projectile && !magic) {
                    if (ts.openingTicks > 0) bonus += .06f;
                    if (state.timer("war_frenzy") > 0) bonus += .08f;
                    if (state.timer("war_riposte") > 0) bonus += .10f;
                    if (state.timer("war_runic_strike") > 0) bonus += .05f;
                    if (state.timer("war_order_offense") > 0) bonus += .04f;
                    bonus += warriorPathDamage(stats, state, target, ts);
                    bonus += warriorSpecDamage(stats, state, target, ts);
                }
            }
            case ARQUEIRO -> {
                if (projectile && !magic) {
                    if (coreHas(stats, "tempo") && state.timer("class_tempo") > 0) bonus += .03f;
                    double distance = Math.sqrt(player.squaredDistanceTo(target));
                    bonus += archerPathDamage(stats, state, target, ts, distance);
                    bonus += archerSpecDamage(player, stats, state, target, ts, distance);
                    if (state.timer("arc_prepared_shot") > 0) bonus += .08f;
                    if (state.timer("arc_ambush") > 0) bonus += .06f;
                }
            }
            case ASSASSINO -> {
                if (!projectile && !magic) {
                    if (coreHas(stats, "tempo") && state.timer("class_tempo") > 0) bonus += .03f;
                    if (isBehind(player, target) || ts.openingTicks > 0 || state.timer("ass_opening") > 0) bonus += .08f + .12f*com.rpgstats.balance.ClassBalance.levelProgress(stats.level);
                    if (state.timer("ass_riposte") > 0) bonus += .10f;
                    if (state.timer("ass_soul_strike") > 0) bonus += .06f;
                    bonus += assassinPathDamage(stats, state, target, ts);
                    bonus += assassinSpecDamage(stats, state, target, ts);
                }
            }
            default -> { }
        }

        // Risk nodes aumentam pressao apenas dentro da propria condicao da build.
        if (specHas(stats, "_risk")) {
            if (stats.clazz == RPGClass.GUERREIRO && player.getHealth() < player.getMaxHealth() * .45f) bonus += .035f;
            if (stats.clazz == RPGClass.ASSASSINO && state.gauge("ass_combo") >= 3f) bonus += .035f;
            if (stats.clazz == RPGClass.ARQUEIRO && projectile && player.squaredDistanceTo(target) >= 144) bonus += .03f;
        }

        if(stats.clazz==RPGClass.ASSASSINO)
            return amount*Math.min(1.32f+.23f*com.rpgstats.balance.ClassBalance.levelProgress(stats.level),1f+Math.max(0f,bonus));
        return amount * Math.min(1.32f, 1f + Math.max(0f, bonus));
    }

    public static float modifyIncomingDamage(ServerPlayerEntity player, PlayerStats stats, float amount, DamageSource source) {
        if (!handles(stats) || amount <= 0f) return amount;
        CombatState state = CombatState.get(player.getUuid());
        float reduction = 0f;

        if (stats.clazz == RPGClass.GUERREIRO) {
            float vanguard = pathScale(stats, RPGPath.WAR_VANGUARD);
            if (vanguard > 0) reduction += Math.min(.11f, state.gauge("war_guard") * .010f) * vanguard;
            if (state.timer("war_guard_stance") > 0) reduction += .10f;
            if (state.timer("war_juggernaut") > 0) reduction += .07f;
            if (state.timer("war_order_defense") > 0) reduction += .05f;
            if (state.timer("war_exhaustion") > 0) amount *= 1.08f;
            if (state.timer("war_parry") > 0 && isDirectMeleeDamage(source)) {
                reduction += .28f;
            }
        } else if (stats.clazz == RPGClass.ARQUEIRO) {
            if (state.timer("arc_survival") > 0) reduction += .09f;
            if (state.timer("arc_reposition_guard") > 0) reduction += .05f;
            if (specEnabled(stats, RPGSpecialization.SURVIVALIST)) reduction += .025f;
            Entity attacker = source == null ? null : source.getAttacker();
            if (coreHas(stats, "guard") && attacker != null && attacker.squaredDistanceTo(player) >= 49d)
                reduction += .04f;
        } else if (stats.clazz == RPGClass.ASSASSINO) {
            if (state.timer("ass_phase") > 0) reduction += .22f;
            if (state.timer("ass_escape") > 0) reduction += .06f;
            if (coreHas(stats, "guard") && state.timer("ass_escape") > 0) reduction += .04f;
            if (state.timer("ass_parry") > 0 && isDirectMeleeDamage(source)) {
                reduction += .32f;
            }
        }

        if ((stats.clazz == RPGClass.ARQUEIRO || stats.clazz == RPGClass.ASSASSINO)
                && coreHas(stats, "resolve")) {
            if (player.getHealth() < player.getMaxHealth() * .40f && state.timer("class_resolve_cd") <= 0) {
                state.startTimer("class_resolve", 60);
                state.startTimer("class_resolve_cd", 400);
            }
            if (state.timer("class_resolve") > 0) reduction += .08f;
        }

        // Risk e realmente trade-off, nao buff gratuito.
        if (specHas(stats, "_risk")) {
            if (stats.clazz == RPGClass.GUERREIRO && state.timer("war_frenzy") > 0) amount *= 1.035f;
            if (stats.clazz == RPGClass.ASSASSINO && state.timer("ass_void_debt") > 0) amount *= 1.06f;
        }
        return amount * (1f - Math.min(.35f, Math.max(0f, reduction)));
    }

    public static void onHit(ServerPlayerEntity player, LivingEntity target, PlayerStats stats,
                             float damage, DamageSource source, boolean projectile, boolean magic) {
        if (!handles(stats) || damage <= 0f || magic) return;
        CombatState state = CombatState.get(player.getUuid());
        UUID previous = state.lastClassTarget;
        CombatState.ClassTargetState ts = state.classTarget(target.getUuid());
        boolean duelCadenceWasActive = state.timer("ass_duel_cadence") > 0;

        applyPrimaryAndSecondaryPathHit(player, target, stats, state, ts, damage, projectile, previous);
        applySpecializationHit(player, target, stats, state, ts, damage, projectile, previous, duelCadenceWasActive);
        state.lastClassTarget = target.getUuid();

    }

    /**
     * Consome janelas de "proximo golpe" somente depois de um acerto realmente confirmado e
     * apenas quando o tipo de ataque poderia ter recebido o beneficio. Mantemos isso separado de
     * onHit para que payoffs de especializacao ainda enxerguem a janela durante o mesmo golpe.
     */
    public static void consumePreparedHit(ServerPlayerEntity player, PlayerStats stats,
                                          boolean projectile, boolean magic) {
        if (!handles(stats)) return;
        CombatState state = CombatState.get(player.getUuid());
        if (stats.clazz == RPGClass.GUERREIRO && !projectile && !magic) {
            if (state.timer("war_riposte") > 0) state.setTimer("war_riposte", 0);
            if (state.timer("war_runic_strike") > 0) state.setTimer("war_runic_strike", 0);
        } else if (stats.clazz == RPGClass.ARQUEIRO && projectile && !magic) {
            if (state.timer("arc_prepared_shot") > 0) state.setTimer("arc_prepared_shot", 0);
            if (state.timer("arc_ambush") > 0) state.setTimer("arc_ambush", 0);
            if (coreHas(stats, "tempo") && state.timer("class_tempo") > 0) state.setTimer("class_tempo", 0);
        } else if (stats.clazz == RPGClass.ASSASSINO && !projectile && !magic) {
            if (state.timer("ass_riposte") > 0) state.setTimer("ass_riposte", 0);
            if (state.timer("ass_soul_strike") > 0) state.setTimer("ass_soul_strike", 0);
            if (coreHas(stats, "tempo") && state.timer("class_tempo") > 0) state.setTimer("class_tempo", 0);
        }
    }

    public static void onHurt(ServerPlayerEntity player, PlayerStats stats, float amount, DamageSource source) {
        if (!handles(stats) || amount <= 0f) return;
        CombatState state = CombatState.get(player.getUuid());

        // Scaling may run for a hit that Forge later cancels or armor/absorption fully negates.
        // Consume the parry and grant its payoff only on the confirmed incoming damage callback.
        if (isDirectMeleeDamage(source)) {
            if (stats.clazz == RPGClass.GUERREIRO && state.timer("war_parry") > 0) {
                state.setTimer("war_parry", 0);
                state.startTimer("war_riposte", 70);
                if (specEnabled(stats, RPGSpecialization.DUEL_MASTER) && specHas(stats, "_engine"))
                    state.addGauge("war_guard", .8f, 10f);
            } else if (stats.clazz == RPGClass.ASSASSINO && state.timer("ass_parry") > 0) {
                state.setTimer("ass_parry", 0);
                state.startTimer("ass_riposte", 70);
                state.addGauge("ass_advantage", 1f, 5f);
                if (pathHas(stats, RPGPath.ASS_DUELIST, "_reaction"))
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 30, 0));
            }
        }

        if (stats.clazz == RPGClass.GUERREIRO) {
            state.addGauge("war_impact", Math.min(1.5f, amount * .12f), GAUGE_CAP);
            addPathGauge(stats, state, RPGPath.WAR_VANGUARD, "war_guard", Math.min(2f, amount * .20f));
            addPathGauge(stats, state, RPGPath.WAR_BERSERKER, "war_fury_loop", Math.min(1.7f, amount * .16f));
            if (specEnabled(stats, RPGSpecialization.PAIN_COLOSSUS))
                state.addGauge("war_pain", Math.min(2.5f, amount * .30f), GAUGE_CAP);
            if (specEnabled(stats, RPGSpecialization.JUGGERNAUT)) state.addGauge("war_inertia", .6f, 6f);
        } else if (stats.clazz == RPGClass.ARQUEIRO) {
            state.setGauge("arc_aim", state.gauge("arc_aim") - 1.5f, GAUGE_CAP);
            if (pathScale(stats, RPGPath.ARC_WARDEN) > 0 && pathHas(stats, RPGPath.ARC_WARDEN, "_reaction"))
                state.startTimer("arc_survival", 55);
        } else if (stats.clazz == RPGClass.ASSASSINO) {
            state.setGauge("ass_shadow_prep", 0f, 1f);
            state.setGauge("ass_combo", state.gauge("ass_combo") - 1.5f, GAUGE_CAP);
        }
    }

    public static void onKill(ServerPlayerEntity player, LivingEntity victim, PlayerStats stats) {
        if (!handles(stats)) return;
        CombatState state = CombatState.get(player.getUuid());
        if (stats.clazz == RPGClass.GUERREIRO) {
            if (state.timer("war_frenzy") > 0 && pathHas(stats, RPGPath.WAR_BERSERKER, "_synergy")) state.startTimer("war_frenzy", 55);
            addPathGauge(stats, state, RPGPath.WAR_COMMANDER, "war_morale", 1.6f);
        } else if (stats.clazz == RPGClass.ARQUEIRO) {
            if (state.lastClassTarget != null && state.lastClassTarget.equals(victim.getUuid())) state.addGauge("arc_focus", 1.2f, GAUGE_CAP);
        } else if (stats.clazz == RPGClass.ASSASSINO) {
            if (specEnabled(stats, RPGSpecialization.EXECUTIONER)) state.startTimer("ass_escape", 70);
            state.addGauge("ass_energy_loop", .8f, GAUGE_CAP);
        }
    }

    public static void tick(ServerPlayerEntity player, PlayerStats stats) {
        if (!handles(stats)) return;
        CombatState state = CombatState.get(player.getUuid());
        double dx = player.getX() - state.lastClassX;
        double dz = player.getZ() - state.lastClassZ;
        double movedSq = (state.lastClassX == 0d && state.lastClassZ == 0d) ? 0d : dx * dx + dz * dz;

        if (stats.clazz == RPGClass.GUERREIRO && specEnabled(stats, RPGSpecialization.JUGGERNAUT)) {
            if (movedSq > .006) state.addGauge("war_inertia", .03f, 6f);
            else state.setGauge("war_inertia", state.gauge("war_inertia") - .04f, 6f);
        }
        if (stats.clazz == RPGClass.GUERREIRO && specEnabled(stats, RPGSpecialization.STORMBLADE)) {
            if (movedSq > .006) {
                state.startTimer("war_storm_moving", 12);
                state.addGauge("war_storm_charge", .018f, 5f);
            } else if (state.timer("war_storm_moving") <= 0) {
                state.setGauge("war_storm_charge", state.gauge("war_storm_charge") - .025f, 5f);
            }
        }
        if (stats.clazz == RPGClass.ARQUEIRO) {
            float skirm = pathScale(stats, RPGPath.ARC_SKIRMISHER);
            if (skirm > 0) {
                if (movedSq > .006) state.addGauge("arc_momentum", .03f * skirm, GAUGE_CAP);
                else state.setGauge("arc_momentum", state.gauge("arc_momentum") - .02f, GAUGE_CAP);
            }
            if (specEnabled(stats, RPGSpecialization.SNIPER)) {
                float stationaryGain = specHas(stats, "_engine") ? .03125f : .025f;
                if (movedSq < .0008) state.addGauge("arc_stationary", stationaryGain, 5f);
                else state.setGauge("arc_stationary", 0f, 5f);
            }
        }
        if (stats.clazz == RPGClass.ASSASSINO && pathScale(stats, RPGPath.ASS_SHADOW) > 0) {
            if (state.combatTicks == 0) {
                float shadowScale = pathScale(stats, RPGPath.ASS_SHADOW)
                        * (pathHas(stats, RPGPath.ASS_SHADOW, "_discipline") ? 1.20f : 1f);
                float prep = state.addGauge("ass_shadow_prep", .018f * shadowScale, 1f);
                if (prep >= .99f && pathHas(stats, RPGPath.ASS_SHADOW, "_setup")) state.startTimer("ass_opening", 40);
            }
        }

        state.lastClassX = player.getX();
        state.lastClassY = player.getY();
        state.lastClassZ = player.getZ();
    }

    public static void activate(ServerPlayerEntity player, PlayerStats stats, String nodeId, SkillEffect effect) {
        activate(player, stats, nodeId, effect, false);
    }

    public static void activate(ServerPlayerEntity player, PlayerStats stats, String nodeId, SkillEffect effect, boolean externalMovement) {
        if (!handles(stats) || effect == null || !ClassAbilityRegistry.CLASS_ACTIVE.equals(effect.effectId())) return;
        String real = HouseRules.real(nodeId);
        float scale = Math.max(.45f, Math.min(1f, effect.value()));
        CombatState state = CombatState.get(player.getUuid());
        if ((stats.clazz == RPGClass.ARQUEIRO || stats.clazz == RPGClass.ASSASSINO) && coreHas(stats, "tempo"))
            state.startTimer("class_tempo", 80);
        if (real.endsWith("_core_utility")) {
            activateCore(player, stats, scale);
            return;
        }
        RPGSpecialization spec = RPGSpecialization.ownerOfNode(real);
        if (spec != null && stats.clazz == RPGClass.ARQUEIRO) {
            ArcherTechniqueHandler.activate(player, stats, spec, real, scale, externalMovement);
            return;
        }
        if (spec != null) {
            activateSpecialization(player, stats, spec, real, scale);
            return;
        }
        RPGPath path = RPGPath.ownerOfNode(real);
        if (path != null) activatePath(player, stats, path, real, scale);
    }

    private static void applyPrimaryAndSecondaryPathHit(ServerPlayerEntity player, LivingEntity target, PlayerStats stats,
                                                        CombatState state, CombatState.ClassTargetState ts, float damage,
                                                        boolean projectile, UUID previous) {
        if (stats.path != null) applyPathHit(player, target, stats, stats.path, 1f, state, ts, damage, projectile, previous);
        if (stats.affinityHouse != null) {
            float scale = pathScale(stats, stats.affinityHouse);
            if (scale > 0) applyPathHit(player, target, stats, stats.affinityHouse, scale, state, ts, damage, projectile, previous);
        }
    }

    private static void applyPathHit(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, RPGPath path, float scale,
                                     CombatState state, CombatState.ClassTargetState ts, float damage, boolean projectile, UUID previous) {
        if (pathScale(stats, path) <= 0) return;
        float discipline = pathHas(stats, path, "_discipline") ? 1.20f : 1f;
        float gainScale = scale * discipline;

        switch (path) {
            case WAR_VANGUARD -> {
                if (projectile) return;
                state.addGauge("war_guard", .35f * gainScale, GAUGE_CAP);
                addImpact(target, ts, damage, .85f * gainScale, stats, state);
                if (pathHas(stats, path, "_setup") && state.gauge("war_guard") >= 3f) state.startTimer("war_guard_ready", 80);
            }
            case WAR_BERSERKER -> {
                if (projectile) return;
                state.addGauge("war_fury_loop", .65f * gainScale, GAUGE_CAP);
                addImpact(target, ts, damage, .75f * gainScale, stats, state);
                if (pathHas(stats, path, "_setup") && player.getHealth() < player.getMaxHealth() * .55f) state.startTimer("war_bers_setup", 80);
            }
            case WAR_WEAPONMASTER -> {
                if (projectile) return;
                String weapon = player.getMainHandStack().getItem().toString();
                boolean changed = !state.lastWeaponKey.isBlank() && !state.lastWeaponKey.equals(weapon);
                if (changed) {
                    state.addGauge("war_weapon_flow", 1.4f * gainScale, 5f);
                    if (pathHas(stats, path, "_setup")) state.startTimer("war_weapon_opening", 90);
                } else state.addGauge("war_weapon_flow", .25f * gainScale, 5f);
                state.lastWeaponKey = weapon;
                addImpact(target, ts, damage, .85f * gainScale, stats, state);
            }
            case WAR_RUNIC -> {
                if (projectile) return;
                state.addGauge("war_runes", .5f * gainScale, 5f);
                if (pathHas(stats, path, "_setup") && state.gauge("war_runes") >= 3f) state.startTimer("war_rune_ready", 100);
            }
            case WAR_COMMANDER -> {
                if (projectile) return;
                state.addGauge("war_morale", .3f * gainScale, GAUGE_CAP);
                if (pathHas(stats, path, "_setup") && previous != null && !previous.equals(target.getUuid()))
                    state.addGauge("war_morale", .35f * gainScale, GAUGE_CAP);
            }
            case ARC_MARKSMAN -> {
                if (!projectile) return;
                double distance = Math.sqrt(player.squaredDistanceTo(target));
                if (distance >= 9 && distance <= 28) state.addGauge("arc_aim", .9f * gainScale, GAUGE_CAP);
                else state.setGauge("arc_aim", state.gauge("arc_aim") - 1.2f, GAUGE_CAP);
                if (consumePathSynergy(state, "arc_mark_synergy"))
                    state.addGauge("arc_aim", .60f * gainScale, GAUGE_CAP);
                ts.markTicks = 100;
                ts.stacks = Math.min(6, ts.stacks + 1);
                if (pathHas(stats, path, "_setup") && state.gauge("arc_aim") >= 3f) state.startTimer("arc_prepared_shot", 80);
            }
            case ARC_WARDEN -> {
                if (!projectile) return;
                ts.markTicks = 150;
                float instinctGain = .7f * gainScale;
                if (state.timer("arc_hunt") > 0) instinctGain += .25f * gainScale;
                state.addGauge("arc_instinct", instinctGain, GAUGE_CAP);
                if (pathHas(stats, path, "_setup")) state.startTimer("arc_hunt", 100);
            }
            case ARC_SKIRMISHER -> {
                if (!projectile) return;
                state.addGauge("arc_momentum", .55f * gainScale, GAUGE_CAP);
                if (consumePathSynergy(state, "arc_skirm_synergy"))
                    state.addGauge("arc_momentum", .75f * gainScale, GAUGE_CAP);
                if (pathHas(stats, path, "_setup") && state.gauge("arc_momentum") >= 3f) state.startTimer("arc_ambush", 70);
            }
            case ARC_ARCANE -> {
                if (!projectile) return;
                ts.stackProgress += gainScale;
                if (consumePathSynergy(state, "arc_magic_synergy")) ts.stackProgress += .75f * scale;
                flushStackProgress(ts, 5);
                ts.markTicks = 100;
                applyArcaneArrowState(target, state.classMode, ts.stacks);
                if (pathHas(stats, path, "_setup") && ts.stacks >= 3) state.startTimer("arc_elemental_reaction", 70);
            }
            case ARC_ARTIFICER -> {
                if (!projectile) return;
                state.addGauge("arc_device", .65f * gainScale, 5f);
                if (consumePathSynergy(state, "arc_art_synergy"))
                    state.addGauge("arc_device", .55f * gainScale, 5f);
                if (pathHas(stats, path, "_setup") && state.gauge("arc_device") >= 3f) state.startTimer("arc_device_ready", 100);
            }
            case ASS_SHADOW -> {
                if (projectile) return;
                if (isBehind(player, target) || state.gauge("ass_shadow_prep") >= .95f) {
                    ts.openingTicks = Math.max(ts.openingTicks, Math.round(65 * scale));
                    state.setGauge("ass_shadow_prep", 0f, 1f);
                }
                if (consumePathSynergy(state, "ass_shadow_synergy")) state.startTimer("ass_escape", 45);
                if (pathHas(stats, path, "_setup") && ts.openingTicks > 0) state.startTimer("ass_opening", 65);
            }
            case ASS_VENOM -> {
                if (projectile) return;
                ts.doseProgress += gainScale;
                if (consumePathSynergy(state, "ass_venom_synergy")) ts.doseProgress += .75f * scale;
                if (state.timer("ass_venom_empowered") > 0 && state.classMode == 0) ts.doseProgress += .35f * scale;
                flushDoseProgress(ts, 6);
                ts.statusTicks = 140;

                boolean toxicologist = specEnabled(stats, RPGSpecialization.TOXICOLOGIST);
                int amp = toxicologist && state.classMode == 1 ? 1 : 0;
                int poisonTicks = BossScaler.getTier(target) > 0 ? 40 : 75;
                if (toxicologist && specHas(stats, "_engine") && state.timer("ass_toxicology") > 0)
                    poisonTicks = Math.round(poisonTicks * 1.25f);
                if (state.timer("ass_venom_empowered") > 0 && state.classMode == 1 && BossScaler.getTier(target) <= 0)
                    amp = Math.max(amp, 1);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, poisonTicks, amp));
                if (state.timer("ass_venom_empowered") > 0 && state.classMode == 2)
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                            BossScaler.getTier(target) > 0 ? 20 : 45, 0));

                if (state.timer("ass_venom_consume") > 0 && ts.doses > 0) {
                    int spend = Math.min(ts.doses, state.timer("ass_venom_empowered") > 0 ? 4 : 3);
                    ts.doses -= spend;
                    int burstTicks = BossScaler.getTier(target) > 0 ? 28 + spend * 4 : 55 + spend * 12;
                    int burstAmp = BossScaler.getTier(target) > 0 ? 0 : (spend >= 3 ? 1 : 0);
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, burstTicks, burstAmp));
                    if (state.classMode == 2)
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                                BossScaler.getTier(target) > 0 ? 18 : 40 + spend * 5, 0));
                    state.setTimer("ass_venom_consume", 0);
                    state.setTimer("ass_venom_empowered", 0);
                }
                if (pathHas(stats, path, "_setup") && ts.doses >= 3) state.startTimer("ass_venom_ready", 80);
            }
            case ASS_DUELIST -> {
                if (projectile) return;
                float gain = state.timer("ass_duel_cadence") > 0 ? .75f : .3f;
                state.addGauge("ass_advantage", gain * gainScale, 5f);
                if (pathHas(stats, path, "_synergy")
                        && state.timer("class_synergy") > 0
                        && state.timer("ass_duel_synergy") > 0) {
                    state.addGauge("ass_advantage", .45f * gainScale, 5f);
                    state.setTimer("class_synergy", 0);
                    state.setTimer("ass_duel_synergy", 0);
                }
                state.setTimer("ass_duel_cadence", 50);
                if (pathHas(stats, path, "_setup") && state.gauge("ass_advantage") >= 2f) state.startTimer("ass_parry_ready", 80);
            }
            case ASS_SABOTEUR -> {
                state.addGauge("ass_preparation", .55f * gainScale, 5f);
                if (consumePathSynergy(state, "ass_sabo_synergy"))
                    state.addGauge("ass_preparation", .45f * gainScale, 5f);
                if (pathHas(stats, path, "_setup") && state.gauge("ass_preparation") >= 3f)
                    state.startTimer("ass_device_ready", 90);
                if (state.timer("ass_device_armed") > 0) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, BossScaler.getTier(target) > 0 ? 22 : 55, 0));
                    if (pathHas(stats, path, "_reaction"))
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 30, 0));
                    state.setTimer("ass_device_armed", 0);
                }
            }
            case ASS_MYSTIC -> {
                if (projectile) return;
                state.addGauge("ass_echo", .6f * gainScale, 6f);
                if (consumePathSynergy(state, "ass_myst_synergy"))
                    state.addGauge("ass_echo", .45f * gainScale, 6f);
                if (pathHas(stats, path, "_setup") && state.gauge("ass_echo") >= 3f) state.startTimer("ass_soul_ready", 90);
            }
            default -> { }
        }
    }

    private static void applySpecializationHit(ServerPlayerEntity player, LivingEntity target, PlayerStats stats,
                                               CombatState state, CombatState.ClassTargetState ts, float damage,
                                               boolean projectile, UUID previous, boolean duelCadenceWasActive) {
        RPGSpecialization spec = stats.specialization;
        if (!specEnabled(stats, spec)) return;
        float engine = specHas(stats, "_engine") ? 1.25f : 1f;

        switch (spec) {
            case BULWARK -> state.addGauge("war_guard", .25f * engine, GAUGE_CAP);
            case WARLORD -> { ts.markTicks = 100; state.addGauge("war_morale", .25f * engine, GAUGE_CAP); }
            case JUGGERNAUT -> state.addGauge("war_inertia", .35f * engine, 6f);
            case BLOOD_REAVER -> {
                ts.markTicks = 100;
                if (projectile) break;
                var weapon = WarriorSustain.meleeWeapon(player);
                if (damage > 0 && state.timer("internal_war_thirst_gain") <= 0) {
                    state.addGauge("war_thirst", WarriorHealingPolicy.thirstGain(damage,
                            WarriorSustain.weaponScale(player, weapon)) * engine, 5f);
                    state.startTimer("internal_war_thirst_gain", 8);
                }
                if (state.timer("war_blood_reaver") > 0)
                    WarriorSustain.heal(player, Math.min(1.2f, damage * .025f), weapon);
            }
            case RAGEBORN -> state.addGauge("war_fury_loop", .35f * engine, GAUGE_CAP);
            case PAIN_COLOSSUS -> { if (state.gauge("war_pain") > 0 && specHas(stats, "_conversion")) state.startTimer("war_pain_release", 60); }
            case BLADEMASTER -> {
                boolean continued = previous != null && previous.equals(target.getUuid())
                        && state.timer("war_blade_chain") > 0;
                if (!continued) state.setGauge("war_blade_cadence", 0f, 5f);
                state.addGauge("war_blade_cadence", (continued ? .75f : .45f) * engine, 5f);
                state.setTimer("war_blade_chain", 36);
            }
            case DUEL_MASTER -> { if (specHas(stats, "_engine")) state.startTimer("war_parry_ready", 55); }
            case TITAN_MAULER -> addImpact(target, ts, damage, (BossScaler.getTier(target) > 0 ? 1.2f : .7f) * engine, stats, state);
            case RUNE_KNIGHT -> state.addGauge("war_runes", .3f * engine, 5f);
            case SPELLBREAKER -> { if (state.timer("war_spell_seal") > 0) ts.markTicks = 80; }
            case STORMBLADE -> {
                if (state.timer("war_storm_moving") > 0)
                    state.addGauge("war_storm_charge", .6f * engine, 5f);
            }
            case BANNER_LORD -> {
                if (state.warBannerPlaced && player.squaredDistanceTo(
                        state.warBannerX, state.warBannerY, state.warBannerZ) <= 49d)
                    state.addGauge("war_morale", .4f * engine, GAUGE_CAP);
            }
            case IRON_GUARD -> state.addGauge("war_guard", .2f * engine, GAUGE_CAP);

            case SNIPER -> { if (projectile && state.gauge("arc_stationary") >= 2f) ts.markTicks = 100; }
            case DEADEYE -> { if (projectile) state.addGauge("arc_precision", previous != null && previous.equals(target.getUuid()) ? .7f * engine : .25f, 6f); }
            case BALLISTICIAN -> { if (projectile) ts.stacks = Math.min(6, ts.stacks + 1); }
            case BEASTMASTER -> { if (projectile) { ts.markTicks = 140; if (state.timer("arc_beast_order") > 0) buffPets(player, Math.round(80 * engine)); } }
            case TRAPPER -> { if (projectile && state.timer("arc_trap_ready") > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, Math.round((BossScaler.getTier(target) > 0 ? 25 : 60) * engine), 0)); }
            case SURVIVALIST -> { if (projectile) state.startTimer("arc_survival", Math.round(45 * engine)); }
            case WINDRUNNER -> { if (projectile) state.addGauge("arc_momentum", .45f * engine, GAUGE_CAP); }
            case ACROBAT -> { if (projectile && !player.isOnGround()) state.startTimer("arc_air_angle", 55); }
            case GUERRILLA -> { if (projectile && state.timer("arc_exposed_position") <= 0) state.startTimer("arc_ambush", Math.round(55 * engine)); state.setTimer("arc_exposed_position", 90); }
            case FLAMEBOW -> { if (projectile && state.classMode == 0) target.setOnFireFor(2); }
            case FROSTBOW -> { if (projectile && state.classMode == 1) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, BossScaler.getTier(target) > 0 ? 22 : 50, 0)); }
            case STORMBOW -> { if (projectile && state.classMode == 2) { ts.stacks = Math.min(5, ts.stacks + 1); if (ts.stacks >= 3 && specHas(stats, "_conversion")) target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 70, 0)); } }
            case CROSSBOW_EXPERT -> { if (projectile && state.timer("arc_crossbow_ready") > 0) state.addGauge("arc_device", .8f * engine, 5f); }
            case BOMBARDIER -> { if (projectile && state.timer("arc_bomb_shot") > 0) { applySlowAround(player.getServerWorld(), target, 3.5, 50); state.setTimer("arc_bomb_shot", 0); } }
            case ENGINEER -> { if (projectile && state.timer("arc_device_zone") > 0) state.addGauge("arc_device", .4f * engine, 5f); }

            case NIGHTBLADE -> { if (!projectile && ts.openingTicks > 0) state.startTimer("ass_escape", 45); }
            case PHANTOM -> { if (!projectile && state.timer("ass_phase_exit") > 0) state.startTimer("ass_escape", Math.round(45 * engine)); }
            case EXECUTIONER -> { if (!projectile && target.getHealth() / Math.max(1f, target.getMaxHealth()) < .3f) state.addGauge("ass_combo", .7f * engine, 5f); }
            case ALCHEMIST -> {
                if (!projectile && state.classMode == 0) {
                    ts.doseProgress += state.timer("ass_formula") > 0 ? engine : 1f;
                    flushDoseProgress(ts, 6);
                }
            }
            case PLAGUEBRINGER -> {
                if (!projectile && ts.doses >= 4 && specHas(stats, "_engine") && state.timer("ass_plague") > 0)
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, BossScaler.getTier(target) > 0 ? 20 : 45, 0));
                if (!projectile && ts.doses >= 4 && specHas(stats, "_conversion")) applyPoisonAround(player.getServerWorld(), target, 3.5, 45);
            }
            case TOXICOLOGIST -> {
                if (!projectile && state.classMode == 2) {
                    int slowTicks = state.timer("ass_toxicology") > 0 ? Math.round(45 * engine) : 45;
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, 0));
                }
            }
            case FENCER -> {
                if (!projectile && specHas(stats, "_engine") && !duelCadenceWasActive) {
                    float tempoScale = state.timer("ass_fencer_tempo") > 0 ? 1.5f : 1f;
                    state.addGauge("ass_advantage", .8f * engine * tempoScale, 5f);
                    if (state.timer("ass_fencer_tempo") > 0) state.setTimer("ass_fencer_tempo", 0);
                }
            }
            case BLADE_DANCER -> {
                if (!projectile) {
                    boolean switchedTarget = previous != null && !previous.equals(target.getUuid());
                    float windowScale = state.timer("ass_dance_window") > 0 ? 1.35f : 1f;
                    state.addGauge("ass_dance", (switchedTarget ? 1f : .2f) * engine * windowScale, 5f);
                }
            }
            case COUNTERBLADE -> { if (!projectile && state.timer("ass_riposte") > 0) state.addGauge("ass_advantage", 1f, 5f); }
            case DEMOLITIONIST -> { if (state.timer("ass_demolition") > 0) ts.stored = Math.min(6f, ts.stored + damage * .12f); }
            case INFILTRATOR -> { if (!projectile && state.timer("ass_opening") > 0) state.startTimer("ass_escape", Math.round(50 * engine)); }
            case WIREMASTER -> { if (state.timer("ass_device_armed") > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, BossScaler.getTier(target) > 0 ? 22 : 55, 0)); }
            case HEXKILLER -> { if (!projectile && state.timer("ass_hex_hunt") > 0) ts.markTicks = Math.round(80 * engine); }
            case SOULKNIFE -> { if (!projectile) state.addGauge("ass_echo", .55f * engine, 6f); }
            case VOIDWALKER -> { if (!projectile && state.timer("ass_void_debt") > 0) state.setTimer("ass_void_debt", 0); }

            default -> { }
        }
    }

    private static float warriorPathDamage(PlayerStats stats, CombatState state, LivingEntity target, CombatState.ClassTargetState ts) {
        float bonus = 0f;
        float bers = pathScale(stats, RPGPath.WAR_BERSERKER);
        if (bers > 0 && state.timer("war_bers_setup") > 0) bonus += .035f * bers;
        float weapon = pathScale(stats, RPGPath.WAR_WEAPONMASTER);
        if (weapon > 0 && state.timer("war_weapon_opening") > 0) bonus += .04f * weapon;
        float rune = pathScale(stats, RPGPath.WAR_RUNIC);
        if (rune > 0 && state.gauge("war_runes") >= 3f) bonus += .035f * rune;
        float cmd = pathScale(stats, RPGPath.WAR_COMMANDER);
        if (cmd > 0 && state.gauge("war_morale") >= 3f) bonus += .025f * cmd;
        if (ts.openingTicks > 0 && stats.path == RPGPath.WAR_VANGUARD && pathHas(stats, RPGPath.WAR_VANGUARD, "_synergy")) bonus += .025f;
        return bonus;
    }

    private static float archerPathDamage(PlayerStats stats, CombatState state, LivingEntity target,
                                          CombatState.ClassTargetState ts, double distance) {
        float bonus = 0f;
        float mark = pathScale(stats, RPGPath.ARC_MARKSMAN);
        if (mark > 0 && distance >= 9 && distance <= 28) bonus += Math.min(.08f, state.gauge("arc_aim") * .009f) * mark;
        float skirm = pathScale(stats, RPGPath.ARC_SKIRMISHER);
        if (skirm > 0) bonus += Math.min(.06f, state.gauge("arc_momentum") * .007f) * skirm;
        float arcane = pathScale(stats, RPGPath.ARC_ARCANE);
        if (arcane > 0 && state.timer("arc_elemental_reaction") > 0) bonus += .035f * arcane;
        float art = pathScale(stats, RPGPath.ARC_ARTIFICER);
        if (art > 0 && state.timer("arc_device_ready") > 0) bonus += .03f * art;
        float ward = pathScale(stats, RPGPath.ARC_WARDEN);
        if (ward > 0 && ts.markTicks > 0 && pathHas(stats, RPGPath.ARC_WARDEN, "_synergy")) bonus += .025f * ward;
        return bonus;
    }

    private static float assassinPathDamage(PlayerStats stats, CombatState state, LivingEntity target, CombatState.ClassTargetState ts) {
        float bonus = 0f;
        float effectGrowth=1f+com.rpgstats.balance.ClassBalance.levelProgress(stats.level);
        float venom = pathScale(stats, RPGPath.ASS_VENOM);
        if (venom > 0 && ts.doses >= 3) bonus += Math.min(.05f, ts.doses * .008f) * venom * effectGrowth;
        float duel = pathScale(stats, RPGPath.ASS_DUELIST);
        if (duel > 0) bonus += Math.min(.045f, state.gauge("ass_advantage") * .009f) * duel * effectGrowth;
        float myst = pathScale(stats, RPGPath.ASS_MYSTIC);
        if (myst > 0) bonus += Math.min(.045f, state.gauge("ass_echo") * .008f) * myst * effectGrowth;
        float sabo = pathScale(stats, RPGPath.ASS_SABOTEUR);
        if (sabo > 0 && state.timer("ass_device_armed") > 0) bonus += .025f * sabo * effectGrowth;
        return bonus;
    }

    private static float warriorSpecDamage(PlayerStats stats, CombatState state, LivingEntity target, CombatState.ClassTargetState ts) {
        RPGSpecialization spec = stats.specialization;
        if (!specEnabled(stats, spec)) return 0f;
        float b = switch (spec) {
            case BLOOD_REAVER -> ts.markTicks > 0 ? .035f : 0f;
            case RAGEBORN -> state.timer("war_frenzy") > 0 ? .05f : 0f;
            case PAIN_COLOSSUS -> Math.min(.055f, state.gauge("war_pain") * .007f);
            case BLADEMASTER -> Math.min(.05f, state.gauge("war_blade_cadence") * .010f);
            case DUEL_MASTER -> state.timer("war_riposte") > 0 ? .055f : 0f;
            case TITAN_MAULER -> BossScaler.getTier(target) > 0 ? .05f : (ts.openingTicks > 0 ? .035f : 0f);
            case RUNE_KNIGHT -> state.gauge("war_runes") >= 2 ? .035f : 0f;
            case SPELLBREAKER -> state.timer("war_spell_seal") > 0 ? .04f : 0f;
            case STORMBLADE -> Math.min(.045f, state.gauge("war_storm_charge") * .009f);
            case WARLORD -> ts.markTicks > 0 ? .035f : 0f;
            case JUGGERNAUT -> Math.min(.04f, state.gauge("war_inertia") * .008f);
            case BANNER_LORD, TACTICIAN, IRON_GUARD, BULWARK -> state.timer("war_formation") > 0 ? .025f : 0f;
            default -> 0f;
        };
        return specHas(stats, "_conversion") ? b * 1.15f : b;
    }

    private static float archerSpecDamage(ServerPlayerEntity player, PlayerStats stats, CombatState state,
                                          LivingEntity target, CombatState.ClassTargetState ts, double distance) {
        RPGSpecialization spec = stats.specialization;
        if (!specEnabled(stats, spec)) return 0f;
        float b = switch (spec) {
            case SNIPER -> state.gauge("arc_stationary") >= 2 && distance >= 14 ? .065f : 0f;
            case DEADEYE -> Math.min(.06f, state.gauge("arc_precision") * .010f);
            case BALLISTICIAN -> ts.stacks >= 2 ? .035f : 0f;
            case BEASTMASTER -> ts.markTicks > 0 ? .025f : 0f;
            case TRAPPER -> target.hasStatusEffect(StatusEffects.SLOWNESS) ? .045f : 0f;
            case SURVIVALIST -> player.getHealth() < player.getMaxHealth() * .55f ? .025f : 0f;
            case WINDRUNNER -> Math.min(.05f, state.gauge("arc_momentum") * .006f);
            case ACROBAT -> !player.isOnGround() || state.timer("arc_air_angle") > 0 ? .055f : 0f;
            case GUERRILLA -> state.timer("arc_ambush") > 0 ? .055f : 0f;
            case FLAMEBOW -> target.isOnFire() ? .035f : 0f;
            case FROSTBOW -> target.hasStatusEffect(StatusEffects.SLOWNESS) ? .035f : 0f;
            case STORMBOW -> ts.stacks >= 3 ? .04f : 0f;
            case CROSSBOW_EXPERT -> state.timer("arc_crossbow_ready") > 0 ? .055f : 0f;
            case BOMBARDIER -> state.timer("arc_bomb_shot") > 0 ? .04f : 0f;
            case ENGINEER -> state.timer("arc_device_zone") > 0 ? .035f : 0f;
            default -> 0f;
        };
        return specHas(stats, "_conversion") ? b * 1.15f : b;
    }

    private static float assassinSpecDamage(PlayerStats stats, CombatState state, LivingEntity target, CombatState.ClassTargetState ts) {
        RPGSpecialization spec = stats.specialization;
        if (!specEnabled(stats, spec)) return 0f;
        float threshold = BossScaler.getTier(target) > 0 ? .18f : .30f;
        float b = switch (spec) {
            case NIGHTBLADE -> ts.openingTicks > 0 ? .065f : 0f;
            case PHANTOM -> state.timer("ass_phase_exit") > 0 ? .05f : 0f;
            case EXECUTIONER -> target.getHealth() / Math.max(1f, target.getMaxHealth()) <= threshold ? .10f : 0f;
            case ALCHEMIST -> ts.doses >= 3 ? .035f : 0f;
            case PLAGUEBRINGER -> ts.doses >= 4 ? .045f : 0f;
            case TOXICOLOGIST -> ts.doses >= 2 ? .035f : 0f;
            case FENCER -> {
                float fencer = state.gauge("ass_advantage") >= 2 ? .04f : 0f;
                if (state.timer("ass_fencer_tempo") > 0 && state.timer("ass_duel_cadence") == 0) fencer += .05f;
                yield Math.min(.08f, fencer);
            }
            case BLADE_DANCER -> Math.min(.05f, state.gauge("ass_dance") * .010f);
            case COUNTERBLADE -> state.timer("ass_riposte") > 0 ? .06f : 0f;
            case DEMOLITIONIST -> state.timer("ass_demolition") > 0 ? .04f : 0f;
            case INFILTRATOR -> state.timer("ass_opening") > 0 ? .05f : 0f;
            case WIREMASTER -> target.hasStatusEffect(StatusEffects.SLOWNESS) ? .035f : 0f;
            case HEXKILLER -> state.timer("ass_hex_hunt") > 0 ? .04f
                    : (specHas(stats, "_engine") && ts.markTicks > 0 ? .02f : 0f);
            case SOULKNIFE -> Math.min(.05f, state.gauge("ass_echo") * .008f);
            case VOIDWALKER -> state.timer("ass_void_debt") > 0 ? .065f : 0f;
            default -> 0f;
        };
        return specHas(stats, "_conversion") ? b * 1.15f : b;
    }

    private static void activateCore(ServerPlayerEntity player, PlayerStats stats, float scale) {
        CombatState state = CombatState.get(player.getUuid());
        switch (stats.clazz) {
            case GUERREIRO -> { state.startTimer("war_guard_stance", Math.round(65 * scale)); state.addGauge("war_guard", 1.5f * scale, GAUGE_CAP); message(player, "Contra-pressao", "guarda preparada"); }
            case ARQUEIRO -> { state.startTimer("arc_prepared_shot", Math.round(95 * scale)); state.addGauge("arc_focus", 1.2f * scale, GAUGE_CAP); message(player, "Tiro Preparado", "o proximo disparo recompensa posicionamento"); }
            case ASSASSINO -> { state.startTimer("ass_opening", Math.round(65 * scale)); state.startTimer("ass_escape", Math.round(45 * scale)); player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Math.round(45 * scale), 0)); message(player, "Reposicionamento", "abertura curta preparada"); }
            default -> { }
        }
    }

    private static void activatePath(ServerPlayerEntity player, PlayerStats stats, RPGPath path, String nodeId, float scale) {
        CombatState state = CombatState.get(player.getUuid());
        boolean signature = nodeId.endsWith("_signature");
        int ticks = Math.round((signature ? 135 : 85) * scale);
        boolean reaction = pathHas(stats, path, "_reaction");
        boolean synergy = pathHas(stats, path, "_synergy");

        switch (path) {
            case WAR_VANGUARD -> {
                float spent = takeGauge(state, "war_guard", signature ? 5f : 2f);
                state.startTimer("war_guard_stance", ticks + Math.round(spent * 7));
                if (reaction) state.startTimer("war_parry", Math.min(35, ticks));
                if (signature) player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, ticks, spent >= 4 ? 1 : 0));
                message(player, signature ? "Linha Inabalavel" : "Guarda de Vanguarda", "Guarda gasta " + one(spent));
            }
            case WAR_BERSERKER -> {
                float fury = Math.min(stats.resource, signature ? 16f : 9f);
                stats.resource = Math.max(0f, stats.resource - fury);
                state.startTimer("war_frenzy", ticks + Math.round(fury * 2));
                if (reaction && player.getHealth() < player.getMaxHealth() * .4f) player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 45, 0));
                if (signature) state.startTimer("war_exhaustion", 75);
                message(player, signature ? "Ruptura de Furia" : "Frenesi", "Furia gasta " + one(fury));
            }
            case WAR_WEAPONMASTER -> {
                state.startTimer(signature ? "war_riposte" : "war_weapon_opening", ticks);
                state.addGauge("war_weapon_flow", signature ? 1.8f : .8f, 5f);
                if (reaction) state.startTimer("war_parry", 30);
                message(player, signature ? "Dominio de Arma" : "Abertura Tecnica", "ritmo de arma preparado");
            }
            case WAR_RUNIC -> {
                if (!signature) state.classMode = (state.classMode + 1) % 3;
                float runes = takeGauge(state, "war_runes", signature ? 4f : 2f);
                state.startTimer("war_runic_strike", ticks + Math.round(runes * 9));
                if (reaction && state.classMode == 1) state.startTimer("war_guard_stance", 45);
                message(player, "Descarga Runica", "modo " + (state.classMode + 1) + " · " + one(runes) + " cargas");
            }
            case WAR_COMMANDER -> {
                if (!signature) state.classMode = (state.classMode + 1) % 3;
                float morale = takeGauge(state, "war_morale", signature ? 5f : 2f);
                String order = state.classMode == 0 ? "war_order_offense"
                        : state.classMode == 1 ? "war_order_defense" : "war_order_advance";
                state.startTimer(order, ticks + Math.round(morale * 7));
                state.setGauge("war_tactic_progress", 0f, 3f);
                if (state.classMode == 2)
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Math.min(90, ticks), 0));
                if (signature) state.startTimer("war_formation", ticks);
                message(player, "Ordem", state.classMode == 0 ? "ofensiva"
                        : state.classMode == 1 ? "defensiva" : "avanco");
            }
            case ARC_MARKSMAN -> { state.startTimer("arc_prepared_shot", ticks); if (signature) state.addGauge("arc_aim", 1.8f, GAUGE_CAP); if (reaction) state.startTimer("arc_reposition_guard", 45); message(player, "Mira Preparada", "Mira " + one(state.gauge("arc_aim"))); }
            case ARC_WARDEN -> { state.startTimer("arc_hunt", ticks); state.addGauge("arc_instinct", signature ? 1.8f : .8f, GAUGE_CAP); if (reaction) state.startTimer("arc_survival", 55); message(player, "Cacada", "Instinto " + one(state.gauge("arc_instinct"))); }
            case ARC_SKIRMISHER -> { float m = takeGauge(state, "arc_momentum", signature ? 5f : 2.5f); state.startTimer("arc_ambush", ticks + Math.round(m * 7)); player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ticks, signature ? 1 : 0)); if (reaction) state.startTimer("arc_reposition_guard", 40); message(player, "Fluxo de Movimento", "Momentum gasto " + one(m)); }
            case ARC_ARCANE -> {
                if (!signature) state.classMode = (state.classMode + 1) % 3;
                else state.startTimer("arc_elemental_reaction", ticks);
                if (reaction) state.startTimer("arc_reposition_guard", 35);
                message(player, "Afinidade de Flecha", state.classMode == 0 ? "Fogo" : state.classMode == 1 ? "Gelo" : "Tempestade");
            }
            case ARC_ARTIFICER -> { state.addGauge("arc_device", signature ? 1.8f : .8f, 5f); state.startTimer(signature ? "arc_device_zone" : "arc_device_ready", ticks); if (reaction) state.startTimer("arc_reposition_guard", 35); message(player, "Dispositivo", "cargas " + one(state.gauge("arc_device"))); }
            case ASS_SHADOW -> {
                state.startTimer("ass_opening", ticks);
                state.startTimer("ass_escape", ticks / 2);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Math.min(45, ticks / 2), 0));
                if (reaction) player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Math.min(55, ticks / 2), 0));
                message(player, signature ? "Entrada Sombria" : "Sombra Preparada", "primeiro golpe consome abertura");
            }
            case ASS_VENOM -> {
                boolean prepared = state.timer("ass_venom_ready") > 0;
                if (prepared) {
                    state.setTimer("ass_venom_ready", 0);
                    state.startTimer("ass_venom_empowered", ticks + 20);
                }
                if (!signature) state.classMode = (state.classMode + 1) % 3;
                else state.startTimer("ass_venom_consume", ticks + (prepared ? 20 : 0));
                if (reaction) state.startTimer("ass_escape", 30);
                message(player, "Formula Toxica", (state.classMode == 0 ? "Desgaste" : state.classMode == 1 ? "Potencia" : "Controle")
                        + (prepared ? " · Veneno Pronto consumido" : ""));
            }
            case ASS_DUELIST -> {
                boolean prepared = state.timer("ass_parry_ready") > 0;
                if (prepared) state.setTimer("ass_parry_ready", 0);
                int duelTicks = ticks + (prepared ? 18 : 0);
                state.startTimer(signature ? "ass_riposte" : "ass_parry", duelTicks);
                state.addGauge("ass_advantage", (signature ? 1.3f : .5f) + (prepared ? .35f : 0f), 5f);
                message(player, signature ? "Riposta Armada" : "Janela de Parry",
                        "Vantagem " + one(state.gauge("ass_advantage")) + (prepared ? " · Preparacao consumida" : ""));
            }
            case ASS_SABOTEUR -> {
                boolean prepared = state.timer("ass_device_ready") > 0;
                if (prepared) state.setTimer("ass_device_ready", 0);
                int deviceTicks = ticks + (prepared ? 20 : 0);
                state.startTimer("ass_device_armed", deviceTicks);
                state.addGauge("ass_preparation", signature ? 1.8f : .8f, 5f);
                if (signature) state.startTimer("ass_demolition", deviceTicks);
                message(player, "Dispositivo Armado", "Preparacao " + one(state.gauge("ass_preparation"))
                        + (prepared ? " · Preparacao consumida" : ""));
            }
            case ASS_MYSTIC -> {
                boolean prepared = state.timer("ass_soul_ready") > 0;
                if (prepared) state.setTimer("ass_soul_ready", 0);
                float echoCap = Math.max(0f, (signature ? 4f : 2f) - (prepared ? 1f : 0f));
                float echoes = takeGauge(state, "ass_echo", echoCap);
                state.startTimer("ass_soul_strike", ticks + Math.round(echoes * 9) + (prepared ? 18 : 0));
                if (reaction) state.startTimer("ass_escape", 35);
                message(player, "Eco de Alma", "ecos gastos " + one(echoes)
                        + (prepared ? " · Alma Pronta consumida" : ""));
            }
            default -> { }
        }
        if (synergy) {
            state.startTimer("class_synergy", 80);
            String timer = pathSynergyTimer(path);
            if (!timer.isBlank()) state.startTimer(timer, 80);
        }
    }

    private static void activateSpecialization(ServerPlayerEntity player, PlayerStats stats, RPGSpecialization spec,
                                               String nodeId, float scale) {
        CombatState state = CombatState.get(player.getUuid());
        boolean signature = nodeId.endsWith("_signature");
        boolean ascension = nodeId.endsWith("_ascension");
        int ticks = Math.round((ascension ? 175 : signature ? 125 : 75) * scale);
        state.startTimer("spec_" + spec.name().toLowerCase(Locale.ROOT) + (ascension ? "_asc" : signature ? "_sig" : "_tech"), ticks);

        switch (spec) {
            case BULWARK -> { state.startTimer("war_bulwark_barrier", ticks); state.startTimer("war_guard_stance", ticks); }
            case WARLORD -> { state.addGauge("war_morale", signature ? 2.5f : 1.2f, GAUGE_CAP); state.startTimer("war_warlord_mark", ticks); }
            case JUGGERNAUT -> { float inertia = takeGauge(state, "war_inertia", signature ? 6f : 3f); state.startTimer("war_juggernaut", ticks + Math.round(inertia * 8)); player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Math.min(80, ticks), 0)); }
            case BLOOD_REAVER -> { state.startTimer("war_blood_reaver", ticks); state.addGauge("war_thirst", signature ? 1.5f : .7f, 5f); }
            case RAGEBORN -> { float fury = takeGauge(state, "war_fury_loop", signature || ascension ? 10f : 4f); state.startTimer("war_frenzy", ticks + Math.round(fury * 5)); if (ascension) state.startTimer("war_frenzy_asc_pending", ticks + Math.round(fury * 5) + 2); }
            case PAIN_COLOSSUS -> state.startTimer("war_pain_release", ticks);
            case BLADEMASTER -> { state.addGauge("war_blade_cadence", 1.7f, 5f); state.startTimer("war_blade_mastery", ticks); }
            case DUEL_MASTER -> { state.startTimer("war_parry", Math.min(45, ticks)); state.startTimer("war_riposte_ready", ticks); }
            case TITAN_MAULER -> state.startTimer("war_titan_impact", ticks);
            case RUNE_KNIGHT -> { state.addGauge("war_runes", signature ? 2.5f : 1.2f, 5f); state.startTimer("war_runic_strike", ticks); }
            case SPELLBREAKER -> state.startTimer("war_spell_seal", ticks);
            case STORMBLADE -> { state.addGauge("war_storm_charge", 1.7f, 5f); state.startTimer("war_stormblade", ticks); }
            case BANNER_LORD -> { state.startTimer("war_banner", ticks); state.startTimer("war_formation", ticks); state.warBannerX = player.getX(); state.warBannerY = player.getY(); state.warBannerZ = player.getZ(); state.warBannerPlaced = true; }
            case TACTICIAN -> { state.classMode = (state.classMode + 1) % 3; String order = state.classMode == 0 ? "war_order_offense" : state.classMode == 1 ? "war_order_defense" : "war_order_advance"; state.startTimer(order, ticks); state.setGauge("war_tactic_progress", 0f, 3f); }
            case IRON_GUARD -> { state.startTimer("war_iron_guard", ticks); state.startTimer("war_guard_stance", ticks / 2); }

            case SNIPER -> { state.startTimer("arc_prepared_shot", ticks); state.addGauge("arc_stationary", 1.7f, 5f); }
            case DEADEYE -> state.addGauge("arc_precision", signature ? 1.7f : .8f, 6f);
            case BALLISTICIAN -> state.startTimer("arc_ballistic", ticks);
            case BEASTMASTER -> { state.startTimer("arc_beast_order", ticks); buffPets(player, ticks); }
            case TRAPPER -> state.startTimer("arc_trap_ready", ticks);
            case SURVIVALIST -> state.startTimer("arc_survival", ticks);
            case WINDRUNNER -> { state.addGauge("arc_momentum", 1.7f, GAUGE_CAP); player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ticks, 0)); }
            case ACROBAT -> { state.startTimer("arc_air_angle", ticks); player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, ticks, 0)); }
            case GUERRILLA -> { state.startTimer("arc_ambush", ticks); player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ticks / 2, 0)); }
            case FLAMEBOW -> { state.classMode = 0; state.startTimer("arc_elemental_reaction", ticks); }
            case FROSTBOW -> { state.classMode = 1; state.startTimer("arc_elemental_reaction", ticks); }
            case STORMBOW -> { state.classMode = 2; state.startTimer("arc_elemental_reaction", ticks); }
            case CROSSBOW_EXPERT -> state.startTimer("arc_crossbow_ready", ticks);
            case BOMBARDIER -> state.startTimer("arc_bomb_shot", ticks);
            case ENGINEER -> state.startTimer("arc_device_zone", ticks);

            case NIGHTBLADE -> { state.startTimer("ass_opening", ticks); if (ascension) player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Math.min(55, ticks), 0)); }
            case PHANTOM -> { state.startTimer("ass_phase", Math.min(45, ticks)); state.startTimer("ass_phase_exit", ticks); }
            case EXECUTIONER -> state.startTimer("ass_execute_window", ticks);
            case ALCHEMIST -> { state.classMode = (state.classMode + 1) % 3; state.startTimer("ass_formula", ticks); }
            case PLAGUEBRINGER -> state.startTimer("ass_plague", ticks);
            case TOXICOLOGIST -> { state.classMode = (state.classMode + 1) % 3; state.startTimer("ass_toxicology", ticks); }
            case FENCER -> {
                state.setTimer("ass_duel_cadence", 0);
                state.startTimer("ass_fencer_tempo", ticks);
            }
            case BLADE_DANCER -> {
                state.addGauge("ass_dance", 1.7f, 5f);
                state.startTimer("ass_dance_window", ticks);
            }
            case COUNTERBLADE -> state.startTimer("ass_parry", Math.min(45, ticks));
            case DEMOLITIONIST -> { state.startTimer("ass_device_armed", ticks); state.startTimer("ass_demolition", ticks); }
            case INFILTRATOR -> { state.startTimer("ass_opening", ticks); state.startTimer("ass_escape", ticks / 2); }
            case WIREMASTER -> state.startTimer("ass_device_armed", ticks);
            case HEXKILLER -> state.startTimer("ass_hex_hunt", ticks);
            case SOULKNIFE -> { state.addGauge("ass_echo", 1.7f, 6f); state.startTimer("ass_soul_strike", ticks); }
            case VOIDWALKER -> { shortBlink(player, 2.4 * scale); state.startTimer("ass_void_debt", ticks); }

            default -> { }
        }
        message(player, ascension ? "ASCENSAO" : signature ? "Dominio" : "Tecnica", spec.display);
    }

    private static void addImpact(LivingEntity target, CombatState.ClassTargetState ts, float damage, float scale,
                                  PlayerStats stats, CombatState state) {
        float gain = (.65f + Math.min(1.5f, damage * .10f)) * scale;
        if (specEnabled(stats, RPGSpecialization.TITAN_MAULER)) gain *= BossScaler.getTier(target) > 0 ? 1.35f : 1.15f;
        ts.impact += gain;
        state.addGauge("war_impact", gain * .4f, GAUGE_CAP);
        float threshold = BossScaler.getTier(target) > 0 ? 12f : 7f;
        if (ts.impact >= threshold) {
            ts.impact = 0f;
            ts.openingTicks = BossScaler.getTier(target) > 0 ? 32 : 68;
            state.startTimer("war_opening", ts.openingTicks);
        }
    }

    private static float pathScale(PlayerStats stats, RPGPath path) {
        if (stats == null || path == null || path.parent != stats.clazz) return 0f;
        String foundation = pathPrefix(path) + "_foundation";
        if (stats.path == path && stats.unlockedNodes.contains(foundation)) return 1f;
        if (stats.affinityHouse == path && stats.unlockedNodes.contains(HouseRules.PREFIX + foundation)) return .65f;
        return 0f;
    }

    private static boolean pathHas(PlayerStats stats, RPGPath path, String suffix) {
        if (stats == null || path == null) return false;
        String id = pathPrefix(path) + suffix;
        if (stats.path == path && stats.unlockedNodes.contains(id)) return true;
        return stats.affinityHouse == path && stats.unlockedNodes.contains(HouseRules.PREFIX + id);
    }

    private static String pathPrefix(RPGPath path) {
        return path.nodes.get(0).id().replace("_foundation", "");
    }

    private static boolean specEnabled(PlayerStats stats, RPGSpecialization spec) {
        return spec != null && stats.specialization == spec && specHas(stats, "_initiation");
    }

    private static boolean specHas(PlayerStats stats, String suffix) {
        RPGSpecialization spec = stats == null ? null : stats.specialization;
        if (spec == null) return false;
        String prefix = spec.nodes.get(0).id().replace("_initiation", "");
        return stats.unlockedNodes.contains(prefix + suffix);
    }

    private static boolean coreHas(PlayerStats stats, String suffix) {
        if (stats == null || stats.clazz == null || stats.clazz == RPGClass.MAGO) return false;
        String prefix = switch (stats.clazz) {
            case GUERREIRO -> "war";
            case ARQUEIRO -> "arc";
            case ASSASSINO -> "ass";
            case MAGO -> "mag";
        };
        return stats.unlockedNodes.contains(prefix + "_core_" + suffix);
    }

    private static void addPathGauge(PlayerStats stats, CombatState state, RPGPath path, String key, float amount) {
        float scale = pathScale(stats, path);
        if (scale > 0) state.addGauge(key, amount * scale, GAUGE_CAP);
    }

    private static float takeGauge(CombatState state, String key, float max) {
        float take = Math.min(Math.max(0f, max), state.gauge(key));
        state.setGauge(key, state.gauge(key) - take, GAUGE_CAP);
        return take;
    }

    private static boolean consumePathSynergy(CombatState state, String key) {
        if (state.timer(key) <= 0) return false;
        state.setTimer(key, 0);
        state.setTimer("class_synergy", 0);
        return true;
    }

    private static String pathSynergyTimer(RPGPath path) {
        return switch (path) {
            case ARC_MARKSMAN -> "arc_mark_synergy";
            case ARC_WARDEN -> "arc_ward_synergy";
            case ARC_SKIRMISHER -> "arc_skirm_synergy";
            case ARC_ARCANE -> "arc_magic_synergy";
            case ARC_ARTIFICER -> "arc_art_synergy";
            case ASS_SHADOW -> "ass_shadow_synergy";
            case ASS_VENOM -> "ass_venom_synergy";
            case ASS_DUELIST -> "ass_duel_synergy";
            case ASS_SABOTEUR -> "ass_sabo_synergy";
            case ASS_MYSTIC -> "ass_myst_synergy";
            default -> "";
        };
    }

    private static void flushStackProgress(CombatState.ClassTargetState ts, int cap) {
        int gained = Math.min(Math.max(0, cap - ts.stacks), (int) ts.stackProgress);
        if (gained <= 0) return;
        ts.stackProgress -= gained;
        ts.stacks = Math.min(cap, ts.stacks + gained);
    }

    private static void flushDoseProgress(CombatState.ClassTargetState ts, int cap) {
        int gained = Math.min(Math.max(0, cap - ts.doses), (int) ts.doseProgress);
        if (gained <= 0) return;
        ts.doseProgress -= gained;
        ts.doses = Math.min(cap, ts.doses + gained);
    }

    private static void applyArcaneArrowState(LivingEntity target, int mode, int stacks) {
        if (mode == 0) target.setOnFireFor(Math.min(4, 1 + stacks / 2));
        else if (mode == 1) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 35 + stacks * 5, 0));
        else if (stacks >= 3) target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0));
    }

    private static void applySlowAround(ServerWorld world, LivingEntity center, double radius, int ticks) {
        Box box = center.getBoundingBox().expand(radius);
        for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != center))
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, BossScaler.getTier(living) > 0 ? ticks / 2 : ticks, 0));
    }

    private static void applyPoisonAround(ServerWorld world, LivingEntity center, double radius, int ticks) {
        Box box = center.getBoundingBox().expand(radius);
        for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != center))
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, BossScaler.getTier(living) > 0 ? ticks / 2 : ticks, 0));
    }

    private static void buffPets(ServerPlayerEntity player, int ticks) {
        Box box = player.getBoundingBox().expand(12);
        for (TameableEntity pet : player.getServerWorld().getEntitiesByClass(TameableEntity.class, box,
                p -> p.isAlive() && p.getOwner() == player)) {
            pet.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, ticks, 0));
            pet.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ticks, 0));
        }
    }

    private static boolean isDirectMeleeDamage(DamageSource source) {
        if (source == null) return false;
        if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_RESISTANCE)
                || source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)
                || source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION)
                || source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FIRE)
                || source.isOf(net.minecraft.entity.damage.DamageTypes.THORNS)
                || source.isOf(net.minecraft.entity.damage.DamageTypes.SONIC_BOOM)
                || source.isOf(net.minecraft.entity.damage.DamageTypes.MAGIC)
                || source.isOf(net.minecraft.entity.damage.DamageTypes.INDIRECT_MAGIC)
                || com.rpgstats.compat.CompatManager.isIronsSpellDamage(source)) return false;
        Entity attacker = source.getAttacker();
        Entity direct = source.getSource();
        return attacker instanceof LivingEntity && direct == attacker;
    }

    private static boolean isBehind(ServerPlayerEntity player, LivingEntity target) {
        Vec3d delta = player.getPos().subtract(target.getPos());
        if (delta.lengthSquared() < .001) return false;
        return target.getRotationVec(1f).dotProduct(delta.normalize()) < -.45;
    }

    private static boolean isUndead(LivingEntity target) {
        String id = target.getType().toString().toLowerCase(Locale.ROOT);
        return id.contains("zombie") || id.contains("skeleton") || id.contains("wither") || id.contains("phantom");
    }

    private static void healNearby(ServerPlayerEntity player, double radius, float amount, boolean absorption) {
        if (amount <= 0f) return;
        Box box = player.getBoundingBox().expand(radius);
        for (ServerPlayerEntity ally : player.getServerWorld().getEntitiesByClass(ServerPlayerEntity.class, box,
                p -> p.isAlive() && (p == player || player.getScoreboardTeam() == null || player.isTeammate(p)))) {
            ally.heal(amount);
            if (absorption) ally.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 80, 0));
        }
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.PLAYERS, .6f, 1.2f);
    }

    private static void cleanse(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.POISON);
        player.removeStatusEffect(StatusEffects.WITHER);
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.WEAKNESS);
    }

    private static void shortBlink(ServerPlayerEntity player, double distance) {
        Vec3d look = player.getRotationVec(1f).normalize().multiply(distance);
        player.addVelocity(look.x, .08, look.z);
        player.velocityModified = true;
    }

    private static void message(ServerPlayerEntity player, String title, String detail) {
        player.sendMessage(Text.literal("§e" + title + " §7· §f" + detail), true);
    }

    private static String one(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
