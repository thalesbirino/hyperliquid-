import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("=== Password Hashes for data.sql ===\n");

        // User passwords - "password123"
        String userPassword = "password123";
        System.out.println("User Password: " + userPassword);
        System.out.println("Hash: " + encoder.encode(userPassword));
        System.out.println();

        // Strategy password - "Admin@9090"
        String strategyPassword = "Admin@9090";
        System.out.println("Strategy Password: " + strategyPassword);
        System.out.println("Hash: " + encoder.encode(strategyPassword));
        System.out.println();

        System.out.println("=== Copy these hashes to data.sql ===");
    }
}
