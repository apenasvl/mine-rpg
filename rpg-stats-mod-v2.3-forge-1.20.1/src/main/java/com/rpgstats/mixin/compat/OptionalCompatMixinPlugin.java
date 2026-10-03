package com.rpgstats.mixin.compat;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
public final class OptionalCompatMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String p) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target,String mixin) {
        String mod=mixin.contains("Legendary") ? "legendary_monsters" : mixin.contains("Souls") ? "soulsweapons" : mixin.contains("CombatRoll") ? "combatroll" : "immersive_armors";
        String version=mod.equals("legendary_monsters") ? "1.20.1" : mod.equals("soulsweapons") ? "1.4.10-1.20.1-forge}" : mod.equals("combatroll") ? "1.3.3+1.20.1" : "1.7.2+1.20.1";
        var loading=FMLLoader.getLoadingModList();
        if (loading == null) return false;
        return loading.getMods().stream().anyMatch(m -> m.getModId().equals(mod) && m.getVersion().toString().equals(version));
    }
    public void acceptTargets(Set<String> a,Set<String> b) {}
    public List<String> getMixins() { return null; }
    public void preApply(String t,ClassNode c,String m,IMixinInfo i) {}
    public void postApply(String t,ClassNode c,String m,IMixinInfo i) {}
}
