package corepatterns;

import java.util.*;

/**
 * ============================================================
 * SECTION 4: STRING FUNDAMENTALS IN JAVA
 * ============================================================
 *
 *
 * -------------------------------------------------------
 * 4.1  STRING IMMUTABILITY
 * -------------------------------------------------------
 *
 * Strings in Java are IMMUTABLE — every "modification" creates a NEW String.
 *
 *   String s = "hello";
 *   s = s + " world";   // "hello" is NOT modified.
 *                        // A NEW string "hello world" is created.
 *                        // The old "hello" becomes garbage.
 *
 * WHY THIS MATTERS FOR PERFORMANCE:
 *   Concatenating in a loop: each += creates a new String, copies all
 *   previous characters, then appends. For n concatenations of average
 *   length m, that's O(n * m) copies → O(n²) total.
 *
 *   This is why StringBuilder exists: it's a mutable buffer that
 *   appends in O(1) amortized, giving O(n) total for n appends.
 *
 *
 * -------------------------------------------------------
 * 4.2  STRINGBUILDER
 * -------------------------------------------------------
 *
 * Mutable character sequence. Use whenever building strings in a loop.
 *
 * Key methods:
 *   append(x)          — add to end. O(1) amortized.
 *   insert(index, x)   — insert at position. O(n) — shifts characters.
 *   delete(start, end)  — remove range. O(n).
 *   charAt(index)       — get character at position. O(1).
 *   setCharAt(i, c)     — replace character. O(1).
 *   length()            — current length. O(1).
 *   reverse()           — reverse in place. O(n).
 *   toString()          — convert to immutable String. O(n).
 *   deleteCharAt(i)     — delete single character. O(n).
 *
 *
 * -------------------------------------------------------
 * 4.3  ESSENTIAL STRING METHODS
 * -------------------------------------------------------
 *
 *   charAt(i)           — O(1) character access
 *   substring(s, e)     — O(n) creates a copy in modern Java
 *   toCharArray()       — O(n) convert to char[] for in-place work
 *   split(regex)        — split by delimiter, returns String[]
 *   trim() / strip()    — remove leading/trailing whitespace
 *   indexOf(str)        — find first occurrence, -1 if not found
 *   lastIndexOf(str)    — find last occurrence
 *   startsWith(prefix)  — boolean check
 *   endsWith(suffix)    — boolean check
 *   equals(other)       — content comparison (NOT ==)
 *   compareTo(other)    — lexicographic comparison
 *   toLowerCase() / toUpperCase()
 *   replace(old, new)   — replace all occurrences of a char/string
 *   matches(regex)      — regex match
 *
 *
 * -------------------------------------------------------
 * 4.4  CHARACTER ARITHMETIC
 * -------------------------------------------------------
 *
 * Characters are integers in Java. This enables frequency arrays
 * and encoding tricks that appear constantly in interviews.
 *
 *   'a' - 'a' == 0
 *   'z' - 'a' == 25
 *   'A' - 'A' == 0
 *   'Z' - 'A' == 25
 *   '0' - '0' == 0
 *   '9' - '0' == 9
 *
 * Common patterns:
 *   - Frequency array: int[] freq = new int[26]; freq[c - 'a']++;
 *   - Digit char to int: int digit = c - '0';
 *   - Check lowercase: c >= 'a' && c <= 'z'
 *   - Character class methods: Character.isLetterOrDigit(c), Character.isDigit(c),
 *     Character.toLowerCase(c), Character.isUpperCase(c)
 */
public class S4_StringFundamentals {

    public static void main(String[] args) {

        // =============================================================
        // String immutability — O(n²) anti-pattern
        // =============================================================
        System.out.println("=== STRING IMMUTABILITY ===");

        // BAD — O(n²): each += copies all previous characters
        String bad = "";
        for (int i = 0; i < 5; i++) {
            bad += i + " ";  // creates a new String each time
        }
        System.out.println("BAD (works but O(n^2)): " + bad);

        // GOOD — O(n): StringBuilder appends in O(1) amortized
        StringBuilder good = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            good.append(i).append(' ');
        }
        System.out.println("GOOD (O(n)):           " + good.toString());

        // =============================================================
        // StringBuilder methods
        // =============================================================
        System.out.println("\n=== STRINGBUILDER ===");

        StringBuilder sb = new StringBuilder("hello");
        sb.append(" world");                 // "hello world"
        System.out.println("append: " + sb);

        sb.insert(5, ",");                   // "hello, world"
        System.out.println("insert: " + sb);

        sb.delete(5, 6);                     // "hello world" (remove comma)
        System.out.println("delete: " + sb);

        System.out.println("charAt(0): " + sb.charAt(0));  // 'h'
        sb.setCharAt(0, 'H');               // "Hello world"
        System.out.println("setCharAt: " + sb);

        sb.reverse();                        // "dlrow olleH"
        System.out.println("reverse: " + sb);

        sb.reverse();                        // back to "Hello world"
        System.out.println("length: " + sb.length());     // 11

        // =============================================================
        // Essential String methods
        // =============================================================
        System.out.println("\n=== ESSENTIAL STRING METHODS ===");

        String s = "Hello, World!";

        System.out.println("charAt(7):     " + s.charAt(7));          // 'W'
        System.out.println("substring(7):  " + s.substring(7));       // "World!"
        System.out.println("substring(0,5):" + s.substring(0, 5));    // "Hello"
        System.out.println("indexOf('o'):  " + s.indexOf('o'));        // 4
        System.out.println("lastIndexOf:   " + s.lastIndexOf('o'));    // 8
        System.out.println("startsWith:    " + s.startsWith("Hello"));// true
        System.out.println("endsWith:      " + s.endsWith("!"));      // true
        System.out.println("toLowerCase:   " + s.toLowerCase());
        System.out.println("replace:       " + s.replace('l', 'L'));   // HeLLo, WorLd!

        // split
        String csv = "apple,banana,cherry";
        String[] parts = csv.split(",");
        System.out.println("split: " + Arrays.toString(parts)); // [apple, banana, cherry]

        // trim and strip
        String padded = "  hello  ";
        System.out.println("trim:  '" + padded.trim() + "'");   // 'hello'
        System.out.println("strip: '" + padded.strip() + "'");  // 'hello' (Unicode-aware)

        // toCharArray — useful for in-place manipulation
        char[] chars = "hello".toCharArray();
        chars[0] = 'H';
        System.out.println("toCharArray: " + new String(chars)); // "Hello"

        // =============================================================
        // Character arithmetic and classification
        // =============================================================
        System.out.println("\n=== CHARACTER ARITHMETIC ===");

        // Char-to-index conversion (frequency arrays)
        System.out.println("'c' - 'a' = " + ('c' - 'a')); // 2
        System.out.println("'Z' - 'A' = " + ('Z' - 'A')); // 25
        System.out.println("'7' - '0' = " + ('7' - '0')); // 7

        // Frequency array pattern
        String word = "banana";
        int[] freq = new int[26];
        for (char c : word.toCharArray()) {
            freq[c - 'a']++;
        }
        System.out.print("Frequency of 'banana': ");
        for (int i = 0; i < 26; i++) {
            if (freq[i] > 0) {
                System.out.print((char)('a' + i) + "=" + freq[i] + " ");
            }
        }
        System.out.println(); // a=3 b=1 n=2

        // Character class methods
        System.out.println("\nCharacter class methods:");
        System.out.println("isDigit('5'):    " + Character.isDigit('5'));         // true
        System.out.println("isLetter('a'):   " + Character.isLetter('a'));        // true
        System.out.println("isLetterOrDigit: " + Character.isLetterOrDigit('_'));  // false
        System.out.println("isUpperCase('A'):" + Character.isUpperCase('A'));     // true
        System.out.println("toLowerCase('B'):" + Character.toLowerCase('B'));     // 'b'

        // =============================================================
        // Common interview pattern: build string from digits
        // =============================================================
        System.out.println("\n=== DIGIT PARSING ===");
        String numStr = "12345";
        int num = 0;
        for (char c : numStr.toCharArray()) {
            num = num * 10 + (c - '0');
        }
        System.out.println("Parsed '" + numStr + "' = " + num); // 12345

        // =============================================================
        // String comparison pitfall: == vs .equals()
        // =============================================================
        System.out.println("\n=== COMPARISON PITFALL ===");
        String a = new String("hello");
        String b = new String("hello");
        System.out.println("== :      " + (a == b));        // false (different objects)
        System.out.println(".equals(): " + a.equals(b));    // true (same content)
        // ALWAYS use .equals() for String comparison in Java.
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — STRING FUNDAMENTALS                         │
 * │                                                             │
 * │ 1. ALWAYS use StringBuilder in loops. If an interviewer sees│
 * │    string += in a loop, they'll note it as O(n^2). This is │
 * │    a fundamental Java signal — don't miss it.               │
 * │                                                             │
 * │ 2. Frequency array (int[26]) is faster than HashMap for     │
 * │    lowercase letter counting. Use it when the charset is    │
 * │    bounded (26 letters, 128 ASCII, etc.).                   │
 * │                                                             │
 * │ 3. toCharArray() is your friend for in-place string work.   │
 * │    Strings are immutable but char[] is mutable.             │
 * │                                                             │
 * │ 4. NEVER use == for String comparison. Always .equals().    │
 * │    This is basic but interviewers WILL notice if you get it │
 * │    wrong.                                                   │
 * │                                                             │
 * │ 5. Know Character utility methods (isDigit, isLetter,       │
 * │    toLowerCase). They make parsing code cleaner and show    │
 * │    you know the standard library.                           │
 * └─────────────────────────────────────────────────────────────┘
 */
