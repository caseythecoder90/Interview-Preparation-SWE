package corepatterns;

import java.util.*;

/**
 * ============================================================
 * SECTION 6: MARKDOWN-TO-HTML TRANSPILER
 * ============================================================
 *
 * THIS IS A CONFIRMED FULLSTORY INTERVIEW QUESTION.
 *
 * Strategy: build incrementally. Start with the simplest feature,
 * get it working, then add complexity one feature at a time.
 *
 * Features to support:
 *   1. # Heading      → <h1>Heading</h1>  (##, ### for h2, h3)
 *   2. **bold**       → <b>bold</b>
 *   3. *italic*       → <i>italic</i>
 *   4. `code`         → <code>code</code>
 *   5. [text](url)    → <a href="url">text</a>
 *   6. Plain text     → <p>text</p>
 *
 *
 * -------------------------------------------------------
 * DESIGN DECISIONS
 * -------------------------------------------------------
 *
 * Two-phase approach:
 *   Phase 1: LINE-LEVEL parsing — handle headings and paragraph wrapping
 *   Phase 2: INLINE parsing — handle bold, italic, code, links within a line
 *
 * Why two phases?
 *   - Headings and paragraphs are LINE-LEVEL: you check the start of the line
 *   - Bold/italic/code/links are INLINE: they can appear anywhere within text
 *   - Separating them keeps the code clean and modular
 *
 *
 * -------------------------------------------------------
 * INTERVIEW APPROACH
 * -------------------------------------------------------
 *
 * Tell the interviewer:
 *   "I'll start with headings since they're the simplest — just check
 *   the start of each line. Then I'll add inline formatting with a
 *   character-by-character parser. I'll handle bold/italic with a
 *   state machine, and links with a bracket-tracking approach."
 *
 * Then build it feature by feature:
 *   Version 1: headings only
 *   Version 2: + bold and italic
 *   Version 3: + code spans
 *   Version 4: + links
 *   Version 5: + paragraph wrapping
 */
public class S6_MarkdownParser {

    // =============================================================
    // VERSION 1: Headings only (simplest — get this working first)
    // =============================================================
    //
    // # text   → <h1>text</h1>
    // ## text  → <h2>text</h2>
    // ### text → <h3>text</h3>
    //
    // Algorithm: count leading '#' characters, extract the rest as content.

    /** Parse heading lines only. Non-headings pass through unchanged. */
    static String parseHeading(String line) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        // Must be followed by a space to be a valid heading
        if (level > 0 && level <= 6 && level < line.length() && line.charAt(level) == ' ') {
            String content = line.substring(level + 1).trim();
            return "<h" + level + ">" + content + "</h" + level + ">";
        }
        return null; // not a heading
    }

    // =============================================================
    // VERSION 2-4: Inline formatting (bold, italic, code, links)
    // =============================================================
    //
    // Character-by-character parsing with state tracking.
    //
    // Processing order matters for correctness:
    //   1. Backtick (`) — code spans (highest priority, no nesting inside)
    //   2. Double star (**) — bold
    //   3. Single star (*) — italic
    //   4. Square bracket ([) — start of link
    //
    // Code spans take priority because nothing is formatted inside them.
    // "This is `**not bold**`" → "This is <code>**not bold**</code>"

    /** Parse inline formatting: **bold**, *italic*, `code`, [text](url). */
    static String parseInline(String text) {
        StringBuilder output = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            // --- BACKTICK: code span (highest priority) ---
            if (c == '`') {
                int close = text.indexOf('`', i + 1);
                if (close != -1) {
                    output.append("<code>").append(text, i + 1, close).append("</code>");
                    i = close + 1;
                    continue;
                }
                // No closing backtick — treat as literal
                output.append(c);
                i++;
                continue;
            }

            // --- DOUBLE STAR: bold ---
            if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                // Find closing **
                int close = text.indexOf("**", i + 2);
                if (close != -1) {
                    String inner = text.substring(i + 2, close);
                    // Recursively parse inner content (could contain italic, code, links)
                    output.append("<b>").append(parseInline(inner)).append("</b>");
                    i = close + 2;
                    continue;
                }
                // No closing ** — treat as literal
                output.append("**");
                i += 2;
                continue;
            }

            // --- SINGLE STAR: italic ---
            if (c == '*') {
                // Find closing * (but not **)
                int close = findClosingStar(text, i + 1);
                if (close != -1) {
                    String inner = text.substring(i + 1, close);
                    output.append("<i>").append(parseInline(inner)).append("</i>");
                    i = close + 1;
                    continue;
                }
                output.append(c);
                i++;
                continue;
            }

            // --- LINK: [text](url) ---
            if (c == '[') {
                int closeBracket = text.indexOf(']', i + 1);
                if (closeBracket != -1 && closeBracket + 1 < text.length()
                        && text.charAt(closeBracket + 1) == '(') {
                    int closeParen = text.indexOf(')', closeBracket + 2);
                    if (closeParen != -1) {
                        String linkText = text.substring(i + 1, closeBracket);
                        String url = text.substring(closeBracket + 2, closeParen);
                        output.append("<a href=\"").append(url).append("\">")
                              .append(parseInline(linkText)).append("</a>");
                        i = closeParen + 1;
                        continue;
                    }
                }
                output.append(c);
                i++;
                continue;
            }

            // --- DEFAULT: pass through ---
            output.append(c);
            i++;
        }

        return output.toString();
    }

    /** Find the next single * that isn't part of **. Returns -1 if not found. */
    private static int findClosingStar(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (text.charAt(i) == '*') {
                // Make sure it's not part of **
                if (i + 1 < text.length() && text.charAt(i + 1) == '*') {
                    i++; // skip the pair
                    continue;
                }
                return i;
            }
        }
        return -1;
    }

    // =============================================================
    // VERSION 5: Full parser — line-level + inline
    // =============================================================
    //
    // Process each line:
    //   1. Check if it's a heading → parse heading + inline formatting
    //   2. Check if it's blank → skip (or could emit <br>)
    //   3. Otherwise → wrap in <p> and parse inline formatting

    /**
     * Full markdown-to-HTML conversion.
     *
     * Processes line by line (headings, paragraphs),
     * then applies inline formatting within each line.
     */
    public static String markdownToHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\n", -1); // -1 preserves trailing empty strings

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue; // skip blank lines
            }

            String heading = parseHeading(trimmed);
            if (heading != null) {
                // Heading — apply inline parsing to the heading content
                // We need to re-parse: extract content, apply inline, rebuild tag
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') level++;
                String content = trimmed.substring(level + 1).trim();
                html.append("<h").append(level).append(">")
                    .append(parseInline(content))
                    .append("</h").append(level).append(">\n");
            } else {
                // Paragraph — wrap in <p>, apply inline formatting
                html.append("<p>").append(parseInline(trimmed)).append("</p>\n");
            }
        }

        return html.toString().trim();
    }

    // =============================================================
    // EDGE CASES TO MENTION IN INTERVIEW
    // =============================================================
    //
    // 1. UNCLOSED DELIMITERS: if ** has no closing **, treat as literal text
    //    "This is **not closed" → "This is **not closed"
    //
    // 2. EMPTY DELIMITERS: "****" — two empty bolds? Or literal stars?
    //    Our parser: treats as <b></b> (empty bold)
    //
    // 3. NESTED FORMATTING: "**bold and *italic* inside**"
    //    Our parser handles this via recursive parseInline
    //
    // 4. CODE SPANS ESCAPE EVERYTHING: "`**not bold**`"
    //    Inside backticks, no formatting is applied
    //
    // 5. LINKS WITH SPECIAL CHARS: "[click](https://example.com?a=1&b=2)"
    //    URL is taken literally — no parsing inside ()
    //
    // 6. MULTIPLE HEADINGS: "## heading" vs "##heading" (no space)
    //    We require a space after # — "##heading" is not a heading

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== VERSION 1: HEADINGS ===");
        System.out.println(parseHeading("# Hello World"));
        // <h1>Hello World</h1>
        System.out.println(parseHeading("## Section Two"));
        // <h2>Section Two</h2>
        System.out.println(parseHeading("### Sub-section"));
        // <h3>Sub-section</h3>
        System.out.println(parseHeading("Not a heading"));
        // null
        System.out.println(parseHeading("#NoSpace"));
        // null (no space after #)

        System.out.println("\n=== VERSION 2-4: INLINE FORMATTING ===");

        System.out.println(parseInline("This is **bold** text"));
        // This is <b>bold</b> text

        System.out.println(parseInline("This is *italic* text"));
        // This is <i>italic</i> text

        System.out.println(parseInline("This is `code` text"));
        // This is <code>code</code> text

        System.out.println(parseInline("Click [here](https://example.com)"));
        // Click <a href="https://example.com">here</a>

        System.out.println(parseInline("**bold and *italic* inside**"));
        // <b>bold and <i>italic</i> inside</b>

        System.out.println(parseInline("Code escapes: `**not bold**`"));
        // Code escapes: <code>**not bold**</code>

        System.out.println(parseInline("Unclosed **bold doesn't break"));
        // Unclosed **bold doesn't break

        System.out.println("\n=== VERSION 5: FULL PARSER ===");

        String markdown = """
            # Welcome

            This is a **paragraph** with *formatting*.

            ## Features

            Use `code` for inline code.

            Visit [Google](https://google.com) for search.

            ### Nested

            **Bold with *italic* inside** works!
            """;

        String html = markdownToHtml(markdown);
        System.out.println(html);
        // <h1>Welcome</h1>
        // <p>This is a <b>paragraph</b> with <i>formatting</i>.</p>
        // <h2>Features</h2>
        // <p>Use <code>code</code> for inline code.</p>
        // <p>Visit <a href="https://google.com">Google</a> for search.</p>
        // <h3>Nested</h3>
        // <p><b>Bold with <i>italic</i> inside</b> works!</p>

        System.out.println("\n=== EDGE CASES ===");
        System.out.println(markdownToHtml("# **Bold Heading**"));
        // <h1><b>Bold Heading</b></h1>

        System.out.println(markdownToHtml("No special formatting here"));
        // <p>No special formatting here</p>

        System.out.println(markdownToHtml("`code with **stars**`"));
        // <p><code>code with **stars**</code></p>
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — MARKDOWN PARSER                             │
 * │                                                             │
 * │ 1. START SIMPLE. Tell the interviewer: "Let me start with   │
 * │    headings, get that working, then add inline formatting." │
 * │    Building incrementally shows engineering maturity.        │
 * │                                                             │
 * │ 2. SEPARATE LINE-LEVEL from INLINE parsing. This is the     │
 * │    key design decision. Headings are line-level. Bold,      │
 * │    italic, code, links are inline. Two phases, clean code.  │
 * │                                                             │
 * │ 3. CODE SPANS ESCAPE EVERYTHING. Mention this proactively:  │
 * │    "Inside backticks, I won't parse any other formatting.   │
 * │    That's why I check for backticks first."                 │
 * │                                                             │
 * │ 4. HANDLE UNCLOSED DELIMITERS gracefully. Don't crash —     │
 * │    treat unclosed markers as literal text. Say: "If I don't │
 * │    find a closing **, I'll treat the opening as literal."   │
 * │                                                             │
 * │ 5. RECURSIVE INLINE PARSING handles nesting naturally:      │
 * │    "**bold *italic* bold**" — the bold parser finds its     │
 * │    content, then recursively parses inline, finding italic. │
 * │                                                             │
 * │ 6. If asked to extend (tables, lists, images), explain the  │
 * │    architecture: "Tables and lists are line-level features.  │
 * │    I'd add a new check in the line-level phase. Images      │
 * │    (![alt](url)) are inline — I'd add another case in the  │
 * │    inline parser." Showing extensibility is key.            │
 * │                                                             │
 * │ 7. TRADE-OFFS to mention:                                   │
 * │    - This parser is O(n) for most inputs (single pass)     │
 * │    - Recursive inline parsing can be O(n^2) worst case with │
 * │      deeply nested formatting — mention but say it's fine   │
 * │      for typical markdown lengths                           │
 * │    - For production: use a proper parsing library (flexmark, │
 * │      commonmark). Hand-rolling is for interviews only.      │
 * └─────────────────────────────────────────────────────────────┘
 */
