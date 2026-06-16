import java.io.File;
import java.io.IOException;
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

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // Shell's current working directory
        File currentDirectory = new File(System.getProperty("user.dir"));

        while (true) {
            System.out.print("$ ");

            String input = scanner.nextLine();

            // exit builtin
            if (input.equals("exit")) {
                break;
            }

            // pwd builtin
            if (input.equals("pwd")) {
                System.out.println(currentDirectory.getCanonicalPath());
                continue;
            }

            // cd builtin
            if (input.startsWith("cd ")) {
                String path = input.substring(3);

                File newDir;

                if (path.equals("~")) {
                    // Home directory
                    String home = System.getenv("HOME");
                    newDir = new File(home);
                } else if (path.startsWith("/")) {
                    // Absolute path
                    newDir = new File(path);
                } else {
                    // Relative path
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
            if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));
                continue;
            }

            // type builtin
            if (input.startsWith("type ")) {
                String cmd = input.substring(5);

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
            String[] parts = input.split(" ");

            String executablePath = findExecutable(parts[0]);

            if (executablePath != null) {
                ProcessBuilder pb = new ProcessBuilder(parts);

                // Execute in current shell directory
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