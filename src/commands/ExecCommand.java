package commands;

import botster.AbstractPlugin;

import java.io.File;
import java.io.FileNotFoundException;
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
public class ExecCommand extends AbstractPlugin {

    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    public static final int MAX_LINE_LENGTH = 200;
    public static final long PROCESS_TIMEOUT = 5L;
    public static final int DEFAULT_ERRORCODE = 666;

    private final Set<String> execStart;
    private final Set<String> execEnd;
    public static final int MAX_LINES = 3;

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

    private static <T> T timedCall(Callable<T> c) throws InterruptedException, ExecutionException, TimeoutException {
        FutureTask<T> task = new FutureTask<>(c);
        THREAD_POOL.execute(task);
        return task.get(PROCESS_TIMEOUT, TimeUnit.SECONDS);
    }

    @Override
    public String getReply(String command, String message) {
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
    private String exec(String message, boolean isSysoutOnly) {
        StringBuilder ret = new StringBuilder();
        StringWriter compilerOutput = new StringWriter();

        String folder = "execCommand" + File.separator;
        String filename = "Exec.java";
        String file = folder + filename;

        String fixedMessage = fixSemicolons(message, isSysoutOnly);

        List<String> output = Collections.synchronizedList(new ArrayList<>());

        try (PrintWriter writer = new PrintWriter(file)) {
            writeFile(fixedMessage, isSysoutOnly, writer);
            int exitCode = compileAndRun(compilerOutput, folder, file, output);

            for (int i = 0; i < Math.min(MAX_LINES, output.size()); i++) {
                String curStr = output.get(i);
                ret.append(curStr.substring(0, Math.min(MAX_LINE_LENGTH, curStr.length())));
                ret.append("\r\n");
            }

            if (output.size() == 0)
                ret.append(String.format("Execution successful. No output. Exit code: %d", exitCode));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return ret.toString();
    }

    private int compileAndRun(StringWriter compilerOutput, String folder, String file, List<String> output) {
        int exitCode = DEFAULT_ERRORCODE;
        int error = com.sun.tools.javac.Main.compile(new String[]{file}, new PrintWriter(compilerOutput));
        if (error != 0)
            handleCompileError(compilerOutput, output);
        else
            exitCode = runCode(folder, output);
        return exitCode;
    }

    private String fixSemicolons(String message, boolean isSysoutOnly) {
        if (isSysoutOnly && message.charAt(message.length() - 1) == ';')
            message = message.substring(0, message.length() - 1);
        else if (!isSysoutOnly && message.charAt(message.length() - 1) != ';')
            message += ";";
        return message;
    }

    private void writeFile(String message, boolean isSysoutOnly, PrintWriter writer) {
        for (String start : execStart) {
            writer.println(start);
            System.out.println(start);
        }

        String sysoutWrapped = !isSysoutOnly ? message : "sysout(" + message + "\n);";

        writer.println(sysoutWrapped);
        System.out.println(sysoutWrapped);

        for (String end : execEnd) {
            writer.println(end);
            System.out.println(end);
        }
    }

    private int runCode(String folder, List<String> output) {
        int returnCode = DEFAULT_ERRORCODE;
        IntegerCallable intCall = new IntegerCallable(output);

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
        File[] files = new File(folder).listFiles();
        if (files != null)
            for (File f : files)
                if (f.getName().endsWith(".class") && !f.delete())
                    System.out.println("Could not delete file: " + f.getAbsolutePath());
    }

    private void handleCompileError(StringWriter compilerOutput, List<String> output) {
        String[] compOut = compilerOutput.toString().split("[\r\n]+");
        Collections.addAll(output, compOut);
    }

    private static class IntegerCallable implements Callable<Integer> {
        private final List<String> output;
        private Process process;

        public IntegerCallable(List<String> out) {
            this.output = out;
        }

        public Integer call() throws Exception {
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", "execCommand/", "-Djava.security.manager", "-Djava.security.policy=execCommand/exec.policy", "-Xmx64M", "Exec");
            pb.redirectErrorStream(true);
            process = pb.start();

            try (final Scanner scan = new Scanner(process.getInputStream())) {
                while (scan.hasNext())
                    output.add(scan.nextLine());
            }
            return 1;
        }

        private Process getProcess() {
            return process;
        }
    }
}