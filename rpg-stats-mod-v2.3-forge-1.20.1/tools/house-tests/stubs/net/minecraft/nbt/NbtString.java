package net.minecraft.nbt;
public class NbtString extends NbtElement {
    public final String value;
    private NbtString(String value){this.value=value;}
    public static NbtString of(String value){return new NbtString(value);}
}
