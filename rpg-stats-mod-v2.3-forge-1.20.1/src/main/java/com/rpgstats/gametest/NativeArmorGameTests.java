package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.*;
import com.rpgstats.stats.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
import java.util.*;

/** Audit original registered pieces, equipped attributes and individual native hits. */
@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class NativeArmorGameTests {
    private static final List<String> MODS=List.of("soulsweapons","bosses_of_mass_destruction","legendary_monsters","irons_spellbooks","better_weaponry");
    private record Gear(String id,Map<EquipmentSlot,Item> pieces) {}
    private static String family(Identifier id) {
        if(id.toString().equals("soulsweapons:chaos_robes"))return "soulsweapons:chaos";
        String name=id.getPath().replaceFirst("_(helmet|head|hood|hat|mask|chestplate|chest|robe|leggings|legs|pants|boots|feet)$","");
        name=name.replaceFirst("^(helmet|hood|hat|mask|chestplate|robe|leggings|pants|boots)_","");
        return id.getNamespace()+":"+name;
    }
    private static List<Gear> gear(TestContext c,boolean mage) {
        Map<String,Map<EquipmentSlot,Item>> groups=new TreeMap<>();
        for(var item:Registries.ITEM) {
            var id=Registries.ITEM.getId(item);
            if(!(item instanceof ArmorItem) || !MODS.contains(id.getNamespace()) || (mage!=id.getNamespace().equals("irons_spellbooks")))continue;
            if(id.toString().equals("irons_spellbooks:wizard_hat") || id.toString().equals("soulsweapons:chaos_crown"))continue;
            var slot=LivingEntity.getPreferredEquipmentSlot(new ItemStack(item));
            c.assertTrue(slot.getType()==EquipmentSlot.Type.ARMOR,"Native armor has no armor slot: "+id);
            var parts=groups.computeIfAbsent(family(id),ignored->new EnumMap<>(EquipmentSlot.class));
            c.assertTrue(parts.put(slot,item)==null,"Ambiguous native set grouping; inspect item identifiers: "+id);
        }
        for(var variant:List.of(new String[]{"irons_spellbooks:wizard","irons_spellbooks:wizard_hat"},new String[]{"soulsweapons:chaos","soulsweapons:chaos_crown"})) {
            var base=groups.get(variant[0]);var item=Registries.ITEM.get(new Identifier(variant[1]));
            if(base!=null && item instanceof ArmorItem) {
                var parts=new EnumMap<EquipmentSlot,Item>(EquipmentSlot.class);parts.putAll(base);parts.put(EquipmentSlot.HEAD,item);groups.put(variant[1]+"_variant",parts);
            }
        }
        return groups.entrySet().stream().map(e->new Gear(e.getKey(),e.getValue())).toList();
    }
    private static List<String> ids(Gear gear) {return gear.pieces.values().stream().map(i->Registries.ITEM.getId(i).toString()).sorted().toList();}
    private static void configure(ServerPlayerEntity p,RPGClass clazz,boolean juggernaut) {
        var s=new PlayerStats();s.awakened=true;s.level=50;s.clazz=clazz;
        s.stats.put(Stat.VITALIDADE,32);s.stats.put(Stat.TENACIDADE,25);
        s.stats.put(Stat.FORCA,clazz==RPGClass.MAGO?0:clazz==RPGClass.GUERREIRO?47:24);
        s.stats.put(Stat.DESTREZA,clazz==RPGClass.MAGO?0:clazz==RPGClass.GUERREIRO?24:47);
        s.stats.put(Stat.INTELIGENCIA,clazz==RPGClass.MAGO?50:0);
        for(var n:clazz.nodes)s.unlockedNodes.add(n.id());
        if(juggernaut || clazz==RPGClass.MAGO) {
            s.specialization=juggernaut?RPGSpecialization.JUGGERNAUT:RPGSpecialization.ACCELERATOR;s.path=s.specialization.parent;
            for(var n:s.path.nodes)s.unlockedNodes.add(n.id());for(var n:s.specialization.nodes)s.unlockedNodes.add(n.id());
        }
        s.refreshResourceMax();s.resource=s.resourceMax;s.stamina=s.staminaMax;StatsManager.finish(p,s);p.setNoGravity(true);
    }
    private static Map<String,Double> attributes(ServerPlayerEntity p) {
        Map<String,Double> out=new TreeMap<>();
        for(var attribute:Registries.ATTRIBUTE) {
            var instance=p.getAttributeInstance(attribute);
            if(instance!=null)out.put(Registries.ATTRIBUTE.getId(attribute).toString(),instance.getValue());
        }
        return out;
    }
    private static void stable(TestContext c,ServerPlayerEntity p,Gear gear) {
        var before=attributes(p);
        for(int n=0;n<3;n++)StatsApplier.apply(p);
        var after=attributes(p);
        c.assertTrue(before.keySet().equals(after.keySet()),"Native armor attributes disappeared after RPG refresh: "+gear.id);
        for(var e:before.entrySet())c.assertTrue(Double.isFinite(e.getValue()) && Math.abs(e.getValue()-after.get(e.getKey()))<.00001,"Native armor attribute stacked or changed: "+gear.id+" "+e.getKey());
        int protection=gear.pieces.values().stream().mapToInt(i->((ArmorItem)i).getProtection()).sum();
        c.assertTrue(protection==0 || p.getArmor()>0,"Equipped native armor protection was removed: "+gear.id);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativeArmorRegistryAndEquippedAttributesStayStable(TestContext c) {
        try {
            for(var namespace:MODS) {
                List<String> pieces=new ArrayList<>();for(var i:Registries.ITEM)if(i instanceof ArmorItem && Registries.ITEM.getId(i).getNamespace().equals(namespace))pieces.add(Registries.ITEM.getId(i).toString());Collections.sort(pieces);
                RPGStatsMod.LOGGER.info("RPG_ARMOR_INVENTORY {}",new com.google.gson.Gson().toJson(Map.of("mod",namespace,"loaded",ModList.get().isLoaded(namespace),"pieces",pieces)));
            }
            for(boolean mage:new boolean[]{false,true})for(var g:gear(c,mage)) {
                var p=TestPlayers.create(c);configure(p,mage?RPGClass.MAGO:RPGClass.GUERREIRO,false);
                for(var e:g.pieces.entrySet())p.equipStack(e.getKey(),new ItemStack(e.getValue()));p.playerTick();p.tick();com.rpgstats.compat.ClassArmorBonuses.apply(p);stable(c,p,g);
                RPGStatsMod.LOGGER.info("RPG_ARMOR_EQUIPPED {}",new com.google.gson.Gson().toJson(Map.of("set",g.id,"pieces",ids(g),"attributes",attributes(p))));
                TestPlayers.finish(c);
            }
        }finally{TestPlayers.finish(c);}c.complete();
    }
    private static void nativeHit(MobEntity boss,ServerPlayerEntity p,int phase)throws ReflectiveOperationException {
        if(phase==7)boss.refreshPositionAndAngles(p.getX(),p.getY(),p.getZ()+1,180,0);
        boss.setTarget(p);boss.getClass().getMethod("setSpawning",boolean.class).invoke(boss,false);
        boss.getClass().getMethod(phase==52?"setRupture":"setMaceOfSpades",boolean.class).invoke(boss,true);
        var type=Class.forName("net.soulsweaponry.entity.ai.goal.ReturningKnightGoal");
        var goal=(net.minecraft.entity.ai.goal.Goal)type.getConstructor(boss.getClass()).newInstance(boss);
        for(var value:new Object[][]{{"attackCooldown",100},{"specialCooldown",100},{"summonCooldown",100},{"attackStatus",phase-1},{"targetPos",p.getBlockPos()},{"cordsRegistered",true}}) {
            var f=type.getDeclaredField((String)value[0]);f.setAccessible(true);f.set(goal,value[1]);
        }
        if(phase==52) {
            p.refreshPositionAndAngles(boss.getX()+12,boss.getY(),boss.getZ(),0,0);
            var area=new Box(boss.getX()-18,boss.getY()-8,boss.getZ()-18,boss.getX()+18,boss.getY()+8,boss.getZ()+18);
            if(!boss.getWorld().getOtherEntities(boss,area).contains(p))throw new AssertionError("Player absent from native eruption entity query; hit cannot be measured");
        }
        var status=type.getDeclaredField("attackStatus");status.setAccessible(true);
        RPGStatsMod.LOGGER.debug("RPG_NATIVE_BRANCH phase={} target={} mace={} summon={} obliterate={} blind={} rupture={} preStatus={}",phase,boss.getTarget()==p,boss.getClass().getMethod("getMaceOfSpades").invoke(boss),boss.getClass().getMethod("getSummon").invoke(boss),boss.getClass().getMethod("getObliterate").invoke(boss),boss.getClass().getMethod("getBlind").invoke(boss),boss.getClass().getMethod("getRupture").invoke(boss),status.get(goal));
        goal.tick();
        RPGStatsMod.LOGGER.debug("RPG_NATIVE_BRANCH_END phase={} postStatus={} velocity={}",phase,status.get(goal),p.getVelocity());
    }
    private static void encounter(TestContext c,RPGClass clazz,boolean juggernaut,int phase) {encounter(c,clazz,juggernaut,phase,false);}
    private static void encounter(TestContext c,RPGClass clazz,boolean juggernaut,int phase,boolean allMageSpecs) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var sets=gear(c,clazz==RPGClass.MAGO);
        var mageBuilds=new ArrayList<RPGSpecialization>();
        if(allMageSpecs && !sets.isEmpty()) {
            var wizard=sets.stream().filter(g->g.id.equals("irons_spellbooks:wizard")).findFirst().orElseThrow();
            mageBuilds.add(null);for(var spec:RPGSpecialization.values())if(spec.parent.parent==RPGClass.MAGO)mageBuilds.add(spec);
            sets=new ArrayList<>(Collections.nCopies(mageBuilds.size(),wizard));
        }
        final var encounterSets=sets;
        if(sets.isEmpty()){c.assertTrue(clazz!=RPGClass.MAGO || !ModList.get().isLoaded("irons_spellbooks"),"Loaded Iron's has no native armor fixtures");c.complete();return;}
        var players=new ArrayList<ServerPlayerEntity>();var bosses=new ArrayList<MobEntity>();var forced=new HashSet<ChunkPos>();
        for(var g:sets) {
            var p=TestPlayers.create(c);configure(p,clazz,juggernaut);
            if(allMageSpecs) {
                var stats=StatsManager.get(p);var spec=mageBuilds.get(players.size());stats.specialization=spec;stats.path=spec==null?null:spec.parent;stats.unlockedNodes.clear();
                for(var n:RPGClass.MAGO.nodes)stats.unlockedNodes.add(n.id());
                if(spec!=null){for(var n:spec.parent.nodes)stats.unlockedNodes.add(n.id());for(var n:spec.nodes)stats.unlockedNodes.add(n.id());}
                StatsManager.finish(p,stats);
                // Measure mitigation without the separate once-per-cooldown fatal rescues.
                com.rpgstats.combat.CombatState.get(p.getUuid()).startCooldown("internal_phoenix",3600);
                com.rpgstats.combat.CombatState.get(p.getUuid()).startCooldown("internal_second_chance",2400);
            }
            for(var e:g.pieces.entrySet())p.equipStack(e.getKey(),new ItemStack(e.getValue()));p.playerTick();p.tick();com.rpgstats.compat.ClassArmorBonuses.apply(p);stable(c,p,g);
            var pos=c.getAbsolutePos(new BlockPos(131072+players.size()*64,3,49152+clazz.ordinal()*8192+phase*64+(juggernaut?4096:0)));var chunk=new ChunkPos(pos);
            if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
            var boss=(MobEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());boss.setAiDisabled(true);boss.setNoGravity(true);
            boss.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+1,180,0);
            p.refreshPositionAndAngles(phase==52?boss.getX()+12:pos.getX(),pos.getY(),phase==52?boss.getZ():pos.getZ(),0,0);
            var playerChunk=new ChunkPos(p.getBlockPos());
            if(!c.getWorld().getForcedChunks().contains(playerChunk.toLong())){forced.add(playerChunk);c.getWorld().setChunkForced(playerChunk.x,playerChunk.z,true);}
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)c.getWorld().setBlockState(p.getBlockPos().add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
            c.assertTrue(c.getWorld().spawnEntity(boss),"Native armor boss fixture did not spawn");players.add(p);bosses.add(boss);
        }
        for(int tick=1;tick<100;tick++)c.runAtTick(tick,()->players.forEach(p->{p.tick();p.playerTick();}));
        c.runAtTick(100,()->{
            try {
                for(int i=0;i<players.size();i++) {
                    var p=players.get(i);var g=encounterSets.get(i);stable(c,p,g);p.setHealth(p.getMaxHealth());float hp=p.getHealth();var attrs=attributes(p);
                    var area=phase==52?new Box(bosses.get(i).getX()-18,bosses.get(i).getY()-8,bosses.get(i).getZ()-18,bosses.get(i).getX()+18,bosses.get(i).getY()+8,bosses.get(i).getZ()+18):new Box(p.getBlockPos()).expand(phase==7?5:3);
                    RPGStatsMod.LOGGER.info("RPG_ARMOR_HIT_CONTEXT class={} set={} phase={} playerPos={} bossPos={} queried={} invulnerable={} hurtTime={}",clazz,g.id,phase,p.getPos(),bosses.get(i).getPos(),c.getWorld().getOtherEntities(bosses.get(i),area).contains(p),p.isInvulnerableTo(p.getDamageSources().mobAttack(bosses.get(i))),p.hurtTime);
                    var effects=p.getStatusEffects().stream().map(e->e.getEffectType().getTranslationKey()+":"+e.getAmplifier()).sorted().toList();
                    c.assertTrue(bosses.get(i).isAlive(),"Native set killed the boss before its attack: "+g.id);nativeHit(bosses.get(i),p,phase);
                    float loss=hp-p.getHealth();c.assertTrue(Float.isFinite(loss)&&loss>=0 && p.getArmor()>=0,"Native armor produced invalid resolved damage: "+g.id);
                    Map<String,Object> row=new LinkedHashMap<>();row.put("class",clazz.name());row.put("spec",allMageSpecs?(mageBuilds.get(i)==null?"BASE":mageBuilds.get(i).name()):juggernaut?"JUGGERNAUT":clazz==RPGClass.MAGO?"ACCELERATOR":"BASE");row.put("set",g.id);row.put("pieces",ids(g));row.put("phase",phase);row.put("armor",p.getArmor());row.put("toughness",p.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));row.put("hp",hp);row.put("loss",loss);row.put("remaining",p.getHealth());row.put("alive",p.isAlive());row.put("effects",effects);row.put("attributes",attrs);
                    RPGStatsMod.LOGGER.info("RPG_ARMOR_RESULT {}",new com.google.gson.Gson().toJson(row));
                    c.assertTrue(loss>0,"Native attack did not deal confirmed damage: "+clazz+" "+g.id+" phase="+phase);
                    if(clazz==RPGClass.MAGO && g.pieces.size()==4)c.assertTrue(p.isAlive(),"Complete native mage armor died to one reference boss hit: "+g.id+" phase="+phase);
                }
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            finally{for(var b:bosses){b.discard();BossScaler.untrack(b);}for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);}c.complete();
        });
    }
    @GameTest(templateName="empty",tickLimit=240)
    public static void warriorNativeArmorStrike(TestContext c){encounter(c,RPGClass.GUERREIRO,false,7);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void warriorNativeArmorLaunch(TestContext c){encounter(c,RPGClass.GUERREIRO,false,21);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void warriorNativeArmorEruption(TestContext c){encounter(c,RPGClass.GUERREIRO,false,52);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void juggernautNativeArmorStrike(TestContext c){encounter(c,RPGClass.GUERREIRO,true,7);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void juggernautNativeArmorLaunch(TestContext c){encounter(c,RPGClass.GUERREIRO,true,21);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void juggernautNativeArmorEruption(TestContext c){encounter(c,RPGClass.GUERREIRO,true,52);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void archerNativeArmorStrike(TestContext c){encounter(c,RPGClass.ARQUEIRO,false,7);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void archerNativeArmorLaunch(TestContext c){encounter(c,RPGClass.ARQUEIRO,false,21);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void archerNativeArmorEruption(TestContext c){encounter(c,RPGClass.ARQUEIRO,false,52);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void assassinNativeArmorStrike(TestContext c){encounter(c,RPGClass.ASSASSINO,false,7);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void assassinNativeArmorLaunch(TestContext c){encounter(c,RPGClass.ASSASSINO,false,21);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void assassinNativeArmorEruption(TestContext c){encounter(c,RPGClass.ASSASSINO,false,52);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void mageNativeArmorStrike(TestContext c){encounter(c,RPGClass.MAGO,false,7);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void mageNativeArmorLaunch(TestContext c){encounter(c,RPGClass.MAGO,false,21);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void mageNativeArmorEruption(TestContext c){encounter(c,RPGClass.MAGO,false,52);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void allMageSpecsNativeStrike(TestContext c){encounter(c,RPGClass.MAGO,false,7,true);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void allMageSpecsNativeLaunch(TestContext c){encounter(c,RPGClass.MAGO,false,21,true);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void allMageSpecsNativeEruption(TestContext c){encounter(c,RPGClass.MAGO,false,52,true);}
    private NativeArmorGameTests(){}
}
