import java.io.*; import java.util.*; import java.lang.reflect.*;
public class Combos { public static void main(String[] a) throws Exception {
  Class<?> c=Class.forName("com.good4.schedule.domain.ClassSchedules"); Object inst=c.getField("INSTANCE").get(null);
  Method find=c.getMethod("find", String.class, String.class, String.class);
  String[] years={"1. Sınıf","2. Sınıf","3. Sınıf","4. Sınıf","5. Sınıf"};
  int ok=0, missing=0, crash=0;
  for (String line: java.nio.file.Files.readAllLines(new File(a[0]).toPath())) {
    String[] p=line.split("\t"); 
    for (String y: years) {
      try { Object r=find.invoke(inst,p[0],p[1],y); if (r==null) { if(!y.equals("5. Sınıf")) {missing++; System.out.println("PROGRAM YOK: "+p[0]+" / "+p[1]+" / "+y);} } else ok++; }
      catch (InvocationTargetException e) { crash++; System.out.println("ÇÖKME: "+p[0]+" / "+p[1]+" / "+y+" -> "+e.getCause()); }
    }
  }
  System.out.println("ok="+ok+" eksik="+missing+" çökme="+crash);
}}
