package com.rpgstats.gui;
/** Distinct identities for usable skills; passives have no combat HUD entry. */
public final class ArcaneHudText {
 public record Entry(String code,String name,String hint) {}
 public static Entry entry(String nodeId) {
  String id=nodeId==null?"":nodeId;
  if(id.startsWith("sec_"))id=id.substring(4);
  return switch(id){
   case "arc_core_utility" -> new Entry("TP","Tiro Preparado","Próxima flecha: +8% de dano; atire em até 4,75s.");
   case "arc_magic_technique" -> new Entry("AF","Alternar Afinidade","Troca Fogo → Gelo → Tempestade; não dispara uma flecha.");
   case "arc_magic_signature" -> new Entry("RE","Reacao Elemental","+3,5% de dano por 6,75s; mantém a afinidade.");
   case "arc_magic_fire_technique" -> new Entry("B1","Revestir com Brasas","Próximo tiro em 5s: marca por 6s e incendeia por 2s.");
   case "arc_magic_fire_signature" -> new Entry("B2","Consumir Brasas","Atire no alvo marcado em 6s: +4 de dano e fogo em até 2.");
   case "arc_magic_fire_ascension" -> new Entry("B3","Ciclo de Combustao","9s: marcar/consumir; até 3 consumos e propagação de fogo.");
   case "arc_magic_frost_technique" -> new Entry("G1","Revestir com Geada","Próximo tiro em 5s: marca por 6s e Lentidão por 2s.");
   case "arc_magic_frost_signature" -> new Entry("G2","Estilhacar Geada","Atire no alvo marcado em 6s: +4 de dano e Lentidão.");
   case "arc_magic_frost_ascension" -> new Entry("G3","Ciclo Glacial","9s: marcar/consumir; até 3 consumos e controle de área.");
   case "arc_magic_storm_technique" -> new Entry("T1","Carregar Condutor","Próximo tiro em 5s: marca um condutor por 6s.");
   case "arc_magic_storm_signature" -> new Entry("T2","Descarga Encadeada","Atire no condutor em 6s: +4 e corrente em até 2 vizinhos.");
   case "arc_magic_storm_ascension" -> new Entry("T3","Ciclo de Relampagos","9s: marcar/consumir; até 3 consumos e correntes limitadas.");
   default -> null;
  };
 }
 public static String affinity(int mode) {return switch(Math.floorMod(mode,3)){case 1->"Gelo";case 2->"Tempestade";default->"Fogo";};}
 private ArcaneHudText(){}
}
