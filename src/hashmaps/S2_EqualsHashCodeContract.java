package hashmaps;

import java.util.*;

/**
 * ============================================================
 * SECTION 2: THE equals() AND hashCode() CONTRACT
 * ============================================================
 *
 * This is a CONFIRMED interview topic. If you use custom objects as
 * HashMap keys (which many problems implicitly require), you must
 * understand this contract.
 *
 * -------------------------------------------------------
 * 2.1  THE CONTRACT (Three Rules)
 * -------------------------------------------------------
 *
 * RULE 1 (MANDATORY):
 *   If a.equals(b) is true → a.hashCode() == b.hashCode() MUST be true.
 *   "Equal objects must have equal hash codes."
 *
 * RULE 2 (ALLOWED):
 *   If a.hashCode() == b.hashCode() → a.equals(b) may or may not be true.
 *   "Equal hash codes do NOT imply equal objects." (Collisions are fine.)
 *
 * RULE 3 (CONSEQUENCE):
 *   If you override equals(), you MUST override hashCode().
 *   If you don't, Rule 1 is likely violated, and HashMap breaks silently.
 *
 * WHY? HashMap uses hashCode() to find the BUCKET, then equals() to find
 * the exact KEY within that bucket. If hashCode() gives different values
 * for equal objects, HashMap looks in the wrong bucket and never finds
 * the key — even though it's "in there."
 *
 *
 * -------------------------------------------------------
 * 2.2  WHAT GOES WRONG WITHOUT PROPER hashCode()
 * -------------------------------------------------------
 *
 * Scenario: You create a Person class, override equals() (two Persons
 * are equal if they have the same name and age), but forget hashCode().
 *
 *   Person p1 = new Person("Alice", 30);
 *   Person p2 = new Person("Alice", 30);
 *
 *   p1.equals(p2) → true   (your equals works)
 *
 *   Map<Person, String> map = new HashMap<>();
 *   map.put(p1, "Engineer");
 *
 *   map.get(p2) → null  ← WRONG! p2 is "equal" to p1, should return "Engineer"
 *
 * What happened:
 *   - p1.hashCode() returns, say, 12345 (default from Object: based on memory address)
 *   - p2.hashCode() returns, say, 67890 (different memory address → different hash)
 *   - put(p1, ...) → stored in bucket for hash 12345
 *   - get(p2) → looks in bucket for hash 67890 → empty → returns null
 *
 * The key is "in" the map but HashMap can't find it because it's
 * looking in the wrong bucket. The object is effectively LOST.
 */
public class S2_EqualsHashCodeContract {

    // =============================================================
    // 2.3  CORRECT IMPLEMENTATION — Person class
    // =============================================================

    static class Person {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        /**
         * CORRECT equals() implementation.
         *
         * Pattern (memorize this 5-step template):
         *   1. Check reference equality (same object → equal)
         *   2. Check for null
         *   3. Check class type (use getClass(), NOT instanceof for symmetric equals)
         *   4. Cast to the correct type
         *   5. Compare every significant field
         */
        @Override
        public boolean equals(Object o) {
            // Step 1: reference equality (fast path)
            if (this == o) return true;

            // Step 2 & 3: null check and type check
            if (o == null || getClass() != o.getClass()) return false;

            // Step 4: cast
            Person person = (Person) o;

            // Step 5: compare all significant fields
            return age == person.age &&
                   Objects.equals(name, person.name); // handles null name safely
        }

        /**
         * CORRECT hashCode() implementation.
         *
         * APPROACH 1: Objects.hash() — simple, clean, good enough for interviews.
         *
         * Internally, Objects.hash() uses the "prime multiplication" pattern:
         *   result = 31 * result + field.hashCode()
         *
         * WHY 31?
         *   - It's an odd prime (reduces collision patterns)
         *   - 31 * i == (i << 5) - i → JVM can optimize to bit shift + subtract
         *   - Empirically tested to give good distributions
         *   - Same reason Java's String.hashCode() uses 31
         */
        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + "}";
        }
    }

    // =============================================================
    // 2.4  MANUAL hashCode() — for when you want more control
    // =============================================================

    static class PersonManualHash {
        private final String name;
        private final int age;
        private final double salary;

        public PersonManualHash(String name, int age, double salary) {
            this.name = name;
            this.age = age;
            this.salary = salary;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PersonManualHash that = (PersonManualHash) o;
            return age == that.age &&
                   Double.compare(that.salary, salary) == 0 &&
                   Objects.equals(name, that.name);
        }

        /**
         * MANUAL hashCode() using the prime multiplication pattern.
         *
         * For each field:
         *   - int:     use the value directly
         *   - long:    (int)(value ^ (value >>> 32))
         *   - double:  use Double.hashCode(value)
         *   - String/Object: use field.hashCode() (null-safe via Objects.hashCode)
         *   - boolean: Boolean.hashCode(value)
         *
         * Combine: result = 31 * result + fieldHash
         */
        @Override
        public int hashCode() {
            int result = 17; // start with a non-zero prime
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + age;
            result = 31 * result + Double.hashCode(salary);
            return result;
        }
    }

    // =============================================================
    // 2.5  BROKEN IMPLEMENTATION — what NOT to do
    // =============================================================

    /** Overrides equals but NOT hashCode — BROKEN with HashMap. */
    static class BrokenPerson {
        private final String name;
        private final int age;

        public BrokenPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BrokenPerson that = (BrokenPerson) o;
            return age == that.age && Objects.equals(name, that.name);
        }

        // hashCode() NOT overridden → uses Object.hashCode() (memory address)
        // This VIOLATES the contract: equal objects may have different hashCodes!
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== CORRECT IMPLEMENTATION ===");
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);

        System.out.println("p1.equals(p2):     " + p1.equals(p2));       // true
        System.out.println("p1.hashCode():     " + p1.hashCode());
        System.out.println("p2.hashCode():     " + p2.hashCode());
        System.out.println("Hash codes equal:  " + (p1.hashCode() == p2.hashCode())); // true

        Map<Person, String> map = new HashMap<>();
        map.put(p1, "Engineer");
        System.out.println("map.get(p2):       " + map.get(p2));  // "Engineer" ← WORKS!

        System.out.println("\n=== BROKEN IMPLEMENTATION ===");
        BrokenPerson bp1 = new BrokenPerson("Alice", 30);
        BrokenPerson bp2 = new BrokenPerson("Alice", 30);

        System.out.println("bp1.equals(bp2):   " + bp1.equals(bp2));     // true
        System.out.println("bp1.hashCode():    " + bp1.hashCode());
        System.out.println("bp2.hashCode():    " + bp2.hashCode());
        System.out.println("Hash codes equal:  " + (bp1.hashCode() == bp2.hashCode())); // almost certainly false

        Map<BrokenPerson, String> brokenMap = new HashMap<>();
        brokenMap.put(bp1, "Engineer");
        System.out.println("brokenMap.get(bp2): " + brokenMap.get(bp2));
        // null ← BROKEN! Can't find the entry even though bp1.equals(bp2)

        System.out.println("\n=== HASHSET WITH CUSTOM OBJECTS ===");
        // HashSet uses hashCode+equals too
        Set<Person> set = new HashSet<>();
        set.add(p1);
        System.out.println("set.contains(p2):  " + set.contains(p2));  // true
        set.add(p2); // p2 is equal to p1, so set size stays 1
        System.out.println("set.size():        " + set.size());        // 1

        Set<BrokenPerson> brokenSet = new HashSet<>();
        brokenSet.add(bp1);
        System.out.println("\nbrokenSet.contains(bp2): " + brokenSet.contains(bp2)); // false
        brokenSet.add(bp2); // bp2 has different hashCode, so it's treated as a new element
        System.out.println("brokenSet.size():        " + brokenSet.size());          // 2 ← WRONG!
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — equals() and hashCode()                    │
 * │                                                             │
 * │ 1. State the contract clearly: "Equal objects MUST have     │
 * │    equal hash codes. The reverse doesn't need to be true."  │
 * │                                                             │
 * │ 2. If asked to implement, use the 5-step template for       │
 * │    equals() and Objects.hash() for hashCode(). Don't try    │
 * │    to be clever — correctness over performance here.        │
 * │                                                             │
 * │ 3. getClass() vs instanceof: getClass() preserves symmetry  │
 * │    (a.equals(b) == b.equals(a)) with subclasses. instanceof │
 * │    can break symmetry. Use getClass() unless you have a     │
 * │    good reason.                                             │
 * │                                                             │
 * │ 4. When asked "what happens if you override equals but not  │
 * │    hashCode?", explain the bucket mismatch scenario:        │
 * │    "Put looks in bucket A, get looks in bucket B, key is    │
 * │    effectively lost." This is the answer they want.         │
 * │                                                             │
 * │ 5. Java records (Java 16+) auto-generate correct equals()   │
 * │    and hashCode() based on all fields. Mention this if      │
 * │    you want to show you're up to date:                      │
 * │      record Person(String name, int age) {}                 │
 * │    Done. No boilerplate needed.                             │
 * └─────────────────────────────────────────────────────────────┘
 */
