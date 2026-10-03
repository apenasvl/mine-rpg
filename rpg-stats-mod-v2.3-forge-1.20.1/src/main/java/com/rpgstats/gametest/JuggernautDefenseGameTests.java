package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.*;
import com.rpgstats.stats.*;
import com.rpgstats.boss.BossScaler;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;

/** Regression against a mixed offensive/defensive build, without Protection IV. */
@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class JuggernautDefenseGameTests {
    @GameTest(templateName="empty",tickLimit=160)
    public static void juggernautSurvivesNativeKnightLaunchWithMixedBuild(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var p=TestPlayers.create(c);p.setNoGravity(true);
        var stats=new PlayerStats();stats.awakened=true;stats.level=50;stats.clazz=RPGClass.GUERREIRO;
        stats.path=RPGPath.WAR_VANGUARD;stats.specialization=RPGSpecialization.JUGGERNAUT;
        stats.stats.put(Stat.VITALIDADE,32);stats.stats.put(Stat.TENACIDADE,25);
        stats.stats.put(Stat.FORCA,47);stats.stats.put(Stat.DESTREZA,24);
        for(var n:RPGClass.GUERREIRO.nodes)stats.unlockedNodes.add(n.id());
        for(var n:RPGPath.WAR_VANGUARD.nodes)stats.unlockedNodes.add(n.id());
        for(var n:RPGSpecialization.JUGGERNAUT.nodes)stats.unlockedNodes.add(n.id());
        stats.refreshResourceMax();stats.resource=stats.resourceMax;stats.stamina=stats.staminaMax;
        StatsManager.finish(p,stats);
        // Represent armor attributes directly so the regression does not depend on a specific armor mod.
        p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(11);
        p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).setBaseValue(0);
        p.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(.5);
        var pos=c.getAbsolutePos(new BlockPos(28672,3,14336));var chunk=new ChunkPos(pos);
        boolean unforce=!c.getWorld().getForcedChunks().contains(chunk.toLong());
        if(unforce)c.getWorld().setChunkForced(chunk.x,chunk.z,true);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
        var boss=(MobEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());
        boss.setAiDisabled(true);boss.setNoGravity(true);
        boss.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+1,180,0);
        p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);c.getWorld().spawnEntity(boss);
        c.runAtTick(100,()->{
            try {
                p.setHealth(p.getMaxHealth());float before=p.getHealth();
                BossPressureGameTests.commonHit(boss,p,true);
                float loss=before-p.getHealth();
                RPGStatsMod.LOGGER.info("RPG_JUGGERNAUT_REFERENCE nativeLaunch=true level=50 vitality=32 tenacity=25 strength=47 dexterity=24 armor={} toughness={} maxHP={} loss={} remaining={} velocity={}",p.getArmor(),p.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS),before,loss,p.getHealth(),p.getVelocity());
                c.assertTrue(loss>0,"Native launch attack did not deal confirmed damage");
                c.assertTrue(p.isAlive()&&loss<=before*.65f,"Mixed Juggernaut build cannot survive native launch with recovery room");
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            finally {boss.discard();BossScaler.untrack(boss);if(unforce)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);}
            c.complete();
        });
    }
    private JuggernautDefenseGameTests(){}
}
