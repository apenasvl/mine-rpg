package com.rpgstats.compat.bosses;
import com.rpgstats.integration.DataDrivenRegistry;
import com.rpgstats.stats.Stat;
import com.rpgstats.stats.StatsManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Map;

/** Server reads its own stats and snapshot; no client-supplied eligibility. */
public final class BossEquipmentService {
    public static EquipmentPolicy.Eligibility check(ServerPlayerEntity player, ItemStack stack) {
        var rules=DataDrivenRegistry.equipmentRules(stack).orElse(null);
        if(rules==null)return EquipmentPolicy.evaluate(1,null,Map.of(),null);
        var stats=StatsManager.get(player);
        if(stack.getItem() instanceof net.minecraft.item.ArmorItem
                && (stats.clazz==null || !rules.classes().contains(stats.clazz.name())))
            return new EquipmentPolicy.Eligibility(false,EquipmentPolicy.Reason.CLASS);
        var totals=stats.totalStats();
        return EquipmentPolicy.evaluate(stats.level,stats.clazz==null?null:stats.clazz.name(),Map.of(
                "str",totals.getOrDefault(Stat.FORCA,0),"dex",totals.getOrDefault(Stat.DESTREZA,0),
                "int",totals.getOrDefault(Stat.INTELIGENCIA,0),"faith",totals.getOrDefault(Stat.FE,0),
                "arc",totals.getOrDefault(Stat.ARCANO,0),"vig",totals.getOrDefault(Stat.VITALIDADE,0),
                "end",totals.getOrDefault(Stat.TENACIDADE,0)),rules);
    }
    public static boolean rejectWithMessage(ServerPlayerEntity player,ItemStack stack) {
        var result=check(player,stack);if(result.allowed())return false;
        var rules=DataDrivenRegistry.equipmentRules(stack).orElse(null);
        String message=result.reason()==EquipmentPolicy.Reason.DISABLED
                ?"Poder nativo desativado nesta integração; use as habilidades da sua classe."
                :result.reason()==EquipmentPolicy.Reason.CLASS
                    ?"Armadura requer classe "+String.join("/",rules.classes())+"."
                    :"Requer nível "+rules.minLevel()+" e atributos "+rules.stats()+".";
        player.sendMessage(net.minecraft.text.Text.literal(message).formatted(net.minecraft.util.Formatting.RED),true);
        return true;
    }
    private BossEquipmentService() {}
}
