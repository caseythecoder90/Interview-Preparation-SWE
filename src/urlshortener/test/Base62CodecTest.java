package urlshortener.test;

import urlshortener.codec.Base62Codec;

public class Base62CodecTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    static void testEncodeBasic() {
        System.out.println("\n=== Test 1: Encode Basic Values ===");
        check("0 encodes to '0'", Base62Codec.encode(0).equals("0"));
        check("1 encodes to '1'", Base62Codec.encode(1).equals("1"));
        check("9 encodes to '9'", Base62Codec.encode(9).equals("9"));
        check("10 encodes to 'a'", Base62Codec.encode(10).equals("a"));
        check("35 encodes to 'z'", Base62Codec.encode(35).equals("z"));
        check("36 encodes to 'A'", Base62Codec.encode(36).equals("A"));
        check("61 encodes to 'Z'", Base62Codec.encode(61).equals("Z"));
        check("62 encodes to '10'", Base62Codec.encode(62).equals("10"));
    }

    static void testRoundTrip() {
        System.out.println("\n=== Test 2: Encode/Decode Round-Trip ===");
        long[] values = {0, 1, 61, 62, 100, 999, 12345, 1_000_000,
                Integer.MAX_VALUE, 999_999_999_999L};
        for (long val : values) {
            String encoded = Base62Codec.encode(val);
            long decoded = Base62Codec.decode(encoded);
            check("Round-trip " + val + " → '" + encoded + "' → " + decoded, val == decoded);
        }
    }

    static void testPaddedEncode() {
        System.out.println("\n=== Test 3: Padded Encoding ===");
        check("0 padded to 6 = '000000'", Base62Codec.encode(0, 6).equals("000000"));
        check("1 padded to 6 = '000001'", Base62Codec.encode(1, 6).equals("000001"));
        check("Padded length is 6", Base62Codec.encode(100, 6).length() == 6);
        // Large number doesn't need padding — should be unchanged
        String large = Base62Codec.encode(999_999_999L, 6);
        check("Large number ≥ 6 chars", large.length() >= 6);
    }

    static void testDecodeInvalid() {
        System.out.println("\n=== Test 4: Decode Invalid Input ===");

        boolean caught1 = false;
        try { Base62Codec.decode(null); } catch (IllegalArgumentException e) { caught1 = true; }
        check("Null throws", caught1);

        boolean caught2 = false;
        try { Base62Codec.decode(""); } catch (IllegalArgumentException e) { caught2 = true; }
        check("Empty throws", caught2);

        boolean caught3 = false;
        try { Base62Codec.decode("abc!def"); } catch (IllegalArgumentException e) { caught3 = true; }
        check("Invalid char '!' throws", caught3);
    }

    static void testEncodeNegative() {
        System.out.println("\n=== Test 5: Encode Negative ===");
        boolean caught = false;
        try { Base62Codec.encode(-1); } catch (IllegalArgumentException e) { caught = true; }
        check("Negative number throws", caught);
    }

    static void testMaxValue() {
        System.out.println("\n=== Test 6: Max Value Capacity ===");
        check("6 digits: 56+ billion", Base62Codec.maxValue(6) > 56_000_000_000L);
        check("7 digits: 3.5+ trillion", Base62Codec.maxValue(7) > 3_500_000_000_000L);
    }

    static void testSequentialUniqueness() {
        System.out.println("\n=== Test 7: Sequential Codes are Unique ===");
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (long i = 0; i < 10000; i++) {
            codes.add(Base62Codec.encode(i));
        }
        check("10000 sequential values produce 10000 unique codes", codes.size() == 10000);
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Base62 Codec — Test Suite                  ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testEncodeBasic();
        testRoundTrip();
        testPaddedEncode();
        testDecodeInvalid();
        testEncodeNegative();
        testMaxValue();
        testSequentialUniqueness();

        System.out.println("\n══════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("══════════════════════════════════════════════");
    }
}
