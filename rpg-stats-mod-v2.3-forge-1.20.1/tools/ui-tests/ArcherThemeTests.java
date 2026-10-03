import com.rpgstats.gui.ArcherTheme;
import com.rpgstats.gui.MageViewport;
import java.util.HashSet;

public class ArcherThemeTests {
    public static void main(String[] args) {
        int[] colors = {0xFFA5D3A2,0xFF82C69D,0xFF98D0B7,0xFF8BBFC2,0xFFBEC184};
        var cells = new HashSet<String>();
        int i=0;
        for (ArcherTheme theme : ArcherTheme.values()) {
            if(theme.color()!=colors[i++]) throw new AssertionError("House palette changed");
            if(theme.cell(null)!=0 || theme.cell("unknown")!=0) throw new AssertionError("Invalid spec must use house artwork");
            cells.add(theme.asset()+":0");
            for(String spec: theme.specializations()) {
                int cell=theme.cell(spec);
                if(cell<1 || cell>3 || !cells.add(theme.asset()+":"+cell)) throw new AssertionError("Specializations share artwork");
            }
        }
        if(cells.size()!=20) throw new AssertionError("Expected 5 houses + 15 specializations");
        if(ArcherTheme.forHouse(null)!=ArcherTheme.MARKSMAN) throw new AssertionError("Unassigned warrior fallback");
        for(int[] size:new int[][]{{960,540},{480,270},{427,240},{320,240},{854,360},{256,144}}) {
            var v=MageViewport.fit(size[0],size[1]);
            if(v.x()<0 || v.y()<0 || v.x()+820*v.scale()>size[0]+.01 || v.y()+470*v.scale()>size[1]+.01) throw new AssertionError("Clipped viewport");
            for(double x:new double[]{10,300,810}) if(Math.abs(v.localX(v.x()+x*v.scale())-x)>.001) throw new AssertionError("Mouse x misses visual");
            for(double y:new double[]{10,200,460}) if(Math.abs(v.localY(v.y()+y*v.scale())-y)>.001) throw new AssertionError("Mouse y misses visual");
        }
        System.out.println("PASS: five exact palettes, twenty unique art routes and inverse mouse transforms");
    }
}
