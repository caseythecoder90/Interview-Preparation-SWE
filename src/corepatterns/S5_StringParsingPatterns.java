package corepatterns;

import java.util.*;

/**
 * ============================================================
 * SECTION 5: STRING PARSING PATTERNS FOR INTERVIEWS
 * ============================================================
 *
 * Four parsing patterns:
 *   1. Character-by-character with state tracking
 *   2. State machine parsing
 *   3. Stack-based parsing for nested structures
 *   4. Token-based parsing
 *
 * These patterns connect directly to the Fullstory markdown parser question.
 * The markdown parser itself is in S6_MarkdownParser.java.
 */
public class S5_StringParsingPatterns {

    // =============================================================
    // 5.1  CHARACTER-BY-CHARACTER PARSING WITH STATE
    // =============================================================
    //
    // THE FUNDAMENTAL PATTERN for any parser:
    //   - Iterate through the string one character at a time
    //   - Maintain state variables tracking "where am I in the process"
    //   - Use a StringBuilder to accumulate output
    //
    // Template:
    //   StringBuilder output = new StringBuilder();
    //   for (int i = 0; i < input.length(); i++) {
    //       char c = input.charAt(i);
    //       if (state == X && c == '...') {
    //           // transition state, possibly emit output
    //       } else {
    //           output.append(c); // default: pass through
    //       }
    //   }
    //
    // Example: convert escape sequences like \n and \t to actual characters.

    /** Parse escape sequences: \n → newline, \t → tab, \\ → backslash. */
    public static String parseEscapes(String input) {
        StringBuilder output = new StringBuilder();
        boolean escaped = false; // state: did we just see a backslash?

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> output.append('\n');
                    case 't' -> output.append('\t');
                    case '\\' -> output.append('\\');
                    default -> { output.append('\\'); output.append(c); }
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                output.append(c);
            }
        }
        if (escaped) output.append('\\'); // trailing backslash
        return output.toString();
    }

    // =============================================================
    // 5.2  STATE MACHINE PARSING
    // =============================================================
    //
    // A state machine has:
    //   - Discrete STATES (enum or int constants)
    //   - TRANSITIONS between states triggered by input characters
    //   - ACTIONS performed during transitions (emit output, save data)
    //
    // This is more structured than ad-hoc boolean flags. Use it when
    // parsing has 3+ distinct states.
    //
    // Example: parse a simplified bold/italic markdown.
    //   **text** → <b>text</b>
    //   *text*   → <i>text</i>
    //
    // State diagram:
    //
    //   NORMAL --'*'--> SAW_ONE_STAR --'*'--> IN_BOLD
    //     ^                |                     |
    //     |              other → emit <i>,       |
    //     |              go to IN_ITALIC      '*' → SAW_BOLD_STAR
    //     |                                      |
    //     |   IN_ITALIC --'*'--> emit </i>,    '*' → emit </b>,
    //     |   back to NORMAL                   back to NORMAL
    //     |                                      |
    //     +--------------------------------------+

    enum State { NORMAL, SAW_ONE_STAR, IN_ITALIC, IN_BOLD, SAW_BOLD_STAR }

    /** Parse simplified markdown: **bold** and *italic* to HTML tags. */
    public static String parseBoldItalic(String input) {
        StringBuilder output = new StringBuilder();
        State state = State.NORMAL;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (state) {
                case NORMAL -> {
                    if (c == '*') {
                        state = State.SAW_ONE_STAR;
                    } else {
                        output.append(c);
                    }
                }
                case SAW_ONE_STAR -> {
                    if (c == '*') {
                        // Two stars → bold opening
                        output.append("<b>");
                        state = State.IN_BOLD;
                    } else {
                        // Single star → italic opening
                        output.append("<i>");
                        output.append(c);
                        state = State.IN_ITALIC;
                    }
                }
                case IN_ITALIC -> {
                    if (c == '*') {
                        output.append("</i>");
                        state = State.NORMAL;
                    } else {
                        output.append(c);
                    }
                }
                case IN_BOLD -> {
                    if (c == '*') {
                        state = State.SAW_BOLD_STAR;
                    } else {
                        output.append(c);
                    }
                }
                case SAW_BOLD_STAR -> {
                    if (c == '*') {
                        // Two stars → bold closing
                        output.append("</b>");
                        state = State.NORMAL;
                    } else {
                        // Single star inside bold — treat as literal
                        output.append('*');
                        output.append(c);
                        state = State.IN_BOLD;
                    }
                }
            }
        }
        return output.toString();
    }

    // =============================================================
    // 5.3  STACK-BASED PARSING FOR NESTED STRUCTURES
    // =============================================================
    //
    // When parsing involves NESTING (parentheses, HTML tags, nested markdown),
    // use a STACK to track context.
    //
    // Pattern:
    //   - On opening delimiter: PUSH current context onto stack, start new scope
    //   - On closing delimiter: POP saved context, combine with current result
    //
    // Example: decode a nested encoded string like "3[a2[bc]]" → "abcbcabcbcabcbc"
    // (LeetCode 394: Decode String)

    /**
     * Decode nested encoded string: k[encoded_string]
     * Examples:
     *   "3[a]"       → "aaa"
     *   "3[a2[c]]"   → "accaccacc"
     *   "2[abc]3[cd]" → "abcabccdcdcd"
     */
    public static String decodeString(String s) {
        Deque<StringBuilder> stringStack = new ArrayDeque<>();
        Deque<Integer> countStack = new ArrayDeque<>();
        StringBuilder current = new StringBuilder();
        int num = 0;

        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            } else if (c == '[') {
                // Push current context
                stringStack.push(current);
                countStack.push(num);
                current = new StringBuilder(); // start new scope
                num = 0;
            } else if (c == ']') {
                // Pop context and repeat current string
                int repeat = countStack.pop();
                StringBuilder outer = stringStack.pop();
                outer.append(String.valueOf(current).repeat(repeat));
                current = outer;
            } else {
                current.append(c);
            }
        }
        return current.toString();
    }

    // =============================================================
    // 5.4  TOKEN-BASED PARSING
    // =============================================================
    //
    // Split input into meaningful TOKENS first, then process tokens.
    //
    // Use tokenization when:
    //   - Delimiters are clear (spaces, commas, operators)
    //   - Processing is easier at the token level than character level
    //
    // Use character-by-character when:
    //   - Context matters (same char means different things in different contexts)
    //   - No clear delimiters
    //
    // Example: evaluate a simple Reverse Polish Notation (RPN) expression.
    // "2 3 + 4 *" → (2 + 3) * 4 → 20

    /** Evaluate Reverse Polish Notation (LC 150). */
    public static int evalRPN(String[] tokens) {
        Deque<Integer> stack = new ArrayDeque<>();

        for (String token : tokens) {
            switch (token) {
                case "+", "-", "*", "/" -> {
                    int b = stack.pop(); // second operand (popped first!)
                    int a = stack.pop(); // first operand
                    int result = switch (token) {
                        case "+" -> a + b;
                        case "-" -> a - b;
                        case "*" -> a * b;
                        case "/" -> a / b;
                        default -> throw new IllegalArgumentException();
                    };
                    stack.push(result);
                }
                default -> stack.push(Integer.parseInt(token));
            }
        }
        return stack.pop();
    }

    /**
     * Simple tokenizer: split a math expression into number and operator tokens.
     * "12+34*56" → ["12", "+", "34", "*", "56"]
     */
    public static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder num = new StringBuilder();

        for (char c : expr.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.length() > 0) {
                    tokens.add(num.toString());
                    num.setLength(0); // clear
                }
                tokens.add(String.valueOf(c));
            }
        }
        if (num.length() > 0) tokens.add(num.toString());
        return tokens;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== ESCAPE SEQUENCE PARSING ===");
        System.out.println(parseEscapes("Hello\\nWorld\\t!"));
        // Hello
        // World	!
        System.out.println(parseEscapes("path\\\\to\\\\file"));
        // path\to\file

        System.out.println("\n=== STATE MACHINE: BOLD/ITALIC ===");
        System.out.println(parseBoldItalic("This is **bold** and *italic* text."));
        // This is <b>bold</b> and <i>italic</i> text.
        System.out.println(parseBoldItalic("**all bold**"));
        // <b>all bold</b>
        System.out.println(parseBoldItalic("*nested **not** here*"));
        // <i>nested </i><i>not</i><i> here</i>

        System.out.println("\n=== STACK-BASED: DECODE STRING ===");
        System.out.println(decodeString("3[a]"));          // aaa
        System.out.println(decodeString("3[a2[c]]"));      // accaccacc
        System.out.println(decodeString("2[abc]3[cd]"));   // abcabccdcdcd

        System.out.println("\n=== TOKEN-BASED: RPN EVALUATION ===");
        System.out.println(evalRPN(new String[]{"2", "3", "+", "4", "*"})); // 20
        System.out.println(evalRPN(new String[]{"4", "13", "5", "/", "+"})); // 6

        System.out.println("\n=== TOKENIZER ===");
        System.out.println(tokenize("12+34*56"));  // [12, +, 34, *, 56]
        System.out.println(tokenize("100-5+3"));   // [100, -, 5, +, 3]
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — STRING PARSING PATTERNS                     │
 * │                                                             │
 * │ 1. STATE MACHINE is the cleanest way to handle multi-state  │
 * │    parsing. Use an enum for states. Switch on (state, char) │
 * │    pairs. This avoids spaghetti if-else chains.             │
 * │                                                             │
 * │ 2. STACK FOR NESTING: whenever you see brackets, tags, or   │
 * │    any nested structure, reach for a stack. Push on open,   │
 * │    pop on close. The Decode String problem is the canonical │
 * │    example of this pattern.                                 │
 * │                                                             │
 * │ 3. For the FULLSTORY MARKDOWN PARSER:                        │
 * │    Start with the simplest feature (headings). Get it right.│
 * │    Then add bold, italic, code, links incrementally.        │
 * │    The state machine pattern works well for inline markup.  │
 * │    See S6_MarkdownParser.java for the full implementation.  │
 * │                                                             │
 * │ 4. TOKENIZE FIRST when delimiters are clear. It separates   │
 * │    "breaking input into pieces" from "processing pieces."   │
 * │    Cleaner code, easier to debug. RPN evaluation is the     │
 * │    classic example.                                         │
 * └─────────────────────────────────────────────────────────────┘
 */
