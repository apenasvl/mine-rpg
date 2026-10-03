package com.rpgstats.combat;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.ability.AbilityType;
import com.rpgstats.ability.ClassAbilityRegistry;
import com.rpgstats.ability.SkillEffect;
import com.rpgstats.balance.GlobalCaps;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.compat.CompatManager;
import com.rpgstats.integration.IntegrationServices;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.Stat;
import com.rpgstats.stats.StatsManager;
import com.rpgstats.tree.SkillNode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Passivas, ativas, recursos e pipeline de dano do RPG. */
public final class CombatHandler {

    public static float modifyOutgoingDamage(ServerPlayerEntity player, Entity target, float amount, DamageSource source) {
        if (source.isOf(DamageTypes.THORNS)) return amount;

        PlayerStats stats = StatsManager.get(player);
        float weaponAffinity=com.rpgstats.compat.bosses.WeaponAffinity.damageFactor(stats,
                ArcherShotTracker.launchWeapon(source,player));
        if(ArcherShotTracker.isNativeSecondary(source))return amount*weaponAffinity;
        List<SkillEffect> effects = AbilityRegistry.allFor(stats.unlockedNodes);
        CombatState state = CombatState.get(player.getUuid());
        boolean projectile = isProjectile(source);
        boolean magic = isMagic(source) && !ArcherShotTracker.isBowShot(source);

        float staminaMult = SoulslikeCombat.physicalAttackMultiplier(player, stats, projectile, magic);
        float attributeMult = projectile && !magic ? SoulslikeCombat.rangedAttributeMultiplier(stats)
                : magic ? SoulslikeCombat.magicAttributeMultiplier(stats) : SoulslikeCombat.meleeAttributeMultiplier(stats);
        float mult = 1f;
        float magicPower = 0f;
        for (SkillEffect e : effects) {
            if (e.type() != AbilityType.PASSIVE) continue;
            switch (e.effectId()) {
                case "melee_damage" -> { if (!projectile && !magic) mult += e.value(); }
                case "ranged_damage" -> { if (projectile && !magic) mult += e.value(); }
                case "magic_power" -> { if (magic) magicPower += e.value(); }
                default -> { }
            }
        }
        if (magic) mult += Math.min(0.60f, Math.max(0f, magicPower));

        // Compatibilidade dos nodes legados. As arvores expandidas nao usam mais estes estados como motor.
        if (state.powerTicks > 0) mult += state.powerBonus;
        if (state.bloodlustTicks > 0 && !projectile && !magic) mult += state.bloodlustBonus;
        if (state.smiteTicks > 0 && !projectile && !magic) {
            mult *= Math.max(1f, state.smiteMultiplier);
            state.consumeSmite();
        }

        if (!magic) {
            float crit = AbilityRegistry.sumPassive(stats.unlockedNodes, "crit_chance");
            if (crit > 0 && player.getRandom().nextFloat() < GlobalCaps.crit(crit)) {
                mult *= 1.5f;
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.8f, 1.2f);
            }
        }

        // Apenas saves/nodes antigos ainda podem produzir esses efeitos genericos.
        if (state.rotationTicks > 0 && !magic)
            mult += Math.min(.12f, AbilityRegistry.sumPassive(stats.unlockedNodes, "rotation_damage"));
        if (player.getHealth() < player.getMaxHealth() * .4f)
            mult += Math.min(.12f, AbilityRegistry.sumPassive(stats.unlockedNodes, "low_health_damage"));

        if (!magic) mult = Math.min(1.65f, mult);
        float result = amount * Math.max(0f, mult) * staminaMult * attributeMult;

        if (magic && target instanceof LivingEntity living && stats.clazz == RPGClass.MAGO) {
            result = MageCombatHandler.modifyMagicDamage(player, living, result);
        } else if (stats.clazz == RPGClass.MAGO) {
            result = MageCombatHandler.modifyPhysicalDamage(player, result, projectile, magic);
        }

        if (!magic) {
            result *= IntegrationServices.INSTANCE.weapon(ArcherShotTracker.launchWeapon(source, player))
                    .map(profile -> IntegrationServices.INSTANCE.weaponMultiplier(stats, profile)).orElse(1f);
            if(ArcherShotTracker.isBowShot(source))
                result *= com.rpgstats.integration.DataDrivenRegistry.equipmentRules(ArcherShotTracker.launchWeapon(source,player))
                        .map(rules->rules.damageFactor()).orElse(1f);
        }

        if (ClassMechanics.handles(stats))
            result = ClassMechanics.modifyOutgoingDamage(player, target, stats, result, source, projectile, magic);

        result *= 1f - HouseRules.offensePenalty(stats);
        float capped=amount <= 0f ? result : amount * (magic || stats.clazz == null || stats.clazz == RPGClass.MAGO
                ? GlobalCaps.damageMultiplier(result / amount)
                : GlobalCaps.physicalDamageMultiplier(result / amount, stats.level));
        return magic ? capped : capped*weaponAffinity;
    }

    public static float modifyIncomingDamage(ServerPlayerEntity player, float amount, DamageSource source) {
        amount = SoulslikeCombat.modifyIncomingDamage(player, amount, source);
        if (amount <= 0f) return 0f;
        Entity attacker = source.getAttacker();
        if (attacker instanceof LivingEntity living && !(attacker instanceof PlayerEntity))
            amount *= BossScaler.getDamageMultiplier(living);

        amount = MageCombatHandler.modifyIncomingDamage(player, amount, source);
        if (amount <= 0f) return 0f;
        if (source.isIn(DamageTypeTags.BYPASSES_RESISTANCE)) return amount;

        float beforeClassGuards=amount;
        PlayerStats stats = StatsManager.get(player);
        if (ClassMechanics.handles(stats)) amount = ClassMechanics.modifyIncomingDamage(player, stats, amount, source);

        CombatState state = CombatState.get(player.getUuid());
        float reduction = AbilityRegistry.sumPassive(stats.unlockedNodes, "damage_reduction");
        if (state.guardTicks > 0) reduction += state.guardReduction;
        if (state.castGuardTicks > 0)
            reduction += Math.min(.10f, AbilityRegistry.sumPassive(stats.unlockedNodes, "cast_guard"));
        float resolve = AbilityRegistry.sumPassive(stats.unlockedNodes, "low_health_guard");
        if (resolve > 0 && player.getHealth() < player.getMaxHealth() * .4f && state.resolveCooldown <= 0) {
            state.resolveTicks = 60;
            state.resolveCooldown = 400;
        }
        if (state.resolveTicks > 0) reduction += Math.min(.15f, resolve);
        if (player.getHealth() < player.getMaxHealth() * .4f)
            amount *= 1f + AbilityRegistry.sumPassive(stats.unlockedNodes, "risk_vulnerability");
        if (state.smokeTicks > 0) reduction += 0.25f;
        reduction = GlobalCaps.damageReduction(reduction);
        float guarded=amount*(1f-reduction);
        if(stats.clazz==RPGClass.GUERREIRO && attacker instanceof LivingEntity boss
                && !(boss instanceof PlayerEntity) && BossScaler.getTier(boss)>0
                && player.squaredDistanceTo(boss)<=64d) {
            guarded=com.rpgstats.balance.ClassBalance.warriorBossDamage(beforeClassGuards,guarded,
                    com.rpgstats.balance.ClassBalance.warriorBossReduction(stats.level,stats.totalStats().getOrDefault(Stat.TENACIDADE,0)));
        }
        return guarded*(1f+HouseRules.vulnerability(stats));
    }

    public static void onPlayerHitEntity(ServerPlayerEntity player, LivingEntity target, float damageDealt, DamageSource source) {
        if (source.isOf(DamageTypes.THORNS) || damageDealt <= 0 || ArcherShotTracker.isNativeSecondary(source)) return;
        if (!ArcherShotTracker.claim(player, source)) return;

        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz == RPGClass.ARQUEIRO && isProjectile(source) && !ArcherShotTracker.isBowShot(source)) return;
        boolean projectile = isProjectile(source);
        boolean magic = isMagic(source) && !ArcherShotTracker.isBowShot(source);
        boolean resourceChanged = false;

        if (stats.clazz == RPGClass.ARQUEIRO && ArcherShotTracker.isBowShot(source)) {
            ArcherTechniqueHandler.onHit(player, target, stats, damageDealt, source);
            ArcherSpecializationHandler.onHit(player, target, stats, source, damageDealt);
        }

        // Prepared Assassin effects need the pre-onHit gauges, but must not run on cancelled or
        // absorbed attacks. Reload after their refund so the later resource write preserves it.
        if (!projectile && !magic && source.getSource() == player) {
            AssassinSpecializationHandler.onConfirmedMeleeStart(player, target, damageDealt);
            stats = StatsManager.get(player);
        }

        // Mecanicas usam o estado anterior ao touch quando precisam distinguir emboscada/out-of-combat.
        if (ClassMechanics.handles(stats))
            ClassMechanics.onHit(player, target, stats, damageDealt, source, projectile, magic);
        stateCombatTouch(player);

        if (magic) MageCombatHandler.onMagicHitResolved(player, target, damageDealt);
        else MageCombatHandler.onPhysicalHit(player, target, damageDealt);
        stats = StatsManager.get(player);

        float passive = AbilityRegistry.sumPassive(stats.unlockedNodes, "resource_on_hit", false);
        float loopGain = ClassMechanics.handles(stats)
                ? ClassMechanics.resourceGainOnHit(player, target, stats, projectile, magic)
                : legacyBaseHitGain(stats.clazz, projectile, magic);
        float gain = Math.max(0f, loopGain + (magic ? 0f : passive));
        if (gain > 0 && stats.resource < stats.resourceMax) {
            stats.resource = Math.min(stats.resourceMax, stats.resource + gain);
            resourceChanged = true;
        }

        float lifesteal = GlobalCaps.lifesteal(AbilityRegistry.sumPassive(stats.unlockedNodes, "lifesteal"));
        if (lifesteal > 0 && !magic) WarriorSustain.heal(player, Math.min(6f, damageDealt * lifesteal), projectile ? ArcherShotTracker.launchWeapon(source, player) : WarriorSustain.meleeWeapon(player));

        float poison = AbilityRegistry.sumPassive(stats.unlockedNodes, "poison_hit");
        if (poison > 0 && !magic && !target.isDead()) {
            float durationScale = SoulslikeCombat.afflictionDurationMultiplier(stats)
                    * (1f - BossScaler.getStatusResistance(target));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,
                    Math.round((40 + poison * 20) * durationScale), poison >= 2 ? 1 : 0));
        }

        // Mantem compatibilidade com arvores legadas, mas as arvores expandidas nao ganham mais
        // prepared_cast/rotation_damage genericos no registro novo.
        if (!magic) {
            CombatState legacy = CombatState.get(player.getUuid());
            legacy.rotationTicks = 0;
            legacy.preparationTicks = 80;
            recordLegacyAction(stats, legacy, projectile ? "ranged" : "melee");
        }

        if (resourceChanged) StatsManager.saveAndSync(player, stats);

        // LivingEntityDamageMixin chama este metodo somente depois de dano real confirmado.
        // Especializacoes que dependem do estado produzido por ClassMechanics rodam aqui,
        // antes de consumirmos as janelas de "proximo golpe".
        if (!projectile && !magic && source.getSource() == player) {
            WarriorSpecializationHandler.onConfirmedMeleeHit(player, target, damageDealt);
            AssassinSpecializationHandler.onConfirmedMeleeHit(player, target, damageDealt);
        }
        if (ClassMechanics.handles(stats))
            ClassMechanics.consumePreparedHit(player, stats, projectile, magic);
    }

    public static void onPlayerHurt(ServerPlayerEntity player, float amount, DamageSource source) {
        if (amount <= 0) return;
        PlayerStats stats = StatsManager.get(player);
        if (ClassMechanics.handles(stats)) ClassMechanics.onHurt(player, stats, amount, source);
        stateCombatTouch(player);
        MageCombatHandler.onPlayerHurtResolved(player, amount);
        stats = StatsManager.get(player);

        float passive = AbilityRegistry.sumPassive(stats.unlockedNodes, "resource_on_hurt", false);
        float gain = ClassMechanics.handles(stats)
                ? ClassMechanics.resourceGainOnHurt(stats) + passive
                : (passive > 0 ? passive : legacyBaseHurtGain(stats.clazz));
        if (gain > 0 && stats.resource < stats.resourceMax) {
            stats.resource = Math.min(stats.resourceMax, stats.resource + gain);
            StatsManager.saveAndSync(player, stats);
        }

        // Nao reflete dano de espinhos novamente: evita cadeia/recursao em PvP.
        if (!source.isOf(DamageTypes.THORNS)) {
            float thorns = AbilityRegistry.sumPassive(stats.unlockedNodes, "thorns");
            Entity attacker = source.getAttacker();
            if (thorns > 0 && attacker instanceof LivingEntity living && living != player)
                living.damage(player.getDamageSources().thorns(player), Math.min(12f, amount * thorns));
        }
    }

    public static void onPlayerKill(ServerPlayerEntity player, LivingEntity victim) {
        PlayerStats stats = StatsManager.get(player);
        if (ClassMechanics.handles(stats)) ClassMechanics.onKill(player, victim, stats);

        float gain = AbilityRegistry.sumPassive(stats.unlockedNodes, "resource_on_kill")
                + (ClassMechanics.handles(stats) ? ClassMechanics.resourceGainOnKill(stats, victim) : 0f);
        if (gain > 0) stats.resource = Math.min(stats.resourceMax, stats.resource + gain);
        StatsManager.save(player, stats);

        float heal = AbilityRegistry.sumPassive(stats.unlockedNodes, "heal_on_kill");
        if (heal > 0) WarriorSustain.heal(player, heal, WarriorSustain.meleeWeapon(player));
        MageCombatHandler.onPlayerKill(player, victim);
    }

    /** Ativa um dos quatro slots. R/Z/X/C por padrao no cliente. */
    public static void activateAbility(ServerPlayerEntity player) { activateAbility(player, 0); }

    public static void activateAbility(ServerPlayerEntity player, int slot) {
        if (slot < 0 || slot >= PlayerStats.ACTIVE_SLOTS) return;
        CombatState state = CombatState.get(player.getUuid());
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz == RPGClass.MAGO) CompatManager.syncMageMana(player, stats);
        flushBufferedRegen(player, stats, state, false);

        String selected = stats.activeSlot(slot);
        if (selected == null || selected.isBlank() || !stats.unlockedNodes.contains(selected)
                || !AbilityRegistry.hasActive(selected)) {
            if (slot == 0) {
                selected = firstActive(stats);
                if (selected != null) stats.setActiveSlot(0, selected);
            }
            if (selected == null || selected.isBlank()) {
                player.sendMessage(Text.literal("§cSlot " + (slot + 1) + " sem habilidade ativa."), true);
                return;
            }
        }

        if (stats.clazz == RPGClass.ARQUEIRO && HouseRules.real(selected).startsWith("arc_art_cross_")
                && !ArcherShotTracker.isCrossbow(player.getMainHandStack())
                && !ArcherShotTracker.isCrossbow(player.getOffHandStack())) {
            player.sendMessage(Text.literal("Equipe uma besta para usar esta tecnica."), true);
            return;
        }
        if (!ArcherTechniqueHandler.canActivate(player, stats, HouseRules.real(selected))) {
            player.sendMessage(Text.literal("Prepare a condicao da tecnica: arma, alvo, carga ou armadilha propria."), true);
            return;
        }
        SkillEffect effect = AbilityRegistry.activeForNode(selected);
        if (effect == null) return;
        var physical = com.rpgstats.compat.PhysicalActionService.preflight(player, stats, selected, effect);
        if (physical.route() == com.rpgstats.compat.PhysicalActionBridge.Route.REJECTED) {
            player.sendMessage(Text.literal(physical.reason()), true);
            return;
        }
        int cooldown = state.cooldown(selected);
        if (cooldown > 0) {
            player.sendMessage(Text.literal("§cRecarga: " + ((cooldown + 19) / 20) + "s"), true);
            return;
        }

        boolean mageNode = MageCombatHandler.isMageNode(selected) && stats.clazz == RPGClass.MAGO;
        if (mageNode) {
            if (!CompatManager.canUseRpgTechnique(player)) {
                player.sendMessage(Text.literal("§cConclua ou cancele a spell atual antes de usar esta tecnica."), true);
                return;
            }
            if (!MageCombatHandler.tryPayCost(player, stats, selected, effect)) return;
            state.startCooldown(selected, MageCombatHandler.effectiveCooldown(player, stats, selected, effect));
            StatsManager.save(player, stats);
            MageCombatHandler.activate(player, stats, selected, effect);
            CompatManager.setMageMana(player, stats, stats.resource);
        } else {
            float reduction = Math.min(.25f, AbilityRegistry.sumPassive(stats.unlockedNodes, "resource_cost_reduction"));
            float cost = effect.resourceCost() * (1f - reduction) * (1f + HouseRules.surcharge(stats));
            // prepared_cast so existe para conteudo legado. O rework novo tem preparacoes proprias.
            if (!ClassAbilityRegistry.CLASS_ACTIVE.equals(effect.effectId()) && state.preparationTicks > 0)
                cost *= 1f - Math.min(.15f, AbilityRegistry.sumPassive(stats.unlockedNodes, "prepared_cast"));
            if (stats.resource < cost) {
                player.sendMessage(Text.literal("Recurso insuficiente: " + (int) stats.resource + "/" + (int) Math.ceil(cost)), true);
                return;
            }
            stats.resource -= cost;
            boolean externalMovement = physical.route() == com.rpgstats.compat.PhysicalActionBridge.Route.EXTERNAL;
            if (externalMovement && !com.rpgstats.compat.PhysicalActionService.executeAuthorized(player, stats, selected, effect)) {
                stats.resource += cost;
                StatsManager.saveAndSync(player, stats);
                player.sendMessage(Text.literal("Nao foi possivel iniciar o rolamento. Tente novamente."), true);
                return;
            }
            state.startCooldown(selected, effect.cooldownTicks());
            StatsManager.save(player, stats);

            if (ClassAbilityRegistry.CLASS_ACTIVE.equals(effect.effectId()))
                ClassMechanics.activate(player, stats, selected, effect, externalMovement);
            else applyLegacyActive(player, effect);

            // Keep the same PlayerStats instance: class actives may spend an additional
            // class-specific resource amount (for example Berserker Fury) after the base cost.
            // Reloading here restored the pre-activation NBT and silently refunded that spend.
            if (!ClassAbilityRegistry.CLASS_ACTIVE.equals(effect.effectId())) {
                state.preparationTicks = 0;
                state.rotationTicks = 80;
                state.castGuardTicks = 40;
                recordLegacyAction(stats, state, selected);
            }
        }
        StatsManager.saveAndSync(player, stats);

        SkillNode node = StatsManager.findNode(stats, selected);
        String affinity = "arc_magic_technique".equals(HouseRules.real(selected))
                ? " · " + (state.classMode == 0 ? "Fogo" : state.classMode == 1 ? "Gelo" : "Tempestade") : "";
        player.sendMessage(Text.literal("§a§l[" + (slot + 1) + "] §e" + (node == null ? selected : node.name()) + affinity), true);
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        CombatState state = CombatState.get(player.getUuid());
        state.tick();
        MageCombatHandler.tick(player);

        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz == null) return;
        if (ClassMechanics.handles(stats)) ClassMechanics.tick(player, stats);
        ArcherTechniqueHandler.tick(player, stats);
        com.rpgstats.integration.ArcherTrapCompat.tick(player, stats);

        boolean staminaChanged = SoulslikeCombat.tickStamina(player, stats);
        boolean externalMana = stats.clazz == RPGClass.MAGO && CompatManager.usesExternalMageMana();
        boolean externalManaChanged = externalMana && CompatManager.syncMageMana(player, stats);

        if (!externalMana && stats.resource < stats.resourceMax) {
            state.primaryRegenBuffer += stats.clazz == RPGClass.MAGO
                    ? MageCombatHandler.mageRegenPerTick(stats, player)
                    : regenPerTick(stats.clazz);
        } else if (externalMana) {
            // Iron's owns regeneration and current/max Mana; never run a second RPG regen loop.
            state.primaryRegenBuffer = 0f;
        } else state.primaryRegenBuffer = 0f;

        if (player.age % 5 == 0) {
            boolean synced = flushBufferedRegen(player, stats, state, true);
            if (externalManaChanged && !synced) {
                StatsManager.saveAndSync(player, stats);
                synced = true;
            }
            if (staminaChanged && !synced) {
                StatsManager.saveAndSync(player, stats);
                synced = true;
            }
            if (!synced && hasVisibleCooldown(stats, state)) RPGStatsMod.syncStats(player);
        }
    }

    private static void applyLegacyActive(ServerPlayerEntity player, SkillEffect effect) {
        CombatState state = CombatState.get(player.getUuid());
        ServerWorld world = player.getServerWorld();
        PlayerStats stats = StatsManager.get(player);
        float healingMultiplier = SoulslikeCombat.healingMultiplier(stats);

        switch (effect.effectId()) {
            case "active_self_heal" -> player.heal(effect.value() * healingMultiplier);
            case "active_group_heal" -> {
                Box box = player.getBoundingBox().expand(8);
                for (ServerPlayerEntity ally : world.getEntitiesByClass(ServerPlayerEntity.class, box,
                        p -> p.isAlive() && (p == player || player.getScoreboardTeam() == null || player.isTeammate(p))))
                    ally.heal(effect.value() * healingMultiplier);
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                        SoundCategory.PLAYERS, 0.7f, 1.25f);
            }
            case "active_guard", "active_guard_taunt" -> {
                state.guardTicks = effect.durationTicks();
                state.guardReduction = effect.value();
                if (effect.effectId().equals("active_guard_taunt")) {
                    Box box = player.getBoundingBox().expand(12);
                    for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, box, Entity::isAlive)) mob.setTarget(player);
                }
            }
            case "active_power" -> { state.powerTicks = effect.durationTicks(); state.powerBonus = effect.value(); }
            case "active_bloodlust" -> { state.bloodlustTicks = effect.durationTicks(); state.bloodlustBonus = effect.value(); }
            case "active_smoke" -> {
                state.smokeTicks = effect.durationTicks();
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, effect.durationTicks(), 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, effect.durationTicks(), 1));
            }
            case "active_dash" -> {
                Vec3d look = player.getRotationVec(1f).multiply(effect.value());
                player.addVelocity(look.x, 0.15, look.z);
                player.velocityModified = true;
            }
            case "active_smite" -> { state.smiteMultiplier = Math.max(1f, effect.value()); state.smiteTicks = Math.max(20, effect.durationTicks()); }
            case "active_aoe" -> {
                Box box = player.getBoundingBox().expand(5);
                DamageSource magic = player.getDamageSources().indirectMagic(player, player);
                for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box,
                        ent -> ent != player && ent.isAlive() && isOffensiveTarget(player, ent)))
                    target.damage(magic, effect.value());
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE,
                        SoundCategory.PLAYERS, 0.6f, 1.2f);
            }
            case "active_arrow_rain" -> {
                HitResult hit = player.raycast(24.0, 1.0f, false);
                Vec3d center = hit.getPos();
                int arrows = Math.max(1, Math.round(effect.value()));
                for (int i = 0; i < arrows; i++) {
                    ArrowEntity arrow = new ArrowEntity(world, player);
                    double ox = (player.getRandom().nextDouble() - 0.5) * 6.0;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 6.0;
                    arrow.setPosition(center.x + ox, center.y + 8.0 + player.getRandom().nextDouble() * 2.0, center.z + oz);
                    arrow.setVelocity(0, -1.2, 0);
                    arrow.setDamage(3.0);
                    arrow.pickupType = net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.DISALLOWED;
                    world.spawnEntity(arrow);
                }
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, 0.7f);
            }
            default -> { }
        }
    }

    private static void recordLegacyAction(PlayerStats stats, CombatState state, String action) {
        state.comboActions.add(action);
        state.comboTicks = 80;
        if (state.comboActions.size() >= 3) {
            if (state.comboCooldown <= 0) {
                stats.resource = Math.min(stats.resourceMax,
                        stats.resource + Math.min(6f, AbilityRegistry.sumPassive(stats.unlockedNodes, "combo_resource")));
                state.comboCooldown = 60;
            }
            state.comboActions.clear();
        }
    }

    private static void stateCombatTouch(ServerPlayerEntity player) {
        CombatState.get(player.getUuid()).combatTicks = 200;
    }

    private static boolean hasVisibleCooldown(PlayerStats stats, CombatState state) {
        for (int slot = 0; slot < PlayerStats.ACTIVE_SLOTS; slot++) {
            String id = stats.activeSlot(slot);
            if (id != null && !id.isBlank() && state.cooldown(id) > 0) return true;
        }
        return false;
    }

    private static boolean flushBufferedRegen(ServerPlayerEntity player, PlayerStats stats, CombatState state, boolean sync) {
        boolean changed = false;
        if (state.primaryRegenBuffer > 0 && stats.resource < stats.resourceMax) {
            stats.resource = Math.min(stats.resourceMax, stats.resource + state.primaryRegenBuffer);
            state.primaryRegenBuffer = 0f;
            changed = true;
        }
        if (changed) {
            if (sync) StatsManager.saveAndSync(player, stats);
            else StatsManager.save(player, stats);
        }
        return changed && sync;
    }

    private static float regenPerTick(RPGClass clazz) {
        if (clazz == null) return 0f;
        return switch (clazz) {
            case MAGO -> 0.15f;
            case ASSASSINO -> 0.35f;
            case ARQUEIRO -> 0.12f;
            case GUERREIRO -> 0.05f;
        };
    }

    private static float legacyBaseHitGain(RPGClass clazz, boolean projectile, boolean magic) {
        if (clazz == null || magic) return 0f;
        return switch (clazz) {
            case GUERREIRO -> projectile ? 0.5f : 3f;
            case ASSASSINO -> projectile ? 1f : 2f;
            case ARQUEIRO -> projectile ? 4f : 1f;
            case MAGO -> 0.5f;
        };
    }

    private static float legacyBaseHurtGain(RPGClass clazz) {
        if (clazz == RPGClass.GUERREIRO) return 2f;
        return 0f;
    }

    private static boolean isProjectile(DamageSource source) { return source.getSource() instanceof ProjectileEntity; }
    private static boolean isMagic(DamageSource source) { return source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC); }

    private static boolean isOffensiveTarget(ServerPlayerEntity player, LivingEntity target) {
        if (target instanceof PlayerEntity) return false;
        if (target instanceof PassiveEntity) return false;
        if (target instanceof TameableEntity tameable && tameable.getOwner() == player) return false;
        return true;
    }

    private static String firstActive(PlayerStats stats) {
        for (String nodeId : stats.unlockedNodes) if (AbilityRegistry.hasActive(nodeId)) return nodeId;
        return null;
    }

    private CombatHandler() {}
}
