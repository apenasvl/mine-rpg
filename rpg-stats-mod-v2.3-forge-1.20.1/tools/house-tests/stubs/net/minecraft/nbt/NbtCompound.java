package net.minecraft.nbt;
/** In-memory NBT test double. Does not test Minecraft's binary serialization. */
public class NbtCompound extends NbtElement {
    private final java.util.Map<String,Object> values=new java.util.HashMap<>();
    public void put(String k,NbtElement v){values.put(k,v);}
    public void putInt(String k,int v){values.put(k,v);}
    public void putFloat(String k,float v){values.put(k,v);}
    public void putDouble(String k,double v){values.put(k,v);}
    public void putBoolean(String k,boolean v){values.put(k,v);}
    public void putString(String k,String v){values.put(k,v);}
    public int getInt(String k){return ((Number)values.getOrDefault(k,0)).intValue();}
    public float getFloat(String k){return ((Number)values.getOrDefault(k,0)).floatValue();}
    public double getDouble(String k){return ((Number)values.getOrDefault(k,0)).doubleValue();}
    public boolean getBoolean(String k){return (boolean)values.getOrDefault(k,false);}
    public String getString(String k){return (String)values.getOrDefault(k,"");}
    public boolean contains(String k){return values.containsKey(k);}
    public java.util.Set<String> getKeys(){return values.keySet();}
    public NbtCompound getCompound(String k){return (NbtCompound)values.getOrDefault(k,new NbtCompound());}
    public NbtList getList(String k,int ignored){return (NbtList)values.getOrDefault(k,new NbtList());}
}
