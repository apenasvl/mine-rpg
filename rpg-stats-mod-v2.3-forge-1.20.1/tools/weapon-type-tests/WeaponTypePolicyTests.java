package com.rpgstats.compat;
public final class WeaponTypePolicyTests {
 public static void main(String[] args) {
  check(WeaponTypePolicy.classify("trident",false)==WeaponTypePolicy.Kind.MELEE,"native one-handed spear parent lost");
  check(Boolean.FALSE.equals(WeaponTypePolicy.defaultTwoHanded("bettercombat:trident")),"trident parent must remain one-handed");
  check(WeaponTypePolicy.classify("soul_knife",false)==WeaponTypePolicy.Kind.RAPID,"native soul knife must be rapid");
  check(WeaponTypePolicy.classify("rapier",false)==WeaponTypePolicy.Kind.RAPID,"unique rapier must be rapid");
  check(WeaponTypePolicy.classify("claymore",true)==WeaponTypePolicy.Kind.TWO_HANDED,"explicit heavy metadata must be retained");
  check(WeaponTypePolicy.classify("longsword",false)==WeaponTypePolicy.Kind.MELEE,"ordinary sword category lost");
  check(WeaponTypePolicy.classify("unknown",false)==WeaponTypePolicy.Kind.UNKNOWN,"unknown native must fail closed");
  check(WeaponTypePolicy.classify("dagger",true)==WeaponTypePolicy.Kind.TWO_HANDED,"explicit two-handed takes precedence");
  check(Boolean.TRUE.equals(WeaponTypePolicy.defaultTwoHanded("bettercombat:claymore")),"absent BetterCombat must retain heavy type");
  check(WeaponTypePolicy.defaultTwoHanded("other:claymore")==null,"unapproved parent must not be guessed");
  check(WeaponTypePolicy.defaultTwoHanded("bettercombat:dagger")==false,"dagger is one-handed");
  System.out.println("WeaponTypePolicyTests: PASS");
 }
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
