import java.io.File;
import java.io.IOException;
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

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\'') {
                inSingleQuotes = !inSingleQuotes;
            } else if (Character.isWhitespace(c) && !inSingleQuotes) {
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

            if (parts.length == 0) {
                continue;
            }

            // exit builtin
            if (parts[0].equals("exit")) {
                break;
            }

            // pwd builtin
            if (parts[0].equals("pwd")) {
                System.out.println(currentDirectory.getCanonicalPath());
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
                    System.out.println(
                        "cd: " + path + ": No such file or directory"
                    );
                }

                continue;
            }

            // echo builtin
            if (parts[0].equals("echo")) {
                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) {
                        System.out.print(" ");
                    }
                    System.out.print(parts[i]);
                }
                System.out.println();
                continue;
            }

            // type builtin
            if (parts[0].equals("type")) {
                if (parts.length < 2) {
                    continue;
                }

                String cmd = parts[1];

                if (cmd.equals("echo")
                        || cmd.equals("exit")
                        || cmd.equals("type")
                        || cmd.equals("pwd")
                        || cmd.equals("cd")) {

                    System.out.println(cmd + " is a shell builtin");

                } else {
                    String executablePath = findExecutable(cmd);

                    if (executablePath != null) {
                        System.out.println(cmd + " is " + executablePath);
                    } else {
                        System.out.println(cmd + ": not found");
                    }
                }

                continue;
            }

            // External commands
            String executablePath = findExecutable(parts[0]);

            if (executablePath != null) {
                ProcessBuilder pb = new ProcessBuilder(parts);

                pb.directory(currentDirectory);
                pb.inheritIO();

                Process process = pb.start();
                process.waitFor();
            } else {
                System.out.println(parts[0] + ": command not found");
            }
        }

        scanner.close();
    }
}