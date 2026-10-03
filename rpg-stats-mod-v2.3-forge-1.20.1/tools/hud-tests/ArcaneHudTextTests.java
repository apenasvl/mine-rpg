import com.rpgstats.gui.ArcaneHudText;
import java.util.HashSet;
public final class ArcaneHudTextTests {
 public static void main(String[] args){
  var codes=new HashSet<String>();var names=new HashSet<String>();
  for(String id:new String[]{"arc_core_utility","arc_magic_technique","arc_magic_signature","arc_magic_fire_technique","arc_magic_fire_signature","arc_magic_fire_ascension","arc_magic_frost_technique","arc_magic_frost_signature","arc_magic_frost_ascension","arc_magic_storm_technique","arc_magic_storm_signature","arc_magic_storm_ascension"}){
   var text=ArcaneHudText.entry(id);if(text==null||text.hint().isBlank()||!codes.add(text.code())||!names.add(text.name()))throw new AssertionError("Ambiguous active: "+id);
   if(text.code().length()>2||text.hint().length()>65)throw new AssertionError("HUD text exceeds compact bounds: "+id);
   if(!text.equals(ArcaneHudText.entry("sec_"+id)))throw new AssertionError("Borrowed node lost its identity");
  }
  if(ArcaneHudText.entry("arc_magic_foundation")!=null)throw new AssertionError("Passive advertised as usable");
  if(!ArcaneHudText.affinity(0).equals("Fogo")||!ArcaneHudText.affinity(1).equals("Gelo")||!ArcaneHudText.affinity(2).equals("Tempestade"))throw new AssertionError("Wrong affinity");
  System.out.println("Arcane HUD: 12 unique active identities, hints and affinity labels PASS");
 }
}
