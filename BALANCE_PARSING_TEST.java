import java.math.BigDecimal;

/**
 * Test class để kiểm tra parsing logic
 */
public class BalanceParsingTest {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Balance Parsing Logic ===");
        
        // Test parseBalanceFromJsonArray method
        testParseBalanceFromJsonArray();
        
        // Test parseBalance method
        testParseBalance();
    }
    
    /**
     * Test parsing JSON array từ EOS API
     */
    private static void testParseBalanceFromJsonArray() {
        System.out.println("\n1. Testing parseBalanceFromJsonArray:");
        
        String[] testCases = {
            "[\"100.0000 EOS\"]",        // Normal case
            "[]",                        // Empty array
            "[\"0.0000 EOS\"]",          // Zero balance
            "[\"123.4567 USDT\"]",       // Different token
            "[ \"100.0000 EOS\" ]",      // With spaces
            "invalid",                   // Invalid format
            null                         // Null input
        };
        
        for (String testCase : testCases) {
            String result = parseBalanceFromJsonArray(testCase);
            System.out.println("Input: " + testCase + " -> Output: " + result);
        }
    }
    
    /**
     * Test parsing balance string thành BigDecimal
     */
    private static void testParseBalance() {
        System.out.println("\n2. Testing parseBalance:");
        
        String[] testCases = {
            "100.0000 EOS",              // Normal case
            "100.0000",                  // Number only
            "0.0000 EOS",                // Zero balance
            "123.4567 USDT",             // Different token
            "",                          // Empty string
            null,                        // Null input
            "invalid format",            // Invalid format
            "100.0000 EOS EXTRA"         // Extra parts
        };
        
        for (String testCase : testCases) {
            BigDecimal result = parseBalance(testCase);
            System.out.println("Input: \"" + testCase + "\" -> Output: " + result);
        }
    }
    
    /**
     * Parse balance từ EOS API JSON array response
     * Input: ["100.0000 EOS"] hoặc []
     * Output: "100.0000 EOS" hoặc null nếu empty
     */
    private static String parseBalanceFromJsonArray(String jsonArrayString) {
        if (jsonArrayString == null) {
            return null;
        }
        
        try {
            // Remove brackets và quotes
            String cleaned = jsonArrayString.trim();
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
                
                if (cleaned.isEmpty()) {
                    return null; // Empty array []
                }
                
                // Remove quotes nếu có
                if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                    cleaned = cleaned.substring(1, cleaned.length() - 1);
                }
                
                return cleaned.trim();
            }
            
            System.out.println("Invalid JSON array format: " + jsonArrayString);
            return null;
            
        } catch (Exception e) {
            System.out.println("Error parsing balance JSON array: " + jsonArrayString + ", error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse balance từ string "10.0000 EOS" thành BigDecimal
     */
    private static BigDecimal parseBalance(String rawBalance) {
        if (rawBalance == null || rawBalance.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            String cleanBalance = rawBalance.trim();
            
            // Case 1: "10.0000 EOS" format - tách số từ string
            if (cleanBalance.contains(" ")) {
                String[] parts = cleanBalance.split("\\s+");
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    cleanBalance = parts[0];
                }
            }
            
            // Case 2: "10.0000" format - validate và parse
            if (cleanBalance.matches("^-?\\d+(\\.\\d+)?$")) {
                return new BigDecimal(cleanBalance);
            } else {
                System.out.println("Invalid balance format: " + cleanBalance + " (from raw: " + rawBalance + ")");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Cannot parse balance: " + rawBalance + ", error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error parsing balance: " + rawBalance + ", error: " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }
}