package commands;

import Botster.IRCCommand;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.*;

/**
 * The ExecCommand class can be used to execute arbitrary pieces of Java code,
 * the result of which will be output in the channel.
 * <p>
 * Credits to Honk for originally making most of this ;)
 */
public class ExecCommand extends IRCCommand {

    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    private final Set<String> execStart;
    private final Set<String> execEnd;

    public ExecCommand() {
        addCommand("exec");
        addCommand("sysout");

        execStart = new LinkedHashSet<>();
        execStart.add("import java.util.*;");
        execStart.add("import java.util.concurrent.*;");
        execStart.add("import java.util.regex.*;");
        execStart.add("import java.util.zip.*;");
        execStart.add("import java.util.stream.*;");
        execStart.add("import java.util.function.*;");
        execStart.add("import java.lang.reflect.*;");
        execStart.add("import java.math.*;");
        execStart.add("import java.security.*;");
        execStart.add("import java.text.*;");
        execStart.add("import java.io.*;");
        execStart.add("import java.time.*;");
        execStart.add("public class Exec {");
        execStart.add("    public static void sysout(Object object) {");
        execStart.add("        if (object instanceof Object[]) {");
        execStart.add("            System.out.println(Arrays.deepToString((Object[])object));");
        execStart.add("        } else if (object instanceof byte[]) {");
        execStart.add("            System.out.println(Arrays.toString((byte[])object));");
        execStart.add("        } else if (object instanceof short[]) {");
        execStart.add("            System.out.println(Arrays.toString((short[])object));");
        execStart.add("        } else if (object instanceof int[]) {");
        execStart.add("            System.out.println(Arrays.toString((int[])object));");
        execStart.add("        } else if (object instanceof long[]) {");
        execStart.add("            System.out.println(Arrays.toString((long[])object));");
        execStart.add("        } else if (object instanceof float[]) {");
        execStart.add("            System.out.println(Arrays.toString((float[])object));");
        execStart.add("        } else if (object instanceof double[]) {");
        execStart.add("            System.out.println(Arrays.toString((double[])object));");
        execStart.add("        } else if (object instanceof char[]) {");
        execStart.add("            char[] chars = (char[])object;");
        execStart.add("            String[] out = new String[chars.length];");
        execStart.add("            for (int i=0;i<chars.length;i++) {");
        execStart.add("                if (Character.isISOControl(chars[i])) {");
        execStart.add("                    out[i] = \"\\\\\"+(int)chars[i];");
        execStart.add("                } else {");
        execStart.add("                    out[i] = Character.toString(chars[i]);");
        execStart.add("                }");
        execStart.add("            }");
        execStart.add("            System.out.println(Arrays.toString(out));");
        execStart.add("        } else if (object instanceof boolean[]) {");
        execStart.add("            System.out.println(Arrays.toString((boolean[])object));");
        execStart.add("        } else {");
        execStart.add("            System.out.println(object);");
        execStart.add("        }");
        execStart.add("    }");
        execStart.add("    public static void main(String[] args) throws Throwable {");

        execEnd = new LinkedHashSet<>();
        execEnd.add("    }");
        execEnd.add("}");
    }

    private static <T> T timedCall(final Callable<T> c) throws InterruptedException, ExecutionException, TimeoutException {
        final FutureTask<T> task = new FutureTask<>(c);
        THREAD_POOL.execute(task);
        return task.get(5L, TimeUnit.SECONDS);
    }

    @Override
    public String getReply(final String command, final String message) {
        return exec(message, command.equals("sysout"));
    }

    /**
     * Executes a piece of code and returns the result.
     *
     * @param message      the code to execute
     * @param isSysoutOnly if true, the message will be interpreted as something that
     *                     needs to be printed, rather than executed. Basically replaces
     *                     message with sysout(message);
     */
    private String exec(String message, final boolean isSysoutOnly) {
        final StringBuilder ret = new StringBuilder();
        final StringWriter compilerOutput = new StringWriter();

        final String folder = "execCommand" + File.separator;
        final String filename = "Exec.java";
        final String file = folder + filename;

        System.out.println(isSysoutOnly + " || " + message);

        if (isSysoutOnly && message.charAt(message.length() - 1) == ';')
            message = message.substring(0, message.length() - 1);
        else if (!isSysoutOnly && message.charAt(message.length() - 1) != ';')
            message = message + ";";

        final List<String> output = Collections.synchronizedList(new ArrayList<>());

        try (PrintWriter writer = new PrintWriter(file)) {
            for (final String start : execStart) {
                writer.println(start);
                System.out.println(start);
            }
            writer.println(!isSysoutOnly ? message : "sysout(" + message + "\n);");
            System.out.println(!isSysoutOnly ? message : "sysout(" + message + "\n);");

            for (final String end : execEnd) {
                writer.println(end);
                System.out.println(end);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
        int exitCode = 666;
        final int error = com.sun.tools.javac.Main.compile(new String[]{file}, new PrintWriter(compilerOutput));
        if (error != 0)
            handleCompileError(compilerOutput, output);
        else
            exitCode = runCode(folder, output);

        final int count = 3;
        for (int i = 0; i < Math.min(count, output.size()); i++) {
            ret.append(output.get(i).substring(0, Math.min(200, output.get(i).length())));
            ret.append("\r\n");
        }

        if (output.size() == 0) {
            ret.append("Execution successful. No output. Exit code: ");
            ret.append(exitCode);
        }

        return ret.toString();
    }

    private int runCode(final String folder, final List<String> output) {
        int returnCode = 666;
        final IntegerCallable intCall = new IntegerCallable(output);

        try {
            returnCode = timedCall(intCall);
        } catch (TimeoutException e) {
            // Handle timeout here
            output.add("Execution timed out");
            if (intCall.getProcess() != null)
                intCall.getProcess().destroy();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            output.add("Error executing: " + e.toString());
        } finally {
            deleteTempClassFiles(folder);
        }
        return returnCode;
    }

    private void deleteTempClassFiles(String folder) {
        final File[] files = new File(folder).listFiles();
        if (files != null)
            for (File f : files)
                if (f.getName().endsWith(".class") && !f.delete())
                    System.out.println("Could not delete file: " + f.getAbsolutePath());
    }

    private void handleCompileError(final StringWriter compilerOutput, final List<String> output) {
        final String[] compOut = compilerOutput.toString().split("[\r\n]+");
        Collections.addAll(output, compOut);
    }

    private static class IntegerCallable implements Callable<Integer> {
        private final List<String> output;
        private Process process;

        public IntegerCallable(final List<String> out) {
            this.output = out;
        }

        public Integer call() throws Exception {
            final ProcessBuilder pb = new ProcessBuilder("java", "-cp", "execCommand/", "-Djava.security.manager", "-Djava.security.policy=execCommand/exec.policy", "-Xmx64M", "Exec");
            pb.redirectErrorStream(true);
            process = pb.start();

            final Scanner scan = new Scanner(process.getInputStream());
            while (scan.hasNext())
                output.add(scan.nextLine());

            return 1;
        }

        private Process getProcess() {
            return process;
        }
    }
}