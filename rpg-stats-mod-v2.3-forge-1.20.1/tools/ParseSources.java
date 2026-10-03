import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
public class ParseSources {
 public static void main(String[] args)throws Exception {
  var compiler=ToolProvider.getSystemJavaCompiler();
  var diagnostics=new DiagnosticCollector<JavaFileObject>();
  try(var fm=compiler.getStandardFileManager(diagnostics,null,null);var walk=Files.walk(Path.of(args[0]))) {
   var files=walk.filter(p->p.toString().endsWith(".java")).map(Path::toFile).toList();
   var task=(JavacTask)compiler.getTask(null,fm,diagnostics,java.util.List.of("-proc:none"),null,fm.getJavaFileObjectsFromFiles(files));
   task.parse();
   for(var d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)throw new AssertionError(d.toString());
   System.out.println("PASS: syntax parsed for "+files.size()+" Java files; Minecraft types NOT checked.");
  }
 }
}
