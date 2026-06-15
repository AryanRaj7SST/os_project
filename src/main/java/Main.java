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

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            String input = scanner.nextLine();

            // exit builtin
            if (input.equals("exit")) {
                break;
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
                        || cmd.equals("type")) {

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

            // External command execution
            String[] parts = input.split(" ");
            String executable = findExecutable(parts[0]);

            if (executable != null) {

                List<String> command = new ArrayList<>();
                command.add(executable);

                for (int i = 1; i < parts.length; i++) {
                    command.add(parts[i]);
                }

                ProcessBuilder pb = new ProcessBuilder(command);

                pb.inheritIO(); // show program output in shell

                Process process = pb.start();
                process.waitFor();

            } else {
                System.out.println(parts[0] + ": command not found");
            }
        }

        scanner.close();
    }
}