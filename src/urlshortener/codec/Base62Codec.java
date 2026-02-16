package urlshortener.codec;

/**
 * ============================================================
 * BASE62 CODEC — Encode/Decode Engine
 * ============================================================
 *
 * Converts between long integers and Base62 strings.
 * Base62 alphabet: 0-9 (10) + a-z (26) + A-Z (26) = 62 characters
 *
 * Why Base62 (not Base64)?
 *   - URL-safe: no +, /, or = characters that need encoding
 *   - Compact: a 7-char Base62 string encodes up to 62^7 ≈ 3.5 trillion values
 *   - Human-friendly: easy to copy/paste, no ambiguity
 *
 * How it works (same principle as decimal → any base conversion):
 *   encode(12345):
 *     12345 % 62 = 21 → 'l'     (remainder = least significant digit)
 *     199   % 62 = 13 → 'd'
 *     3     % 62 = 3  → '3'
 *     Result: "3dl" (read remainders bottom-up)
 *
 *   decode("3dl"):
 *     '3' → 3,  'd' → 13,  'l' → 21
 *     3×62² + 13×62 + 21 = 11532 + 806 + 21 = 12345 ✓
 *
 * Space analysis for short URL codes:
 *   6 chars → 62^6 ≈ 56.8 billion unique codes
 *   7 chars → 62^7 ≈ 3.52 trillion unique codes
 *   8 chars → 62^8 ≈ 218 trillion unique codes
 */
public final class Base62Codec {

    // The 62-character alphabet: digits + lowercase + uppercase
    // Using a char array for O(1) index-to-char lookup during encoding
    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final int BASE = 62;

    // Reverse lookup: char → index. Using array for O(1) lookups.
    // Indexed by ASCII value (max 'z' = 122), so size 128 covers it.
    private static final int[] CHAR_TO_INDEX = new int[128];

    static {
        // Initialize all to -1 (invalid character marker)
        java.util.Arrays.fill(CHAR_TO_INDEX, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            CHAR_TO_INDEX[ALPHABET[i]] = i;
        }
    }

    // Prevent instantiation — all methods are static (utility class pattern)
    private Base62Codec() {}

    /**
     * Encodes a non-negative long into a Base62 string.
     *
     * <p>Algorithm: repeated division by 62, collecting remainders.
     * Same as converting decimal to any base.
     *
     * <p>Time Complexity: O(log₆₂(n)) ≈ O(1) — at most 11 iterations for Long.MAX_VALUE
     * <p>Space Complexity: O(log₆₂(n)) for the output string
     *
     * @param num the non-negative number to encode
     * @return Base62 string representation
     * @throws IllegalArgumentException if num is negative
     */
    public static String encode(long num) {
        if (num < 0) {
            throw new IllegalArgumentException("Cannot encode negative number: " + num);
        }
        if (num == 0) {
            return String.valueOf(ALPHABET[0]);  // "0"
        }

        // Build the string in reverse (LSB first), then reverse
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            int remainder = (int) (num % BASE);
            sb.append(ALPHABET[remainder]);
            num /= BASE;
        }

        return sb.reverse().toString();
    }

    /**
     * Encodes a number into a Base62 string with minimum length (zero-padded).
     *
     * <p>Useful for ensuring all short codes have the same length.
     * Pads with '0' (the first character in our alphabet) on the left.
     *
     * @param num       the number to encode
     * @param minLength minimum output length (padded with leading '0's)
     * @return Base62 string of at least minLength characters
     */
    public static String encode(long num, int minLength) {
        String encoded = encode(num);
        while (encoded.length() < minLength) {
            encoded = ALPHABET[0] + encoded;  // pad with '0'
        }
        return encoded;
    }

    /**
     * Decodes a Base62 string back to a long.
     *
     * <p>Algorithm: positional notation — multiply each digit by 62^position.
     * Same as converting any base back to decimal.
     *
     * <p>Time Complexity: O(k) where k = string length ≈ O(1)
     * <p>Space Complexity: O(1)
     *
     * @param encoded the Base62 string to decode
     * @return the decoded long value
     * @throws IllegalArgumentException if string contains invalid characters
     */
    public static long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Cannot decode null or empty string");
        }

        long result = 0;
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c >= 128 || CHAR_TO_INDEX[c] == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: '" + c + "'");
            }
            // Check for overflow before multiplying
            result = result * BASE + CHAR_TO_INDEX[c];
        }

        return result;
    }

    /**
     * Returns the maximum value encodable in the given number of Base62 digits.
     * Useful for capacity planning.
     *
     * @param digits number of Base62 digits
     * @return maximum encodable value (62^digits - 1)
     */
    public static long maxValue(int digits) {
        return (long) Math.pow(BASE, digits) - 1;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== Base62 Codec Demo ===\n");

        // Basic encoding
        long[] testValues = {0, 1, 61, 62, 100, 12345, 1_000_000, Integer.MAX_VALUE};
        System.out.println("--- Encode/Decode ---");
        for (long val : testValues) {
            String encoded = encode(val);
            long decoded = decode(encoded);
            System.out.printf("  %,15d → %-10s → %,d %s%n",
                    val, encoded, decoded, val == decoded ? "✓" : "MISMATCH!");
        }

        // Padded encoding
        System.out.println("\n--- Padded Encoding (min 7 chars) ---");
        for (long val : new long[]{0, 1, 12345, 1_000_000}) {
            System.out.printf("  %,15d → %s%n", val, encode(val, 7));
        }

        // Capacity analysis
        System.out.println("\n--- Capacity by Code Length ---");
        for (int len = 4; len <= 8; len++) {
            System.out.printf("  %d chars → %,d unique codes%n", len, maxValue(len));
        }
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ BIG-O ANALYSIS                                              │
 * │   encode(): O(log₆₂ n) ≈ O(1) — max 11 iterations         │
 * │   decode(): O(k) where k = code length ≈ O(1)              │
 * │   Space:    O(k) for the output string                     │
 * │                                                             │
 * │ WHY BASE62 AND NOT...                                       │
 * │   Base64: contains +, /, = which are not URL-safe          │
 * │   Base36: only lowercase+digits → longer codes             │
 * │   Base16 (hex): 16 chars → much longer codes               │
 * │   UUID: 36 chars — way too long for a "short" URL          │
 * │                                                             │
 * │ CAPACITY PLANNING (CRITICAL INTERVIEW TOPIC)               │
 * │   62^6 ≈ 56 billion → enough for most services             │
 * │   62^7 ≈ 3.5 trillion → enough for the internet           │
 * │   "How many URLs does your system need to support?"        │
 * │   → Calculate the minimum code length from this            │
 * │                                                             │
 * │ COMMON FOLLOW-UP                                            │
 * │ Q: "How do you guarantee uniqueness?"                      │
 * │ A: "Three approaches:                                      │
 * │    1. Counter (AtomicLong): each ID is unique by def.     │
 * │    2. Hash: truncate MD5/SHA → check for collision         │
 * │    3. Random: generate + check for collision               │
 * │    Counter is simplest and collision-free."                 │
 * │                                                             │
 * │ Q: "What about sequential/predictable URLs?"              │
 * │ A: "Counter → sequential (bad for security). Fix:         │
 * │    XOR the counter with a secret key before encoding, or  │
 * │    use a bijective shuffle (Feistel cipher). This makes   │
 * │    URLs appear random while remaining collision-free."     │
 * └─────────────────────────────────────────────────────────────┘
 */
