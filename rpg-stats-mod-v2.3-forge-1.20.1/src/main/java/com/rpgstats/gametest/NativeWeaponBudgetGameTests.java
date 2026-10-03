package com.rpgstats.gametest;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.stats.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
/** Real survival attacks; compare intrinsic budgets separately from declared class affinity. */
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NativeWeaponBudgetGameTests {
    @GameTest(templateName="empty",tickLimit=1350)
    public static void nativeWeaponsStayWithinCoreBudgetAtFiveThirtyAndSixtySeconds(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        java.util.List<ServerPlayerEntity> players=new java.util.ArrayList<>();
        java.util.List<net.minecraft.entity.mob.ZombieEntity> targets=new java.util.ArrayList<>();
        java.util.List<Float> initial=new java.util.ArrayList<>();
        String[] ids={"soulsweapons:darkin_blade","soulsweapons:pure_moonlight_greatsword","soulsweapons:moonveil"};
        RPGClass[] classes={RPGClass.GUERREIRO,RPGClass.MAGO,RPGClass.ASSASSINO};
        for(int role=0;role<ids.length;role++)for(boolean nativeWeapon:new boolean[]{false,true}) {
            var p=TestPlayers.create(c);StatsManager.awaken(p);StatsManager.selectClass(p,classes[role].name());
            int xp=0;for(int n=1;n<45;n++)xp+=PlayerStats.xpToNext(n);StatsManager.addXp(p,xp);
            String stat=classes[role]==RPGClass.GUERREIRO?"FORCA":classes[role]==RPGClass.MAGO?"INTELIGENCIA":"DESTREZA";
            for(int n=0;n<45;n++)StatsManager.allocate(p,stat);
            // Netherite is a core greatsword: both compared builds must meet STR18/DEX7.
            while(StatsManager.get(p).stats.get(Stat.FORCA)<18)StatsManager.allocate(p,"FORCA");
            while(StatsManager.get(p).stats.get(Stat.DESTREZA)<7)StatsManager.allocate(p,"DESTREZA");
            for(var node:classes[role].nodes)StatsManager.unlockNode(p,node.id());
            ItemStack weapon=new ItemStack(nativeWeapon?Registries.ITEM.get(new Identifier(ids[role])):Items.NETHERITE_SWORD);
            p.setStackInHand(Hand.MAIN_HAND,weapon);p.playerTick();p.tick();p.setNoGravity(true);p.setHealth(p.getMaxHealth());
            // This measures outgoing DPS only. Adjacent GameTests must not kill its attackers.
            p.setInvulnerable(true);
            var stats=StatsManager.get(p);stats.stamina=stats.staminaMax;stats.resource=stats.resourceMax;StatsManager.save(p,stats);
            var coreProfile=com.rpgstats.integration.IntegrationServices.INSTANCE.weapon(weapon).orElse(null);
            c.assertTrue(com.rpgstats.integration.IntegrationServices.INSTANCE.meetsRequirements(StatsManager.get(p),coreProfile),"Core benchmark violates its own stat requirements");
            if(nativeWeapon)c.assertTrue(com.rpgstats.compat.bosses.BossEquipmentService.check(p,weapon).allowed(),"Offensive fixture cannot use its weapon");
            var target=EntityType.ZOMBIE.create(c.getWorld());target.setAiDisabled(true);target.setNoGravity(true);
            // Zero armor makes health loss linear when comparing different affinity multipliers.
            target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0);
            target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100000);target.setHealth(100000);
            // The six fixtures extend beyond the empty template: keep them above neighboring
            // structures instead of spawning players inside their walls or hostile mob arenas.
            var pos=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(2+players.size()*4,82,4));
            target.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+1,180,0);
            c.getWorld().spawnEntity(target);players.add(p);targets.add(target);initial.add(target.getHealth());
        }
        // Mock connections send no movement packets; run the same vanilla living-player tick
        // normally reached by that connection, rather than resetting attack cooldowns.
        for(int tick=1;tick<=1300;tick++)c.runAtTick(tick,()->players.forEach(ServerPlayerEntity::playerTick));
        for(int tick=100;tick<1300;tick+=13) {
            final int attackTick=tick;
            c.runAtTick(tick,()-> {
            for(int i=0;i<players.size();i++) {
                var p=players.get(i);p.setVelocity(net.minecraft.util.math.Vec3d.ZERO);p.setSprinting(false);
                c.assertTrue(p.isAlive() && !p.getMainHandStack().isEmpty(),"Outgoing benchmark lost its attacker or weapon");
                p.getRandom().setSeed(10000L+(i/2)*2000L+attackTick);
                if(attackTick==100)c.assertTrue(p.getAttackCooldownProgress(0)>.99f,"Mock connection did not advance vanilla attack cooldown");
                if(attackTick==100)RPGStatsMod.LOGGER.info("RPG_WEAPON_CONTEXT id={} health={} damage={} speed={} cooldown={} stamina={}",Registries.ITEM.getId(p.getMainHandStack().getItem()),p.getHealth(),p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE),p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED),p.getAttackCooldownProgress(0),StatsManager.get(p).stamina);
                p.attack(targets.get(i));
            }
            });
        }
        for(int seconds:new int[]{5,30,60})c.runAtTick(100+seconds*20,()-> {
            boolean pass=true;
            for(int role=0;role<ids.length;role++) {
                float core=initial.get(role*2)-targets.get(role*2).getHealth(), adapted=initial.get(role*2+1)-targets.get(role*2+1).getHealth();
                var corePlayer=players.get(role*2);var nativePlayer=players.get(role*2+1);
                float coreAffinity=com.rpgstats.compat.bosses.WeaponAffinity.damageFactor(StatsManager.get(corePlayer),corePlayer.getMainHandStack());
                float nativeAffinity=com.rpgstats.compat.bosses.WeaponAffinity.damageFactor(StatsManager.get(nativePlayer),nativePlayer.getMainHandStack());
                float intrinsicCore=core/coreAffinity,intrinsicNative=adapted/nativeAffinity;
                RPGStatsMod.LOGGER.info("RPG_NATIVE_WEAPON_DPS id={} class={} seconds={} coreDamage={} nativeDamage={} coreAffinity={} nativeAffinity={} intrinsicRatio={}",
                        ids[role],classes[role],seconds,core,adapted,coreAffinity,nativeAffinity,intrinsicNative/Math.max(.01f,intrinsicCore));
                pass &= core>0 && adapted>0 && intrinsicNative<=intrinsicCore*1.15f+.1f;
            }
            if(!pass || seconds==60){for(var target:targets)target.discard();TestPlayers.finish(c);}
            c.assertTrue(pass,"Native weapon exceeded core sustained output budget");if(seconds==60)c.complete();
        });
    }
    private NativeWeaponBudgetGameTests(){}
}
