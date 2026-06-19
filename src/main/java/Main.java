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
            // jobs builtin
            return null;
        }

        private static String[] parseCommand(String input) {
            List<String> args = new ArrayList<>();
            StringBuilder current = new StringBuilder();

            boolean inSingleQuotes = false;
            boolean inDoubleQuotes = false;

            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);

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

                String outputFile = null;
                String errorFile = null;
                boolean appendOutput = false;
                boolean appendError = false;

                List<String> commandParts = new ArrayList<>();

                for (int i = 0; i < parts.length; i++) {

                    if (parts[i].equals(">") || parts[i].equals("1>")) {
                        appendOutput = false;
                        if (i + 1 < parts.length) {
                            outputFile = parts[i + 1];
                        }
                        i++;
                        continue;
                    }

                    if (parts[i].equals(">>") || parts[i].equals("1>>")) {
                        appendOutput = true;
                        if (i + 1 < parts.length) {
                            outputFile = parts[i + 1];
                        }
                        i++;
                        continue;
                    }

                    if (parts[i].equals("2>")) {
                        if (i + 1 < parts.length) {
                            errorFile = parts[i + 1];
                            appendError = false;
                        }
                        i++;
                        continue;
                    }

                    if (parts[i].equals("2>>")) {
                        if (i + 1 < parts.length) {
                            errorFile = parts[i + 1];
                            appendError = true;
                        }
                        i++;
                        continue;
                    }

                    commandParts.add(parts[i]);
                }

                parts = commandParts.toArray(new String[0]);

                if (parts.length == 0) {
                    continue;
                }

                if (errorFile != null) {
                    File errFile = new File(errorFile);

                    File parent = errFile.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }

                    if (!appendError) {
                        try (FileOutputStream ignored = new FileOutputStream(errFile, false)) {
                            // create empty stderr file
                        }
                    } else {
                        if (!errFile.exists()) {
                            errFile.createNewFile();
                        }
                    }
                }

                if (parts[0].equals("exit")) {
                    break;
                }

                if (parts[0].equals("pwd")) {
                    String output = currentDirectory.getCanonicalPath();

                    if (outputFile != null) {
                        try (PrintStream ps = new PrintStream(new FileOutputStream(outputFile, appendOutput))) {
                            ps.println(output);
                        }
                    } else {
                        System.out.println(output);
                    }

                    continue;
                }

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
                        if (errorFile != null) {
                            try (PrintStream ps = new PrintStream(new FileOutputStream(errorFile, appendError))) {
                                ps.println("cd: " + path + ": No such file or directory");
                            }
                        } else {
                            System.out.println("cd: " + path + ": No such file or directory");
                        }
                    }

                    continue;
                }

                if (parts[0].equals("jobs")) {
                    continue;
                }

                if (parts[0].equals("echo")) {
                    StringBuilder sb = new StringBuilder();

                    for (int i = 1; i < parts.length; i++) {
                        if (i > 1) {
                            sb.append(" ");
                        }
                        sb.append(parts[i]);
                    }

                    if (outputFile != null) {
                        try (PrintStream ps = new PrintStream(new FileOutputStream(outputFile, appendOutput))) {
                            ps.println(sb);
                        }
                    } else {
                        System.out.println(sb);
                    }

                    continue;
                }

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
                            || cmd.equals("cd")
                            || cmd.equals("jobs")) {

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
                        try (PrintStream ps = new PrintStream(new FileOutputStream(outputFile, appendOutput))) {
                            ps.println(result);
                        }
                    } else {
                        System.out.println(result);
                    }

                    continue;
                }

                String executablePath = findExecutable(parts[0]);

                if (executablePath != null) {

                    ProcessBuilder pb = new ProcessBuilder(parts);

                    pb.directory(currentDirectory);
                    pb.redirectInput(ProcessBuilder.Redirect.INHERIT);

                    if (outputFile != null) {
                        if (appendOutput) {
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(outputFile)));
                        } else {
                            pb.redirectOutput(new File(outputFile));
                        }
                    } else {
                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    }

                    if (errorFile != null) {

                        if (appendError) {
                            pb.redirectError(
                                    ProcessBuilder.Redirect.appendTo(
                                            new File(errorFile)
                                    )
                            );
                        } else {
                            pb.redirectError(new File(errorFile));
                        }

                    } else {
                        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    }

                    Process process = pb.start();
                    process.waitFor();

                } else {
                    if (errorFile != null) {
                        try (PrintStream ps = new PrintStream(new FileOutputStream(errorFile, appendError))) {
                            ps.println(parts[0] + ": command not found");
                        }
                    } else {
                        System.out.println(parts[0] + ": command not found");
                    }
                }
            }

            scanner.close();
        }
    }
                        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(outputFile)));
                    } else {
                        pb.redirectOutput(new File(outputFile));
                    }
                } else {
                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                }

                if (errorFile != null) {

                    if (appendError) {
                        pb.redirectError(
                                ProcessBuilder.Redirect.appendTo(
                                        new File(errorFile)));
                    } else {
                        pb.redirectError(new File(errorFile));
                    }
                } else {
                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                }

                Process process = pb.start();
                process.waitFor();

            } else {
                if (errorFile != null) {
                    try (PrintStream ps = new PrintStream(new FileOutputStream(errorFile, appendError))) {
                        ps.println(parts[0] + ": command not found");
                    }}else{System.out.println(parts[0]+": command not found");}}}

scanner.close();}}