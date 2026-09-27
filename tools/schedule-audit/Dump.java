import java.lang.reflect.*;
import java.util.*;
import java.io.*;
public class Dump {
  static Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
  static PrintWriter out;
  static Class<?> CS;
  public static void main(String[] a) throws Exception {
    out = new PrintWriter(new FileWriter(a[0]));
    CS = Class.forName("com.good4.schedule.domain.ClassSchedule");
    String[] objs = {"ClassSchedules","TourismSchedules","CommunicationSchedules","LawSchedules","SportsScienceSchedules","FineArtsSchedules","FisheriesSchedules","TheologySchedules","DentalSchedules","LiteratureSchedules","ScienceSchedules","AgricultureSchedules","AdditionalLiteratureSchedules"};
    for (String o : objs) {
      Class<?> c;
      try { c = Class.forName("com.good4.schedule.domain." + o); } catch (Throwable t) { System.err.println("skip " + o + " " + t); continue; }
      Object inst = c.getField("INSTANCE").get(null);
      for (Field f : c.getDeclaredFields()) {
        if (Modifier.isStatic(f.getModifiers()) && !f.getName().equals("INSTANCE")) { f.setAccessible(true); walk(o, f.get(null)); }
        else if (!Modifier.isStatic(f.getModifiers())) { f.setAccessible(true); walk(o, f.get(inst)); }
      }
      // Faculties that build their schedules on demand expose schedulesFor(department).
      if (a.length > 1) {
        for (Method m : c.getDeclaredMethods()) {
          if (m.getName().startsWith("schedulesFor") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
            m.setAccessible(true);
            for (String line : java.nio.file.Files.readAllLines(new File(a[1]).toPath())) {
              String dep = line.split("\t")[1];
              try { walk(o, m.invoke(inst, dep)); } catch (Throwable t) { System.err.println("schedulesFor failed " + o + " " + dep + ": " + t.getCause()); }
            }
          }
        }
      }
      // lazy getters
      for (Method m : c.getDeclaredMethods()) {
        if (m.getParameterCount()==0 && m.getName().startsWith("get")) { try { m.setAccessible(true); walk(o, m.invoke(inst)); } catch (Throwable t) {} }
      }
    }
    out.close();
  }
  static void walk(String src, Object v) throws Exception {
    if (v == null) return;
    if (CS.isInstance(v)) { if (seen.add(v)) emit(src, v); return; }
    if (v instanceof Collection) { for (Object x : (Collection<?>) v) walk(src, x); return; }
    if (v instanceof Map) { for (Object x : ((Map<?,?>) v).values()) walk(src, x); return; }
    if (v.getClass().getName().startsWith("kotlin.Lazy") || v.getClass().getName().contains("SynchronizedLazyImpl")) {
      try { Method m = v.getClass().getMethod("getValue"); m.setAccessible(true); walk(src, m.invoke(v)); } catch (Throwable t) {}
    }
  }
  static String g(Object o, String n) throws Exception { Method m = o.getClass().getMethod(n); Object r = m.invoke(o); return r == null ? "" : r.toString(); }
  static void emit(String src, Object s) throws Exception {
    out.println("## " + src + " | " + g(s,"getFaculty") + " | " + g(s,"getDepartment") + " | " + g(s,"getClassYear") + " | " + g(s,"getSourceUrl"));
    for (Object e : (List<?>) s.getClass().getMethod("getEntries").invoke(s)) {
      out.println(String.join("\t", g(e,"getDay"), g(e,"getStartTime"), g(e,"getEndTime"), g(e,"getCourseCode"), g(e,"getCourseName").replace("\n"," "), g(e,"getInstructor").replace("\n"," "), g(e,"getClassroom").replace("\n"," "), g(e,"getCourseType"), g(e,"getSection")));
    }
  }
}
