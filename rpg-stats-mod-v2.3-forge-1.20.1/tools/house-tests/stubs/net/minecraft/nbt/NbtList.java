package net.minecraft.nbt;
public class NbtList extends NbtElement {
    private final java.util.List<NbtElement> values=new java.util.ArrayList<>();
    public void add(NbtElement value){values.add(value);}
    public int size(){return values.size();}
    public String getString(int i){return ((NbtString)values.get(i)).value;}
}
