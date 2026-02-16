package stacksqueues;

import java.util.*;

/**
 * ============================================================
 * SECTION 2: STACK PATTERNS IN INTERVIEWS
 * ============================================================
 *
 * Five core patterns that cover 90% of stack interview questions:
 *
 *   1. Matching / Balancing (Valid Parentheses)
 *   2. Monotonic Stack (Daily Temperatures)
 *   3. Expression Evaluation / Parsing (nested state tracking)
 *   4. Iterative Tree Traversal (explicit stack = call stack)
 *   5. Min Stack (data structure composition)
 */
public class S2_StackPatterns {

    // =============================================================
    // 2.1  MATCHING / BALANCING — Valid Parentheses (LC 20)
    // =============================================================
    //
    // PATTERN: Push opening brackets. On closing bracket, pop and check match.
    //
    // WHY A STACK?
    //   Brackets nest — the most RECENTLY opened bracket must close FIRST.
    //   That's LIFO. Stack is the natural fit.
    //
    // Example walkthrough: s = "({[]})"
    //
    //   Char   Action              Stack (top→)
    //   ─────  ──────────────────  ─────────────
    //   '('    push '('            (
    //   '{'    push '{'            { (
    //   '['    push '['            [ { (
    //   ']'    pop → '[', matches  { (
    //   '}'    pop → '{', matches  (
    //   ')'    pop → '(', matches  (empty)
    //
    //   Stack empty at end → VALID
    //
    // Edge cases:
    //   - ""        → true (empty is valid)
    //   - "((("     → false (unmatched opening)
    //   - ")))"     → false (unmatched closing — stack is empty when we try to pop)
    //   - "(]"      → false (wrong type match)

    public static boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();

        for (char c : s.toCharArray()) {
            // Push the EXPECTED closing bracket for each opening bracket
            if (c == '(') stack.push(')');
            else if (c == '{') stack.push('}');
            else if (c == '[') stack.push(']');
            else {
                // Closing bracket: stack must not be empty, and top must match
                if (stack.isEmpty() || stack.pop() != c) return false;
            }
        }
        // Stack must be empty (all opening brackets matched)
        return stack.isEmpty();
    }

    // =============================================================
    // 2.2  MONOTONIC STACK — Daily Temperatures (LC 739)
    // =============================================================
    //
    // WHAT IS A MONOTONIC STACK?
    //   A stack where elements are always in sorted order (increasing or decreasing).
    //   When a new element would violate the ordering, we pop elements until order
    //   is restored. The popped elements have "found their answer."
    //
    // PATTERN: "For each element, find the NEXT GREATER (or smaller) element."
    //   → Use a monotonic stack. Iterate through the array:
    //     - While stack is not empty AND current element "answers" the top: pop and record answer.
    //     - Push the current element (or its index) onto the stack.
    //
    // PROBLEM: Given daily temperatures, find how many days you wait for a warmer day.
    //   Input:  [73, 74, 75, 71, 69, 72, 76, 73]
    //   Output: [1,  1,  4,  2,  1,  1,  0,  0]
    //
    // Walkthrough (stack stores INDICES, we look up temperatures by index):
    //
    //   i=0, temp=73: stack empty → push 0           Stack: [0]
    //   i=1, temp=74: 74 > temps[0]=73 → pop 0, answer[0]=1-0=1
    //                 stack empty → push 1            Stack: [1]
    //   i=2, temp=75: 75 > temps[1]=74 → pop 1, answer[1]=2-1=1
    //                 stack empty → push 2            Stack: [2]
    //   i=3, temp=71: 71 < temps[2]=75 → push 3      Stack: [2, 3]
    //   i=4, temp=69: 69 < temps[3]=71 → push 4      Stack: [2, 3, 4]
    //   i=5, temp=72: 72 > temps[4]=69 → pop 4, answer[4]=5-4=1
    //                 72 > temps[3]=71 → pop 3, answer[3]=5-3=2
    //                 72 < temps[2]=75 → push 5       Stack: [2, 5]
    //   i=6, temp=76: 76 > temps[5]=72 → pop 5, answer[5]=6-5=1
    //                 76 > temps[2]=75 → pop 2, answer[2]=6-2=4
    //                 stack empty → push 6            Stack: [6]
    //   i=7, temp=73: 73 < temps[6]=76 → push 7      Stack: [6, 7]
    //
    //   Remaining in stack (6, 7) → answer stays 0 (no warmer day found)
    //
    // Time: O(n) — each index is pushed and popped at most once
    // Space: O(n) — stack can hold all indices in worst case

    public static int[] dailyTemperatures(int[] temperatures) {
        int n = temperatures.length;
        int[] answer = new int[n];
        Deque<Integer> stack = new ArrayDeque<>(); // stores indices

        for (int i = 0; i < n; i++) {
            // Pop indices whose temperature is beaten by current
            while (!stack.isEmpty() && temperatures[i] > temperatures[stack.peek()]) {
                int prevIndex = stack.pop();
                answer[prevIndex] = i - prevIndex;
            }
            stack.push(i);
        }
        // Indices remaining in stack have answer = 0 (default)
        return answer;
    }

    // =============================================================
    // 2.3  EXPRESSION EVALUATION / PARSING PATTERN
    // =============================================================
    //
    // This connects to the Fullstory MARKDOWN PARSER interview question.
    //
    // THE GENERAL PATTERN:
    //   Stacks track NESTED STATE. When you encounter an opening delimiter,
    //   PUSH the current context. When you encounter a closing delimiter,
    //   POP and process. This works for:
    //     - Parentheses matching
    //     - HTML tag matching
    //     - Markdown formatting (bold, italic nesting)
    //     - Calculator expressions
    //     - Compiler parsing
    //
    // EXAMPLE: Basic Calculator with nested parentheses
    //   Evaluate "2*(3+(4*5))"
    //
    //   When we hit '(': push the current result and operator, start fresh.
    //   When we hit ')': pop the saved context and combine.
    //   The stack holds "snapshots" of partially computed results.
    //
    // Below: evaluate simple expressions with + and * and parentheses.
    // (Simplified — real calculators need operator precedence / Shunting Yard.)

    /**
     * Evaluate simple arithmetic expression with +, -, and parentheses.
     * Example: "1+(2+3)" → 6, "2-(3-1)" → 0
     *
     * Uses two-variable tracking:
     *   result = running total for current scope
     *   sign   = +1 or -1 for the next number
     *
     * On '(': push result and sign, reset.
     * On ')': pop sign and result, combine.
     */
    public static int calculate(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        int result = 0;
        int sign = 1;
        int num = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            } else if (c == '+') {
                result += sign * num;
                num = 0;
                sign = 1;
            } else if (c == '-') {
                result += sign * num;
                num = 0;
                sign = -1;
            } else if (c == '(') {
                // Push current context
                stack.push(result);
                stack.push(sign);
                result = 0;
                sign = 1;
            } else if (c == ')') {
                result += sign * num;
                num = 0;
                // Pop saved context
                int savedSign = stack.pop();
                int savedResult = stack.pop();
                result = savedResult + savedSign * result;
            }
        }
        return result + sign * num;
    }

    // =============================================================
    // 2.4  ITERATIVE TREE TRAVERSAL (reference)
    // =============================================================
    //
    // Covered in detail in the trees package (S2_DFSTraversals.java).
    // Key insight repeated here for the stack context:
    //
    // WHY DOES AN EXPLICIT STACK REPLACE RECURSION?
    //   When you write a recursive function, the JVM maintains a CALL STACK:
    //     - Each recursive call pushes a stack frame.
    //     - Each return pops a stack frame.
    //   Converting to iterative just means YOU manage the stack explicitly.
    //
    //   Recursive inorder:          Iterative inorder:
    //     inorder(node.left)          while curr != null: push(curr), go left
    //     process(node)               pop, process
    //     inorder(node.right)         go right
    //
    //   The explicit stack holds the same nodes the call stack would hold.
    //   See S3_StackVsRecursion for a side-by-side comparison.

    // =============================================================
    // 2.5  MIN STACK (LC 155)
    // =============================================================
    //
    // Design a stack that supports push, pop, top, and getMin — ALL in O(1).
    //
    // THE INSIGHT: maintain a parallel record of the minimum at each level.
    //   When you push, also record what the min is at that point in time.
    //   When you pop, the previous min is automatically restored.
    //
    // APPROACH 1: Two stacks (cleaner, easier to explain)
    //   - mainStack: holds all elements
    //   - minStack:  top always holds the current minimum
    //   - On push: push to mainStack. If val <= minStack top, push to minStack too.
    //   - On pop: pop from mainStack. If popped value == minStack top, pop minStack too.
    //
    // APPROACH 2: Single stack with pairs (more space-efficient)
    //   - Each stack entry is [value, currentMin]
    //   - On push: store (val, min(val, previousMin))
    //   - On pop: pop the pair

    /** Approach 1: Two-stack MinStack. */
    static class MinStackTwoStacks {
        private Deque<Integer> mainStack = new ArrayDeque<>();
        private Deque<Integer> minStack = new ArrayDeque<>();

        public void push(int val) {
            mainStack.push(val);
            // Push to minStack if it's empty OR val is a new minimum
            // Use <= (not <) to handle duplicate minimums
            if (minStack.isEmpty() || val <= minStack.peek()) {
                minStack.push(val);
            }
        }

        public void pop() {
            int removed = mainStack.pop();
            // If we're removing the current min, pop minStack too
            if (removed == minStack.peek()) {
                minStack.pop();
            }
        }

        public int top() {
            return mainStack.peek();
        }

        public int getMin() {
            return minStack.peek();
        }
    }

    /** Approach 2: Single stack with [value, currentMin] pairs. */
    static class MinStackPairs {
        private Deque<int[]> stack = new ArrayDeque<>(); // [value, min]

        public void push(int val) {
            int currentMin = stack.isEmpty() ? val : Math.min(val, stack.peek()[1]);
            stack.push(new int[]{val, currentMin});
        }

        public void pop() {
            stack.pop();
        }

        public int top() {
            return stack.peek()[0];
        }

        public int getMin() {
            return stack.peek()[1];
        }
    }

    // Why is MinStack a common interview question?
    //   It tests DATA STRUCTURE COMPOSITION — combining two simple structures
    //   to achieve something neither can do alone. This is the same principle
    //   as HashMap + DLL for LRU Cache. Interviewers want to see that you can
    //   design composite structures, not just use built-in ones.

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== VALID PARENTHESES ===");
        System.out.println("\"({[]})\":  " + isValid("({[]})"));   // true
        System.out.println("\"(]\":      " + isValid("(]"));       // false
        System.out.println("\"((\":      " + isValid("(("));       // false
        System.out.println("\"\":        " + isValid(""));         // true
        System.out.println("\"()[]{}\": " + isValid("()[]{}"));    // true
        System.out.println("\"([)]\":   " + isValid("([)]"));      // false

        System.out.println("\n=== DAILY TEMPERATURES (MONOTONIC STACK) ===");
        int[] temps = {73, 74, 75, 71, 69, 72, 76, 73};
        System.out.println("Temps:   " + Arrays.toString(temps));
        System.out.println("Answer:  " + Arrays.toString(dailyTemperatures(temps)));
        // [1, 1, 4, 2, 1, 1, 0, 0]

        System.out.println("\n=== EXPRESSION EVALUATION ===");
        System.out.println("\"1+(2+3)\":     " + calculate("1+(2+3)"));        // 6
        System.out.println("\"10-(3-1)\":    " + calculate("10-(3-1)"));       // 8
        System.out.println("\"(1+(4+5+2)-3)+(6+8)\": " +
            calculate("(1+(4+5+2)-3)+(6+8)"));  // 23

        System.out.println("\n=== MIN STACK (Two-Stack Approach) ===");
        MinStackTwoStacks ms1 = new MinStackTwoStacks();
        ms1.push(5);
        ms1.push(3);
        ms1.push(7);
        ms1.push(3);
        System.out.println("min: " + ms1.getMin());   // 3
        ms1.pop(); // remove 3
        System.out.println("min: " + ms1.getMin());   // 3 (another 3 remains)
        ms1.pop(); // remove 7
        ms1.pop(); // remove 3
        System.out.println("min: " + ms1.getMin());   // 5

        System.out.println("\n=== MIN STACK (Pair Approach) ===");
        MinStackPairs ms2 = new MinStackPairs();
        ms2.push(2);
        ms2.push(0);
        ms2.push(3);
        ms2.push(0);
        System.out.println("min: " + ms2.getMin());   // 0
        ms2.pop(); // remove 0
        System.out.println("min: " + ms2.getMin());   // 0
        ms2.pop(); // remove 3
        ms2.pop(); // remove 0
        System.out.println("min: " + ms2.getMin());   // 2
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — STACK PATTERNS                              │
 * │                                                             │
 * │ 1. VALID PARENTHESES: the "push expected closing" trick     │
 * │    (push ')' when you see '(') makes the code cleaner than  │
 * │    a big if-else chain for matching. Use it.                │
 * │                                                             │
 * │ 2. MONOTONIC STACK: the key insight is that each element is │
 * │    pushed and popped AT MOST ONCE, so the total work is     │
 * │    O(n) even though there's a while loop inside the for.    │
 * │    Interviewers will ask about this — explain it clearly.   │
 * │                                                             │
 * │ 3. EXPRESSION EVALUATION: the pattern is always the same:   │
 * │    - '(' → push current state, reset                        │
 * │    - ')' → pop saved state, combine with current result     │
 * │    This applies to calculators, HTML parsers, and the       │
 * │    Fullstory markdown parser question.                      │
 * │                                                             │
 * │ 4. MIN STACK: interviewers evaluate whether you realize     │
 * │    you need to track the min AT EACH STACK LEVEL. The       │
 * │    duplicate-min edge case (push 3 twice) catches people.   │
 * │    Use <= not < when deciding whether to push to minStack.  │
 * └─────────────────────────────────────────────────────────────┘
 */
