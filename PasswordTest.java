import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        
        // Test various passwords against the stored hash
        String storedHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpubyYbee7rhMFz";
        
        String[] testPasswords = {
            "password123", 
            "password", 
            "123456", 
            "admin", 
            "test", 
            "testpassword",
            "user_000001",
            "testuser1"
        };
        
        System.out.println("Testing password hash: " + storedHash);
        System.out.println("---");
        
        for (String password : testPasswords) {
            boolean matches = encoder.matches(password, storedHash);
            System.out.println("Password: \"" + password + "\" -> " + (matches ? "MATCHES" : "NO MATCH"));
        }
        
        // Also test generating a new hash for password123
        String newHash = encoder.encode("password123");
        System.out.println("\\nNew hash for password123: " + newHash);
        System.out.println("Verification: " + encoder.matches("password123", newHash));
    }
}
