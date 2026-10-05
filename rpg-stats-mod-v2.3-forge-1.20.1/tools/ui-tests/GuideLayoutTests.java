import com.rpgstats.gui.MageViewport;
public class GuideLayoutTests {
 public static void main(String[] args)throws Exception {
  Class<?> layout;
  try {layout=Class.forName("com.rpgstats.gui.GuideLayout");}
  catch(ClassNotFoundException e){throw new AssertionError("Guide must provide bounded readable content and scaled navigation",e);}
  var limit=layout.getMethod("scrollLimit",int.class);
  if((int)limit.invoke(null,950)!=540||(int)limit.invoke(null,200)!=0)throw new AssertionError("Long guide content must remain reachable");
  for(int[] size:new int[][]{{320,180},{512,384},{960,540},{1280,720},{2560,1080}}){
   var v=MageViewport.fit(size[0],size[1]);
   if(Math.abs(v.localX(v.x()+742*v.scale())-742)>.00001||Math.abs(v.localY(v.y()+424*v.scale())-424)>.00001)
    throw new AssertionError("Visible guide navigation and hitboxes must share coordinates");
   if(v.y()+452*v.scale()>size[1])throw new AssertionError("Guide footer clipped");
  }
  System.out.println("PASS: guide content limits and scaled navigation geometry");
 }
}
