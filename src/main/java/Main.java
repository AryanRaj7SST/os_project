import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));

        while (true) {
            System.out.print("$ ");
            System.out.flush();

            String input = reader.readLine();

            if (input == null) {
                break; // EOF
            }

            input = input.trim();

            if (input.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
            } else {
                System.out.println(input + ": command not found");
            }
        }
    }
}