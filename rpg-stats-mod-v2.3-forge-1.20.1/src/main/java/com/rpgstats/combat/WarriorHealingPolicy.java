package com.rpgstats.combat;
import java.util.ArrayDeque;

/** A rolling second shared by weapon sustain sources, independent of hit count. */
public final class WarriorHealingPolicy {
    public static float weaponScale(boolean offClass,boolean twoHanded) {return offClass?.5f:twoHanded?.7f:1f;}
    public static float thirstGain(float actualDamage,float scale) {
        return .35f*Math.min(1f,Math.max(0f,actualDamage)/8f)*scale;
    }
    public static final class Budget {
        private record Entry(long tick,float amount) {}
        private final ArrayDeque<Entry> entries=new ArrayDeque<>();
        public boolean expired(long tick) {prune(tick);return entries.isEmpty();}
        private void prune(long tick) {while(!entries.isEmpty()&&tick-entries.peekFirst().tick()>=20)entries.removeFirst();}
        public float take(float requested,float missing,float scale,long tick) {
            prune(tick);
            if(!Float.isFinite(requested)||!Float.isFinite(missing)||!Float.isFinite(scale)||requested<=0||missing<=0||scale<=0)return 0;
            float spent=0;for(var e:entries)spent+=e.amount();
            float accepted=Math.max(0,Math.min(Math.min(requested*scale,missing),1.5f-spent));
            if(accepted>0) {
                if(!entries.isEmpty()&&entries.peekLast().tick()==tick)acceptedEntry(tick,accepted+entries.removeLast().amount());
                else acceptedEntry(tick,accepted);
            }
            return accepted;
        }
        private void acceptedEntry(long tick,float amount){entries.addLast(new Entry(tick,amount));}
    }
    private WarriorHealingPolicy(){}
}
