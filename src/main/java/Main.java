import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static String findExecutable(String command) {
        String pathEnv = System.getenv("PATH");

        if (pathEnv == null) {
            return null;
        }

        String[] directories = pathEnv.split(File.pathSeparator);

        for (String dir : directories) {
            File file = new File(dir, command);

            if (file.exists() && file.isFile() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }

    private static String[] parseCommand(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // Backslashes inside double quotes
            if (inDoubleQuotes && c == '\\') {
                if (i + 1 < input.length()) {
                    char next = input.charAt(i + 1);

                    if (next == '"' || next == '\\') {
                        current.append(next);
                        i++;
                    } else {
                        current.append('\\');
                        current.append(next);
                        i++;
                    }
                } else {
                    current.append('\\');
                }
                continue;
            }

            // Backslashes outside quotes
            if (!inSingleQuotes && !inDoubleQuotes && c == '\\') {
                if (i + 1 < input.length()) {
                    current.append(input.charAt(i + 1));
                    i++;
                }
                continue;
            }

            if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                continue;
            }

            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }

            if (Character.isWhitespace(c)
                    && !inSingleQuotes
                    && !inDoubleQuotes) {

                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args.toArray(new String[0]);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        File currentDirectory = new File(System.getProperty("user.dir"));

        while (true) {
            System.out.print("$ ");

            String input = scanner.nextLine();

            if (input.isEmpty()) {
                continue;
            }

            String[] parts = parseCommand(input);

            // Handle stdout redirection (> and 1>)
            String outputFile = null;
            List<String> commandParts = new ArrayList<>();

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals(">") || parts[i].equals("1>")) {
                    if (i + 1 < parts.length) {
                        outputFile = parts[i + 1];
                    }
                    break;
                }
                commandParts.add(parts[i]);
            }

            parts = commandParts.toArray(new String[0]);

            if (parts.length == 0) {
                continue;
            }

            // exit builtin
            if (parts[0].equals("exit")) {
                break;
            }

            // pwd builtin
            if (parts[0].equals("pwd")) {
                String output = currentDirectory.getCanonicalPath();

                if (outputFile != null) {
                    try (PrintStream ps = new PrintStream(new FileOutputStream(outputFile))) {
                        ps.println(output);
                    }
                } else {
                    System.out.println(output);
                }

                continue;
            }

            // cd builtin
            if (parts[0].equals("cd")) {
                if (parts.length < 2) {
                    continue;
                }

                String path = parts[1];
                File newDir;

                if (path.equals("~")) {
                    newDir = new File(System.getenv("HOME"));
                } else if (path.startsWith("/")) {
                    newDir = new File(path);
                } else {
                    newDir = new File(currentDirectory, path);
                }

                newDir = newDir.getCanonicalFile();

                if (newDir.exists() && newDir.isDirectory()) {
                    currentDirectory = newDir;
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }

                continue;
            }

            // echo builtin
            if (parts[0].equals("echo")) {
                StringBuilder sb = new StringBuilder();

                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) {
                        sb.append(" ");
                    }
                    sb.append(parts[i]);
                }

                if (outputFile != null) {
                    try (PrintStream ps = new PrintStream(new FileOutputStream(outputFile))) {
                        ps.println(sb);
                    }
                } else {
                    System.out.println(sb);
                }

                continue;
            }

            // type builtin
            if (parts[0].equals("type")) {
                if (parts.length < 2) {
                    continue;
                }

                String cmd = parts[1];
                String result;

                if (cmd.equals("echo")
                        || cmd.equals("exit")
                        || cmd.equals("type")
                        || cmd.equals("pwd")
                        || cmd.equals("cd")) {

                    result = cmd + " is a shell builtin";

                } else {
                    String executablePath = findExecutable(cmd);

                    if (executablePath != null) {
                        result = cmd + " is " + executablePath;
                    } else {
                        result = cmd + ": not found";
                    }
                }

                if (outputFile != null) {
                    try (PrintStream ps = new PrintStream(new FileOutputStream(outputFile))) {
                        ps.println(result);
                    }
                } else {
                    System.out.println(result);
                }

                continue;
            }

            // External commands
            String executablePath = findExecutable(parts[0]);

            if (executablePath != null) {

                ProcessBuilder pb = new ProcessBuilder(parts);

                pb.directory(currentDirectory);

                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);

                if (outputFile != null) {
                    pb.redirectOutput(new File(outputFile));
                } else {
                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                }

                Process process = pb.start();
                process.waitFor();

            } else {
                System.out.println(parts[0] + ": command not found");
            }
        }

        scanner.close();
    }
}