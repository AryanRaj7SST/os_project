import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("$ ");
            System.out.flush();

            String input = reader.readLine();
            if (input == null) {
                break;
            }

            String[] parts = input.trim().split("\\s+");
            String command = parts[0];

            switch (command) {
                case "exit":
                    if (parts.length > 1 && parts[1].equals("0")) {
                        System.exit(0);
                    }
                    break;

                case "echo":
                    System.out.println(input.substring(5));
                    break;

                case "pwd":
                    System.out.println(System.getProperty("user.dir"));
                    break;

                case "type":
                    if (parts.length < 2) {
                        break;
                    }

                    String cmd = parts[1];

                    if (cmd.equals("echo")
                            || cmd.equals("exit")
                            || cmd.equals("type")
                            || cmd.equals("pwd")) {
                        System.out.println(cmd + " is a shell builtin");
                    } else {
                        System.out.println(cmd + ": not found");
                    }
                    break;

                default:
                    System.out.println(command + ": command not found");
            }
        }
    }
}