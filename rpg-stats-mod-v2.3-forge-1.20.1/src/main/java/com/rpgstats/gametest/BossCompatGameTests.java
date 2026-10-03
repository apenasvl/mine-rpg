package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.integration.FixedBossProfile;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossCompatGameTests {
    @GameTest(templateName="empty",tickLimit=120)
    public static void mariumRangedWeaponsAllowOtherClassesWithDamagePenalty(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        try {
            for(String name:java.util.List.of("galeforce","darkmoon_longbow","kraken_slayer","simons_bowblade","kraken_slayer_crossbow")) {
                var p=TestPlayers.create(c);
                var item=Registries.ITEM.get(new Identifier("soulsweapons",name));
                var weapon=new net.minecraft.item.ItemStack(item);
                var stats=new com.rpgstats.stats.PlayerStats();stats.level=50;
                stats.stats.put(com.rpgstats.stats.Stat.DESTREZA,50);
                for(var clazz:com.rpgstats.classes.RPGClass.values()) {
                    stats.clazz=clazz;com.rpgstats.stats.StatsManager.save(p,stats);
                    c.assertTrue(com.rpgstats.compat.bosses.BossEquipmentService.check(p,weapon).allowed(),name+" rejected "+clazz);
                    float expected=clazz==com.rpgstats.classes.RPGClass.ARQUEIRO?1f:.625f;
                    c.assertTrue(com.rpgstats.compat.bosses.WeaponAffinity.damageFactor(stats,weapon)==expected,name+" affinity mismatch "+clazz);
                }
                stats.clazz=com.rpgstats.classes.RPGClass.ARQUEIRO;
                stats.path=com.rpgstats.classes.RPGPath.ARC_ARCANE;
                stats.specialization=com.rpgstats.classes.RPGSpecialization.FROSTBOW;
                stats.unlockedNodes.add("arc_magic_foundation");
                stats.refreshResourceMax();stats.resource=stats.resourceMax;
                com.rpgstats.stats.StatsManager.save(p,stats);
                com.rpgstats.combat.CombatState.remove(p.getUuid());
                com.rpgstats.combat.CombatState.get(p.getUuid()).classMode=1;
                p.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,weapon);
                p.getInventory().setStack(5,new net.minecraft.item.ItemStack(net.minecraft.item.Items.ARROW,8));
                var damageId=new Identifier("ranged_weapon","damage");
                if(Registries.ATTRIBUTE.containsId(damageId))c.assertTrue(weapon.getAttributeModifiers(net.minecraft.entity.EquipmentSlot.MAINHAND)
                        .get(Registries.ATTRIBUTE.get(damageId)).isEmpty(),name+" retained unbounded native damage");
                if(item instanceof net.minecraft.item.CrossbowItem) {
                    var ammo=new net.minecraft.nbt.NbtList();ammo.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.ARROW).writeNbt(new net.minecraft.nbt.NbtCompound()));
                    weapon.getOrCreateNbt().put("ChargedProjectiles",ammo);
                    net.minecraft.item.CrossbowItem.setCharged(weapon,true);
                    item.use(p.getWorld(),p,net.minecraft.util.Hand.MAIN_HAND);
                } else {
                    item.use(p.getWorld(),p,net.minecraft.util.Hand.MAIN_HAND);
                    for(int tick=0;tick<80;tick++)p.playerTick();
                    p.stopUsingItem();
                    c.assertTrue(p.getInventory().getStack(5).getCount()==7,name+" did not consume one arrow");
                }
                var arrows=p.getServerWorld().getEntitiesByClass(net.minecraft.entity.projectile.PersistentProjectileEntity.class,
                        p.getBoundingBox().expand(4),e->e.getOwner()==p);
                c.assertTrue(!arrows.isEmpty(),name+" failed to shoot");
                var source=p.getDamageSources().arrow(arrows.get(0),p);
                c.assertTrue(com.rpgstats.combat.ArcherShotTracker.isBowShot(source),name+" lost Archer tracking");
                p.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_SWORD));
                c.assertTrue(com.rpgstats.combat.ArcherShotTracker.launchWeapon(source,p).isOf(item),name+" lost launch weapon after swap");
                var target=EntityType.COW.create(c.getWorld());
                target.refreshPositionAndAngles(p.getX()+2,p.getY(),p.getZ(),0,0);c.getWorld().spawnEntity(target);
                target.damage(source,2f);
                c.assertTrue(target.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS),name+" did not apply frost affinity");
                target.discard();arrows.forEach(net.minecraft.entity.Entity::discard);
            }
        } finally {TestPlayers.finish(c);}
        c.complete();
    }
    private static void apply(LivingEntity entity, FixedBossProfile profile, int participants) {
        try {
            var method = BossScaler.class.getDeclaredMethod("applyFixedScale", LivingEntity.class, FixedBossProfile.class, int.class);
            method.setAccessible(true);
            method.invoke(null, entity, profile, participants);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void fixedModifierDoesNotCompoundAndPreservesFraction(TestContext c) {
        var entity = EntityType.ZOMBIE.create(c.getWorld());
        c.assertTrue(entity != null,"Missing zombie fixture");
        var profile = new FixedBossProfile(31,40,35,3,4);
        float base=entity.getMaxHealth();entity.setHealth(base/2);
        apply(entity,profile,1);
        c.assertTrue(Math.abs(entity.getMaxHealth()-base*3)<.01,"Fixed factor absent");
        apply(entity,profile,1);
        c.assertTrue(Math.abs(entity.getMaxHealth()-base*3)<.01,"Fixed factor compounded");
        apply(entity,profile,4);
        c.assertTrue(Math.abs(entity.getHealth()/entity.getMaxHealth()-.5)<.001,"Party restored health");
        apply(entity,profile,1);
        c.assertTrue(Math.abs(entity.getHealth()/entity.getMaxHealth()-.5)<.001,"Party removal changed fraction");
        entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(base*2);
        c.assertTrue(Math.abs(entity.getMaxHealth()-base*6)<.01,"Native base change lost fixed multiplier");
        float phaseFraction=entity.getHealth()/entity.getMaxHealth();
        BossScaler.clearEncounterScaling(entity);
        c.assertTrue(Math.abs(entity.getMaxHealth()-base*2)<.01,"Removed profile retained fixed modifier");
        c.assertTrue(Math.abs(entity.getHealth()/entity.getMaxHealth()-phaseFraction)<.001,"Removed profile changed health fraction");
        BossScaler.untrack(entity);c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void pinnedNativeBossesHaveTheirExpectedRegistrations(TestContext c) {
        String[][] ids={
            {"legendary_monsters","legendary_monsters:overgrown_colossus","legendary_monsters:the_obliterator"},
            {"soulsweapons","soulsweapons:moonknight","soulsweapons:returning_knight"},
            {"bosses_of_mass_destruction","bosses_of_mass_destruction:lich","bosses_of_mass_destruction:gauntlet"}
        };
        for(String[] mod:ids) if(ModList.get().isLoaded(mod[0])) for(int i=1;i<mod.length;i++) {
            var id=new Identifier(mod[i]);
            c.assertTrue(Registries.ENTITY_TYPE.containsId(id),"Missing native registry: "+id);
            var entity=Registries.ENTITY_TYPE.get(id).create(c.getWorld());
            c.assertTrue(entity instanceof LivingEntity,"Native boss not living: "+id);
            entity.discard();
        }
        c.complete();
    }
    private static Object readDefinitions(String json) throws Exception {
        var id=new Identifier("rpgstats","rpgstats/test.json");
        var resource=new net.minecraft.resource.Resource(null, () -> new java.io.ByteArrayInputStream(
                json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var manager=(net.minecraft.resource.ResourceManager) java.lang.reflect.Proxy.newProxyInstance(
                BossCompatGameTests.class.getClassLoader(),new Class<?>[]{net.minecraft.resource.ResourceManager.class},
                (proxy,method,args)-> {
                    if(method.getReturnType()==java.util.Map.class && method.getParameterCount()==2)return java.util.Map.of(id,resource);
                    throw new UnsupportedOperationException(method.getName());
                });
        var load=com.rpgstats.integration.DataDrivenRegistry.class.getDeclaredMethod("load",net.minecraft.resource.ResourceManager.class);
        load.setAccessible(true);return load.invoke(null,manager);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void readerKeepsSnapshotAndCommonAttackAllowsOffClass(TestContext c) {
        var player=TestPlayers.create(c);
        java.lang.reflect.Field field=null;Object previous=null;
        try {
            field=com.rpgstats.integration.DataDrivenRegistry.class.getDeclaredField("snapshot");
            field.setAccessible(true);previous=field.get(null);
            String legacy="{\"type\":\"bosses\",\"entries\":[{\"id\":\"minecraft:zombie\",\"tier\":2}]}";
            var legacySnapshot=readDefinitions(legacy);c.assertTrue(legacySnapshot!=previous,"Legacy reader failed");
            String valid="{\"type\":\"equipment_rules\",\"entries\":[{\"id\":\"minecraft:diamond_sword\",\"min_level\":35,\"classes\":[\"GUERREIRO\"],\"stats\":{\"str\":30}}]}";
            Object accepted=readDefinitions(valid);field.set(null,accepted);
            for(String invalid:new String[]{valid.replace("GUERREIRO","INVALID_CLASS"),
                    valid.replace("35","35.5"),valid.replace("30","-1"),
                    "{\"type\":\"bosses\",\"entries\":[{\"id\":\"minecraft:zombie\"},{\"id\":\"minecraft:zombie\"}]}",
                    "{\"type\":\"bosses\",\"entries\":[{\"id\":\"minecraft:zombie\",\"fixed\":{\"health_factor\":1e999}}]}"})
                c.assertTrue(readDefinitions(invalid)==accepted,"Invalid reload replaced snapshot");
            var stats=new com.rpgstats.stats.PlayerStats();stats.level=50;stats.clazz=com.rpgstats.classes.RPGClass.MAGO;
            stats.stats.put(com.rpgstats.stats.Stat.FORCA,30);com.rpgstats.stats.StatsManager.save(player,stats);
            var sword=new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_SWORD);
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,sword);
            var target=EntityType.ZOMBIE.create(c.getWorld());
            var event=new net.minecraftforge.event.entity.player.AttackEntityEvent(player,target);
            com.rpgstats.forge.ForgeEvents.equipmentAttack(event);
            c.assertTrue(!event.isCanceled(),"Off-class weapon blocked despite level and attributes");
            c.assertTrue(player.getMainHandStack()==sword,"Denied item was removed");
            stats.clazz=com.rpgstats.classes.RPGClass.GUERREIRO;stats.level=34;com.rpgstats.stats.StatsManager.save(player,stats);
            c.assertTrue(!com.rpgstats.compat.bosses.BossEquipmentService.check(player,sword).allowed(),"Below-level sword accepted");
            stats.level=35;com.rpgstats.stats.StatsManager.save(player,stats);
            c.assertTrue(com.rpgstats.compat.bosses.BossEquipmentService.check(player,sword).allowed(),"Eligible sword rejected");
            Object disabled=readDefinitions(valid.replace("\"min_level\"","\"enabled\":false,\"min_level\""));field.set(null,disabled);
            c.assertTrue(!com.rpgstats.compat.bosses.BossEquipmentService.check(player,sword).allowed(),"Explicitly disabled native item can still be used");
        } catch(Exception error){throw new AssertionError(error);}
        finally {
            try {if(field!=null && previous!=null)field.set(null,previous);}catch(Exception error){throw new AssertionError(error);}
            TestPlayers.finish(c);
        }
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativeResistancePacketCannotGrantUnboundedDefense(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var player=TestPlayers.create(c);
        try {
            Class<?> type=Class.forName("net.soulsweaponry.networking.packets.C2S.GiveResistanceC2S");
            Object packet=type.getConstructor().newInstance();
            var handler=java.util.Arrays.stream(type.getDeclaredMethods())
                    .filter(m->m.getName().equals("handlePacket")).findFirst().orElseThrow();
            handler.setAccessible(true);handler.invoke(packet,player,packet);
            c.assertTrue(!player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.RESISTANCE),
                    "Native client packet granted unbounded resistance");
        } catch(ReflectiveOperationException error){throw new AssertionError(error);}
        finally {TestPlayers.finish(c);}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void recordNativeLivingCatalogForCalibration(TestContext c) {
        java.util.Set<String> namespaces=java.util.Set.of("legendary_monsters","soulsweapons","bosses_of_mass_destruction");
        for(var id:Registries.ENTITY_TYPE.getIds()) if(namespaces.contains(id.getNamespace())) {
            var entity=Registries.ENTITY_TYPE.get(id).create(c.getWorld());
            if(entity instanceof LivingEntity living) {
                var attack=living.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                boolean bossBar=false;
                for(Class<?> type=entity.getClass();type!=null;type=type.getSuperclass())
                    for(var field:type.getDeclaredFields()) if(field.getType()==net.minecraft.entity.boss.ServerBossBar.class)bossBar=true;
                RPGStatsMod.LOGGER.info("RPG_BOSS_NATIVE id={} class={} health={} attack={} armor={} bossBar={}",
                        id,entity.getClass().getName(),living.getMaxHealth(),attack==null?0:attack.getValue(),living.getArmor(),bossBar);
            }
            if(entity!=null)entity.discard();
        }
        for(var id:Registries.ITEM.getIds()) if(namespaces.contains(id.getNamespace())) {
            var item=Registries.ITEM.get(id);
            var stack=new net.minecraft.item.ItemStack(item);
            var attributes=stack.getAttributeModifiers(net.minecraft.entity.EquipmentSlot.MAINHAND);
            RPGStatsMod.LOGGER.info("RPG_ITEM_NATIVE id={} class={} sword={} bow={} crossbow={} attributes={}",
                    id,item.getClass().getName(),item instanceof net.minecraft.item.SwordItem,
                    item instanceof net.minecraft.item.BowItem,item instanceof net.minecraft.item.CrossbowItem,attributes);
        }
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativeSwordCannotGrantLifestealOutsideRpgBuild(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var p=TestPlayers.create(c);
        try {
            p.setHealth(10);
            var sword=new net.minecraft.item.ItemStack(Registries.ITEM.get(new Identifier("soulsweapons:bloodthirster")));
            p.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,sword);
            var target=EntityType.ZOMBIE.create(c.getWorld());
            sword.postHit(target,p);
            c.assertTrue(p.getHealth()==10 && p.getAbsorptionAmount()==0,"Native sword granted free sustain outside the RPG build");
        }finally{TestPlayers.finish(c);}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativeSwordDamageFitsCoreBudget(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var stack=new net.minecraft.item.ItemStack(Registries.ITEM.get(new Identifier("soulsweapons:darkin_blade")));
        var attributes=stack.getAttributeModifiers(net.minecraft.entity.EquipmentSlot.MAINHAND);
        double damage=attributes.get(EntityAttributes.GENERIC_ATTACK_DAMAGE).stream().mapToDouble(m->m.getValue()).sum();
        double speed=4+attributes.get(EntityAttributes.GENERIC_ATTACK_SPEED).stream().mapToDouble(m->m.getValue()).sum();
        c.assertTrue(damage<=7.01 && speed<=1.61,"Native physical weapon exceeds the core netherite budget");
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativeRingPacketCannotCastOutsideRpgBuild(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var player=TestPlayers.create(c);
        try {
            Class<?> type=Class.forName("net.soulsweaponry.networking.packets.C2S.MoonlightC2S");
            Object packet=type.getConstructor().newInstance();
            var handler=java.util.Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().equals("handlePacket")).findFirst().orElseThrow();
            handler.setAccessible(true);handler.invoke(packet,player,packet);
            var ring=Registries.ITEM.get(new Identifier("soulsweapons:moonstone_ring"));
            c.assertTrue(!player.getItemCooldownManager().isCoolingDown(ring),"Native ring packet cast and charged cooldown outside the RPG build");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        finally{TestPlayers.finish(c);}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void bossCompletionRewardsTankAndSupportOnce(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var damage=TestPlayers.create(c);var tank=TestPlayers.create(c);var support=TestPlayers.create(c);var idle=TestPlayers.create(c);
        var boss=(LivingEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());
        try {
            c.getWorld().spawnEntity(boss);
            com.rpgstats.boss.EncounterManager.recordDamageDealt(damage,boss,100);
            com.rpgstats.boss.EncounterManager.recordDamageTaken(tank,boss,10);
            com.rpgstats.boss.EncounterManager.recordSupport(support,tank,5,0);
            com.rpgstats.forge.ForgeEvents.confirmedDeath(boss,boss.getDamageSources().playerAttack(damage));
            for(var p:java.util.List.of(damage,tank,support)) c.assertTrue(
                com.rpgstats.stats.StatsManager.get(p).defeatedBossTypes.contains("soulsweapons:returning_knight"),
                "A real damage/tank/support contributor missed completion XP");
            c.assertTrue(!com.rpgstats.stats.StatsManager.get(idle).defeatedBossTypes.contains("soulsweapons:returning_knight"),"Nearby idle player received reward");
            int level=com.rpgstats.stats.StatsManager.get(damage).level;int xp=com.rpgstats.stats.StatsManager.get(damage).xp;
            com.rpgstats.forge.ForgeEvents.confirmedDeath(boss,boss.getDamageSources().playerAttack(damage));
            c.assertTrue(com.rpgstats.stats.StatsManager.get(damage).level==level && com.rpgstats.stats.StatsManager.get(damage).xp==xp,"Completion XP repeated");
        }finally{boss.discard();TestPlayers.finish(c);}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void selectedNativeSwordRetainsVanillaDurability(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var p=TestPlayers.create(c);
        try {
            var sword=new net.minecraft.item.ItemStack(Registries.ITEM.get(new Identifier("soulsweapons:bloodthirster")));
            sword.postHit(EntityType.ZOMBIE.create(c.getWorld()),p);
            c.assertTrue(sword.getDamage()>0,"Removing native abilities also removed weapon durability cost");
        }finally{TestPlayers.finish(c);}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void allNativeBossesReceiveFixedProgression(TestContext c) {
        String[] ids={"soulsweapons:night_shade","soulsweapons:draugr_boss","soulsweapons:chaos_monarch","soulsweapons:accursed_lord_boss","soulsweapons:returning_knight","soulsweapons:moonknight","soulsweapons:day_stalker","soulsweapons:night_prowler","legendary_monsters:cloud_golem","legendary_monsters:posessed_paladin","legendary_monsters:the_obliterator","bosses_of_mass_destruction:lich","bosses_of_mass_destruction:gauntlet","bosses_of_mass_destruction:void_blossom","bosses_of_mass_destruction:obsidilith"};
        for(String name:ids) {
            var id=new Identifier(name);if(!ModList.get().isLoaded(id.getNamespace()))continue;
            var boss=(LivingEntity)Registries.ENTITY_TYPE.get(id).create(c.getWorld());
            try {
                var profile=com.rpgstats.integration.IntegrationServices.INSTANCE.boss(boss).orElse(null);
                c.assertTrue(profile!=null && profile.fixed()!=null,"Native boss lacks fixed progression: "+id);
                c.getWorld().spawnEntity(boss);
                c.assertTrue(BossScaler.getTier(boss)==profile.tier(),"Native profile was not applied: "+id);
            }finally{boss.discard();BossScaler.untrack(boss);}
        }
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void legendaryArmorPacketsCannotActivateDiscardedPowers(TestContext c) {
        if(!ModList.get().isLoaded("legendary_monsters")){c.complete();return;}
        String[] names={"AnnihilatorHelmetAbilityMessage","AnnihilatorChestplatePlaySoundMessage","SkeloraptorRoarKeyMessage","SkeloraptorTailAttackMessage","MessageArmorKey"};
        try {
            for(String name:names) {
                Class<?> packet=Class.forName("net.miauczel.legendary_monsters.Message."+name);
                Object instance=packet.getConstructor().newInstance();
                Class<?> handler=name.equals("MessageArmorKey")?packet:Class.forName(packet.getName()+"$Handler");
                var method=java.util.Arrays.stream(handler.getDeclaredMethods()).filter(m->m.getName().equals(name.equals("MessageArmorKey")?"handle":"onMessage")).findFirst().orElseThrow();
                var called=new java.util.concurrent.atomic.AtomicInteger();
                java.util.function.Supplier<Object> context=()->{called.incrementAndGet();return null;};
                try{method.invoke(null,instance,context);}catch(java.lang.reflect.InvocationTargetException ignored){}
                c.assertTrue(called.get()==0,"Discarded armor power reached its native handler: "+name);
            }
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativeMoonknightTransitionDoesNotRewardPrematurely(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var p=TestPlayers.create(c);
        var boss=(LivingEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:moonknight")).create(c.getWorld());
        try {
            c.getWorld().spawnEntity(boss);
            var before=com.rpgstats.stats.StatsManager.get(p);int xp=before.xp;int level=before.level;
            boss.damage(boss.getDamageSources().playerAttack(p),boss.getMaxHealth()+1);
            c.assertTrue(boss.isAlive(),"Native transition was turned into death");
            c.assertTrue((boolean)boss.getClass().getMethod("isInitiatingPhaseTwo").invoke(boss),"Native transition did not initiate");
            var after=com.rpgstats.stats.StatsManager.get(p);
            c.assertTrue(after.xp==xp && after.level==level && !after.defeatedBossTypes.contains("soulsweapons:moonknight"),"Phase transition rewarded XP");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        finally{boss.discard();TestPlayers.finish(c);}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativePartnerPairRewardsOnlyAfterBothDefeated(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var tank=TestPlayers.create(c);var damage=TestPlayers.create(c);
        var day=(LivingEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:day_stalker")).create(c.getWorld());
        var night=(LivingEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:night_prowler")).create(c.getWorld());
        try {
            c.getWorld().spawnEntity(day);c.getWorld().spawnEntity(night);
            day.getClass().getMethod("setPartnerUuid",java.util.UUID.class).invoke(day,night.getUuid());
            night.getClass().getMethod("setPartnerUuid",java.util.UUID.class).invoke(night,day.getUuid());
            com.rpgstats.boss.EncounterManager.recordDamageTaken(tank,day,10);
            com.rpgstats.boss.EncounterManager.recordDamageDealt(damage,night,100);
            day.setHealth(0);com.rpgstats.forge.ForgeEvents.confirmedDeath(day,day.getDamageSources().playerAttack(damage));
            for(var p:java.util.List.of(tank,damage))c.assertTrue(com.rpgstats.stats.StatsManager.get(p).level==1 && com.rpgstats.stats.StatsManager.get(p).xp==0,"First partner awarded completion XP prematurely");
            night.setHealth(0);com.rpgstats.forge.ForgeEvents.confirmedDeath(night,night.getDamageSources().playerAttack(damage));
            c.assertTrue(com.rpgstats.stats.StatsManager.get(tank).level>1 && com.rpgstats.stats.StatsManager.get(damage).level>1,"Contributors across native partners lost completion XP");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        finally{day.discard();night.discard();TestPlayers.finish(c);}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativeArmorNeverExceedsNetheriteDefenseBudget(TestContext c) {
        for(var id:Registries.ITEM.getIds())if(java.util.Set.of("soulsweapons","legendary_monsters").contains(id.getNamespace())) {
            var item=Registries.ITEM.get(id);if(!(item instanceof net.minecraft.item.ArmorItem armor))continue;
            var slot=armor.getSlotType();var attrs=new net.minecraft.item.ItemStack(item).getAttributeModifiers(slot);
            double max=switch(slot){case CHEST->8;case LEGS->6;case HEAD->3;default->3;};
            double defense=attrs.get(EntityAttributes.GENERIC_ARMOR).stream().mapToDouble(m->m.getValue()).sum();
            double tough=attrs.get(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).stream().mapToDouble(m->m.getValue()).sum();
            c.assertTrue(defense<=max && tough<=3,"Native armor exceeds the tested defensive budget: "+id);
        }
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void absorbedDamageCreatesNoXpContribution(TestContext c) {
        var p=TestPlayers.create(c);var boss=EntityType.ZOMBIE.create(c.getWorld());
        java.lang.reflect.Field field=null;Object previous=null;
        try {
            field=com.rpgstats.integration.DataDrivenRegistry.class.getDeclaredField("snapshot");field.setAccessible(true);previous=field.get(null);
            field.set(null,readDefinitions("{\"type\":\"bosses\",\"entries\":[{\"id\":\"minecraft:zombie\",\"tier\":3}]}"));
            c.getWorld().spawnEntity(boss);boss.setAbsorptionAmount(100);float before=boss.getHealth();
            boss.damage(boss.getDamageSources().playerAttack(p),5);
            c.assertTrue(boss.getHealth()==before,"Absorption fixture lost health");
            c.assertTrue(com.rpgstats.boss.EncounterManager.contributionScore(boss.getUuid(),p.getUuid())==0,"Absorbed attack created XP contribution without lost health");
        }catch(Exception error){throw new AssertionError(error);}
        finally{boss.discard();com.rpgstats.boss.EncounterManager.removeBoss(boss);TestPlayers.finish(c);try{if(field!=null)field.set(null,previous);}catch(Exception error){throw new AssertionError(error);}}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void lethalHealthLossIsCreditedBeforeCompletionXp(TestContext c) {
        var killer=TestPlayers.create(c);var tank=TestPlayers.create(c);var boss=EntityType.ZOMBIE.create(c.getWorld());
        java.lang.reflect.Field field=null;Object previous=null;
        try {
            field=com.rpgstats.integration.DataDrivenRegistry.class.getDeclaredField("snapshot");field.setAccessible(true);previous=field.get(null);
            field.set(null,readDefinitions("{\"type\":\"bosses\",\"entries\":[{\"id\":\"minecraft:zombie\",\"tier\":3}]}"));
            c.getWorld().spawnEntity(boss);
            com.rpgstats.boss.EncounterManager.recordDamageTaken(tank,boss,5);
            boss.damage(boss.getDamageSources().playerAttack(killer),100);
            for(var p:java.util.List.of(killer,tank))c.assertTrue(com.rpgstats.stats.StatsManager.get(p).defeatedBossTypes.contains("minecraft:zombie"),"Lethal attacker or tank missed completion XP");
        }catch(Exception error){throw new AssertionError(error);}
        finally{boss.discard();com.rpgstats.boss.EncounterManager.removeBoss(boss);TestPlayers.finish(c);try{if(field!=null)field.set(null,previous);}catch(Exception error){throw new AssertionError(error);}}
        c.complete();
    }
    private BossCompatGameTests() {}
}
