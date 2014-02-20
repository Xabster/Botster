import java.util.*;

public class Exec {
    public static void sysout(Object object) {
        if (object instanceof Object[])
            System.out.println(Arrays.deepToString((Object[]) object));
        else if (object instanceof byte[])
            System.out.println(Arrays.toString((byte[]) object));
        else if (object instanceof short[])
            System.out.println(Arrays.toString((short[]) object));
        else if (object instanceof int[])
            System.out.println(Arrays.toString((int[]) object));
        else if (object instanceof long[])
            System.out.println(Arrays.toString((long[]) object));
        else if (object instanceof float[])
            System.out.println(Arrays.toString((float[]) object));
        else if (object instanceof double[])
            System.out.println(Arrays.toString((double[]) object));
        else if (object instanceof boolean[])
            System.out.println(Arrays.toString((boolean[]) object));
        else if (object instanceof char[]) {
            char[] chars = (char[]) object;
            String[] out = new String[chars.length];
            for (int i = 0; i < chars.length; i++) {
                if (Character.isISOControl(chars[i]))
                    out[i] = "\\" + (int) chars[i];
                else
                    out[i] = Character.toString(chars[i]);
            }
            System.out.println(Arrays.toString(out));
        } else
            System.out.println(object);
    }

    public static void main(String[] args) throws Throwable {
        sysout(1);
    }
}