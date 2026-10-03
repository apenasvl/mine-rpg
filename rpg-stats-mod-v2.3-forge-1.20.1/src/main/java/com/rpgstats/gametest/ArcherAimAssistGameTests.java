package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.combat.ArcherAimAssist;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcherAimAssistGameTests {
    private static final AtomicInteger LAYERS = new AtomicInteger();
    // Loaded GameTest chunks, with 22-block vertical lanes: adjacent fixture targets lie far outside the 6-degree aim cone.
    private static ServerPlayerEntity player(TestContext c) {
        ServerPlayerEntity p = TestPlayers.create(c);
        p.refreshPositionAndAngles(p.getX(), 110 + LAYERS.getAndIncrement() * 22, p.getZ(), 0, 0);
        p.setNoGravity(true);
        // The client probe creates a normal terrain world, unlike the flat production
        // server. Give each fixture an explicit unobstructed test lane in both worlds.
        var base=p.getBlockPos();
        for(int x=-2;x<=2;x++) for(int y=0;y<=4;y++) for(int z=0;z<=18;z++)
            p.getServerWorld().setBlockState(base.add(x,y,z),net.minecraft.block.Blocks.AIR.getDefaultState());
        PlayerStats s = new PlayerStats(); s.awakened = true; s.clazz = RPGClass.ARQUEIRO;
        s.path = RPGPath.ARC_MARKSMAN; s.unlockedNodes.add("arc_mark_foundation");
        s.refreshResourceMax(); s.resource = s.resourceMax; StatsManager.save(p, s);
        PlayerStats loaded=StatsManager.get(p);
        c.assertTrue(loaded.awakened && loaded.clazz==RPGClass.ARQUEIRO && loaded.path==RPGPath.ARC_MARKSMAN
                && loaded.unlockedNodes.contains("arc_mark_foundation") && loaded.resource>=6,
                "Aim fixture persisted configuration is invalid: "+loaded.clazz+"/"+loaded.path+" nodes="+loaded.unlockedNodes+" Focus="+loaded.resource);
        c.assertTrue(p.getServer().getPlayerManager().getPlayer(p.getUuid())==p && p.isAlive(), "Aim fixture player is not registered/alive");
        return p;
    }
    private static ZombieEntity target(TestContext c, ServerPlayerEntity p) {
        ZombieEntity z = EntityType.ZOMBIE.create(p.getWorld());
        z.setAiDisabled(true); z.setNoGravity(true);
        z.refreshPositionAndAngles(p.getX() + .9, p.getEyeY() - z.getHeight() * .5, p.getZ() + 16, 0, 0);
        c.assertTrue(p.getServerWorld().spawnEntity(z), "Aim fixture target failed to spawn");
        c.assertTrue(p.getServerWorld().getEntitiesByClass(ZombieEntity.class, p.getBoundingBox().expand(28), e -> e == z).contains(z),
                "Aim fixture target is hidden from acquisition query; its chunk must be loaded");
        Vec3d delta=z.getBoundingBox().getCenter().subtract(p.getEyePos());
        c.assertTrue(delta.length()>=8 && delta.length()<=28, "Aim target range invalid: "+delta.length());
        c.assertTrue(Vec3d.fromPolar(p.getPitch(),p.getYaw()).normalize().dotProduct(delta.normalize())>=Math.cos(Math.toRadians(6)),
                "Aim target outside PLAYER cone: look="+Vec3d.fromPolar(p.getPitch(),p.getYaw())+" delta="+delta+" yaw="+p.getYaw()+" pitch="+p.getPitch());
        c.assertTrue(p.canSee(z), "Aim fixture player cannot see target: eye="+p.getEyePos()+" target="+z.getEyePos());
        c.assertTrue(z.isAlive() && !p.isTeammate(z), "Aim target is dead/allied");
        return z;
    }
    private static ArrowEntity arrow(TestContext c, ServerPlayerEntity p, boolean full) {
        ArrowEntity a = new ArrowEntity(p.getWorld(), p);
        a.setPosition(p.getEyePos()); a.setVelocity(0, 0, 3); a.setCritical(full);
        c.assertTrue(p.getServerWorld().spawnEntity(a), "Aim fixture arrow failed to spawn");
        c.assertTrue(a.getOwner()==p && a.getWorld()==p.getWorld() && a.isAlive() && !a.isRemoved() && a.isCritical()==full,
                "Aim projectile fixture invalid: owner="+a.getOwner()+" alive="+a.isAlive()+" removed="+a.isRemoved()+" critical="+a.isCritical());
        return a;
    }
    private static void lock(TestContext c,ServerPlayerEntity p,ArrowEntity a,ZombieEntity z) {
        ItemStack bow=new ItemStack(Items.BOW);
        c.assertTrue(com.rpgstats.combat.ArcherShotTracker.isRangedWeapon(bow) && !com.rpgstats.combat.ArcherShotTracker.isCrossbow(bow), "Fixture bow classification invalid");
        c.assertTrue(!bow.isIn(net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.ITEM,new net.minecraft.util.Identifier("rpgstats","weapons/native_area")))
                && !a.getType().isIn(net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.ENTITY_TYPE,new net.minecraft.util.Identifier("rpgstats","native_area_projectiles"))), "Fixture bow/arrow area tags excluded guidance");
        Vec3d delta=z.getBoundingBox().getCenter().subtract(a.getPos());
        c.assertTrue(a.getVelocity().normalize().dotProduct(delta.normalize())>=Math.cos(Math.toRadians(6)), "Target outside PROJECTILE cone: velocity="+a.getVelocity()+" delta="+delta);
        var hit=p.getServerWorld().raycast(new net.minecraft.world.RaycastContext(a.getPos(),z.getBoundingBox().getCenter(),net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,a));
        c.assertTrue(hit.getType()==net.minecraft.util.hit.HitResult.Type.MISS,"Fixture arrow ray blocked: hit="+hit.getPos()+" arrow="+a.getPos()+" target="+z.getBoundingBox().getCenter());
        float before=StatsManager.get(p).resource;
        ArcherAimAssist.onLaunch(p,a,bow);
        c.assertTrue(StatsManager.get(p).resource==before-6,"Valid onLaunch failed to LOCK/spend: Focus before="+before+" after="+StatsManager.get(p).resource);
    }
    private static void finish(TestContext c, net.minecraft.entity.Entity... entities) {
        for (var e : entities) e.discard(); TestPlayers.finish(c); c.complete();
    }
    @GameTest(templateName="empty", tickLimit=30)
    public static void shotUsesCurrentYawInsteadOfVisualHeadRotation(TestContext c) {
        ServerPlayerEntity p=player(c); p.setHeadYaw(90);
        ZombieEntity z=target(c,p); ArrowEntity a=arrow(c,p,true);
        lock(c,p,a,z); ArcherAimAssist.tick(p.getServer());
        c.assertTrue(a.getVelocity().x>0,"Current shot aim was replaced by visual head rotation");
        finish(c,z,a);
    }
    @GameTest(templateName="empty", tickLimit=30)
    public static void actualArrowTrajectoryImprovesWithoutSpeedOrDamageBonus(TestContext c) {
        ServerPlayerEntity p = player(c); ZombieEntity z = target(c,p);
        ArrowEntity guided = arrow(c,p,true), plain = arrow(c,p,true);
        float focus = StatsManager.get(p).resource; double damage = guided.getDamage();
        lock(c,p,guided,z);
        ArcherAimAssist.tick(p.getServer());
        Vec3d v = guided.getVelocity();
        c.assertTrue(v.x > 0 && Math.abs(v.length()-3) < 1e-8, "Assistance failed to turn or changed speed");
        c.assertTrue(Math.toDegrees(Math.acos(v.z/v.length())) <= 1.50001, "Correction exceeded 1.5 degrees");
        c.assertTrue(guided.getDamage()==damage && !guided.hasNoGravity(), "Assist modified native damage/gravity");
        c.assertTrue(StatsManager.get(p).resource==focus-6, "Valid lock did not spend exactly six Focus");
        c.runAtTick(3, () -> {
            c.assertTrue(guided.getX() > plain.getX() + .10, "Real launched arrow trajectory did not approach locked target");
            finish(c,z,guided,plain);
        });
    }
    @GameTest(templateName="empty", tickLimit=30)
    public static void noTargetOrPartialDrawDoesNotSpendFocus(TestContext c) {
        ServerPlayerEntity p = player(c); float focus=StatsManager.get(p).resource;
        ArrowEntity noTarget=arrow(c,p,true); ArcherAimAssist.onLaunch(p,noTarget,new ItemStack(Items.BOW));
        ZombieEntity z=target(c,p); ArrowEntity partial=arrow(c,p,false);
        ArcherAimAssist.onLaunch(p,partial,new ItemStack(Items.BOW)); ArcherAimAssist.tick(p.getServer());
        c.assertTrue(StatsManager.get(p).resource==focus, "Invalid or partial shot spent Focus");
        c.assertTrue(noTarget.getVelocity().x==0 && partial.getVelocity().x==0, "Invalid shot received guidance");
        finish(c,z,noTarget,partial);
    }
    @GameTest(templateName="empty", tickLimit=30)
    public static void onlyPrimaryMarksmanWithEnoughFocusCanLock(TestContext c) {
        ServerPlayerEntity p=player(c); ZombieEntity z=target(c,p);
        for (int kind=0;kind<4;kind++) {
            PlayerStats s=StatsManager.get(p); s.clazz=kind==0?RPGClass.GUERREIRO:RPGClass.ARQUEIRO;
            s.path=kind==1?RPGPath.ARC_WARDEN:RPGPath.ARC_MARKSMAN;
            s.resource=kind==2?5:30; if(kind==3) s.unlockedNodes.remove("arc_mark_foundation");
            StatsManager.save(p,s); float before=s.resource; ArrowEntity a=arrow(c,p,true);
            ArcherAimAssist.onLaunch(p,a,new ItemStack(Items.BOW)); ArcherAimAssist.tick(p.getServer());
            c.assertTrue(StatsManager.get(p).resource==before && a.getVelocity().x==0, "Invalid class/path/Focus/foundation received assist"); a.discard();
        }
        finish(c,z);
    }
    @GameTest(templateName="empty", tickLimit=30)
    public static void volleyPaysOnceAndOtherArrowRemainsNative(TestContext c) {
        ServerPlayerEntity p=player(c); ZombieEntity z=target(c,p); float before=StatsManager.get(p).resource;
        ArrowEntity first=arrow(c,p,true),second=arrow(c,p,true);
        lock(c,p,first,z); ArcherAimAssist.onLaunch(p,second,new ItemStack(Items.BOW));
        ArcherAimAssist.tick(p.getServer());
        c.assertTrue(StatsManager.get(p).resource==before-6 && first.getVelocity().x>0 && second.getVelocity().x==0,
                "Multishot received repeated spending or guidance"); finish(c,z,first,second);
    }
    @GameTest(templateName="empty", tickLimit=30)
    public static void wallBlocksAcquisitionWithoutCharging(TestContext c) {
        ServerPlayerEntity p=player(c); ZombieEntity z=target(c,p); float before=StatsManager.get(p).resource;
        var wall=net.minecraft.util.math.BlockPos.ofFloored(p.getX(),p.getEyeY(),p.getZ()+4);
        p.getServerWorld().setBlockState(wall,net.minecraft.block.Blocks.STONE.getDefaultState());
        ArrowEntity a=arrow(c,p,true); ArcherAimAssist.onLaunch(p,a,new ItemStack(Items.BOW)); ArcherAimAssist.tick(p.getServer());
        c.assertTrue(StatsManager.get(p).resource==before && a.getVelocity().x==0, "Wall-blocked lock spent Focus or guided");
        p.getServerWorld().removeBlock(wall,false);
        ArrowEntity clear=arrow(c,p,true); lock(c,p,clear,z); ArcherAimAssist.tick(p.getServer());
        c.assertTrue(clear.getVelocity().x>0 && StatsManager.get(p).resource==before-6, "Unblocked control failed to acquire target");
        finish(c,z,a,clear);
    }
    @GameTest(templateName="empty", tickLimit=30)
    public static void crossbowAndOutsideConeRemainNative(TestContext c) {
        ServerPlayerEntity p=player(c); ZombieEntity z=target(c,p); float before=StatsManager.get(p).resource;
        ArrowEntity crossbow=arrow(c,p,true);
        ArcherAimAssist.onLaunch(p,crossbow,new ItemStack(Items.CROSSBOW));
        z.setPosition(p.getX()+3,p.getEyeY()-z.getHeight()*.5,p.getZ()+16);
        ArrowEntity wide=arrow(c,p,true); ArcherAimAssist.onLaunch(p,wide,new ItemStack(Items.BOW));
        ArcherAimAssist.tick(p.getServer());
        c.assertTrue(StatsManager.get(p).resource==before && crossbow.getVelocity().x==0 && wide.getVelocity().x==0,
                "Crossbow or target beyond acquisition cone received guidance");
        z.setPosition(p.getX()+.9,p.getEyeY()-z.getHeight()*.5,p.getZ()+16);
        ArrowEntity valid=arrow(c,p,true); lock(c,p,valid,z); ArcherAimAssist.tick(p.getServer());
        c.assertTrue(valid.getVelocity().x>0 && StatsManager.get(p).resource==before-6, "In-cone bow control failed to acquire target");
        finish(c,z,crossbow,wide,valid);
    }
    @GameTest(templateName="empty", tickLimit=30)
    public static void obstructionEndsLockAndNeverRetargets(TestContext c) {
        ServerPlayerEntity p=player(c); ZombieEntity z=target(c,p); ArrowEntity a=arrow(c,p,true);
        a.setVelocity(0,0,.5); a.setNoGravity(true);
        lock(c,p,a,z); ArcherAimAssist.tick(p.getServer());
        c.assertTrue(a.getVelocity().x>0,"Fixture did not initially acquire target");
        c.runAtTick(2, () -> {
            var wall=net.minecraft.util.math.BlockPos.ofFloored(p.getX(),p.getEyeY(),p.getZ()+4);
            p.getServerWorld().setBlockState(wall,net.minecraft.block.Blocks.STONE.getDefaultState());
            Vec3d before=a.getVelocity(); ArcherAimAssist.tick(p.getServer());
            c.assertTrue(a.getVelocity().equals(before),"Guidance continued through new obstruction");
            p.getServerWorld().removeBlock(wall,false); z.discard(); ZombieEntity replacement=target(c,p);
            replacement.setPosition(p.getX()-.9,p.getEyeY()-replacement.getHeight()*.5,p.getZ()+16);
            double slope=a.getVelocity().x/a.getVelocity().z;
            c.runAtTick(4, () -> {
                c.assertTrue(Math.abs(a.getVelocity().x/a.getVelocity().z-slope)<1e-8,"Terminated guidance retargeted another enemy");
                finish(c,replacement,a);
            });
        });
    }
    @GameTest(templateName="empty", tickLimit=30)
    public static void correctionExpiresAfterEightTicks(TestContext c) {
        ServerPlayerEntity p=player(c); ZombieEntity z=target(c,p); ArrowEntity a=arrow(c,p,true);
        a.setVelocity(0,0,.5); a.setNoGravity(true);
        float before=StatsManager.get(p).resource;
        lock(c,p,a,z); ArcherAimAssist.tick(p.getServer());
        c.assertTrue(a.getVelocity().x>0 && StatsManager.get(p).resource==before-6, "Expiry fixture did not acquire target");
        c.runAtTick(9, () -> {
            double slope=a.getVelocity().x/a.getVelocity().z;
            z.setPosition(p.getX()-.9,p.getEyeY()-z.getHeight()*.5,p.getZ()+16);
            c.runAtTick(12, () -> {
                c.assertTrue(Math.abs(a.getVelocity().x/a.getVelocity().z-slope)<1e-8,"Guidance continued beyond eight ticks");
                finish(c,z,a);
            });
        });
    }

}
