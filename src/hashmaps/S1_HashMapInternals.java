package hashmaps;

/**
 * ============================================================
 * SECTION 1: HOW A HASHMAP ACTUALLY WORKS INTERNALLY
 * ============================================================
 *
 * This is THE most-asked data structure internals question. If an
 * interviewer says "tell me about HashMap," this is what they want.
 *
 * -------------------------------------------------------
 * 1.1  THE UNDERLYING ARRAY OF BUCKETS
 * -------------------------------------------------------
 *
 * A HashMap is backed by an array called `table`:
 *
 *     Node<K,V>[] table;
 *
 * Each element in the array is a "bucket." Each bucket holds either:
 *   - null (empty)
 *   - A single Node
 *   - A linked list of Nodes (collisions chained together)
 *   - A Red-Black Tree of Nodes (Java 8+, when chain gets long)
 *
 * Each Node stores: { int hash, K key, V value, Node<K,V> next }
 *
 * INITIAL CAPACITY: 16 (the array starts with 16 buckets)
 *   Why 16? It's a power of 2 (required for the bitwise index trick)
 *   and a reasonable starting size that balances memory vs. resizing.
 *
 * LOAD FACTOR: 0.75 (default)
 *   This means: when 75% of buckets are occupied (size > capacity * 0.75),
 *   the table doubles in size and rehashes.
 *   - Too low (0.5): wastes memory, fewer collisions
 *   - Too high (0.9): saves memory, more collisions, slower lookups
 *   - 0.75 is a well-studied sweet spot.
 *
 * THRESHOLD = capacity * loadFactor
 *   With defaults: 16 * 0.75 = 12. So after 12 entries, it resizes to 32.
 *
 *
 * -------------------------------------------------------
 * 1.2  HASHING PROCESS STEP BY STEP
 * -------------------------------------------------------
 *
 * When you call map.put("hello", 42), here's exactly what happens:
 *
 * STEP 1: Compute hashCode
 *   int h = "hello".hashCode();
 *   // "hello".hashCode() = 99162322
 *
 * STEP 2: Spread/Perturb the hash (Java's internal "secondary hash")
 *   h = h ^ (h >>> 16);
 *   //   99162322 in binary: 00000101 11101010 00100110 01010010
 *   //   h >>> 16:           00000000 00000000 00000101 11101010
 *   //   XOR result:         00000101 11101010 00100011 10111000
 *
 *   WHY? Without this, only the low bits determine the bucket index.
 *   If many keys have similar low bits, they all collide. XORing the
 *   high bits into the low bits spreads the hash more evenly.
 *
 * STEP 3: Compute bucket index
 *   int index = (n - 1) & hash;   // n = table.length (capacity)
 *
 *   With capacity 16: index = (16 - 1) & hash = 15 & hash
 *   15 in binary = 00001111
 *   This masks out all but the lowest 4 bits of the hash.
 *
 *   This is equivalent to `hash % 16` but MUCH faster (bitwise AND vs modulo).
 *   IT ONLY WORKS WHEN CAPACITY IS A POWER OF 2 (see 1.5 below).
 *
 * STEP 4: Place the entry in the bucket
 *   - If the bucket is empty → create a new Node, place it there.
 *   - If the bucket has entries → walk the chain:
 *     - If a key with the same hash AND equals() is found → update the value.
 *     - Otherwise → append a new Node to the end of the chain.
 *
 *
 * -------------------------------------------------------
 * 1.3  COLLISION HANDLING — CHAINING
 * -------------------------------------------------------
 *
 * When two different keys map to the same bucket index, a COLLISION occurs.
 * Java's HashMap uses SEPARATE CHAINING: each bucket is a linked list.
 *
 * Example: keys "Aa" and "BB" have the same hashCode (2112) in Java.
 *
 *   table[index]:  [Node("Aa", v1)] → [Node("BB", v2)] → null
 *
 * To find a key in a chain: walk the list, checking hash AND equals() at each node.
 * Average chain length with good hash + load factor 0.75 is ~1.
 *
 * JAVA 8+ TREEIFICATION:
 *   When a chain exceeds 8 nodes (TREEIFY_THRESHOLD = 8), it converts
 *   from a linked list to a RED-BLACK TREE.
 *
 *   Why? A linked list chain has O(n) lookup. A Red-Black Tree has O(log n).
 *   This prevents worst-case O(n) HashMap lookup when many keys collide.
 *
 *   When the tree shrinks below 6 nodes (UNTREEIFY_THRESHOLD = 6),
 *   it converts back to a linked list (trees have higher per-node overhead).
 *
 * VISUAL — Bucket with 3 collisions:
 *
 *   table:
 *   [0] → null
 *   [1] → null
 *   [2] → [Node(key="Aa", val=1)] → [Node(key="BB", val=2)] → [Node(key="CC", val=3)] → null
 *   [3] → [Node(key="hello", val=42)] → null
 *   [4] → null
 *   ...
 *   [15] → null
 *
 *
 * -------------------------------------------------------
 * 1.4  RESIZING (REHASHING)
 * -------------------------------------------------------
 *
 * When size exceeds threshold (capacity * loadFactor):
 *
 * 1. A new array is created with DOUBLE the capacity.
 * 2. EVERY entry in the old array is rehashed and placed into the new array.
 *    The new bucket index changes because `(newCapacity - 1) & hash` uses
 *    more bits now.
 * 3. Old entries either stay at the same index OR move to (oldIndex + oldCapacity).
 *
 * This is O(n) for the resize operation. But since resizing happens
 * exponentially less often (at n=12, 24, 48, 96, ...), the AMORTIZED
 * cost of put() is still O(1).
 *
 * AMORTIZED O(1) explanation:
 *   Think of it like ArrayList.add(). Most adds are O(1). When you hit
 *   capacity, you pay O(n) once to copy everything. But you just doubled
 *   the capacity, so the next O(n) resize won't happen until you've done
 *   n more O(1) operations. Total cost for n operations = O(n). Average = O(1).
 *
 * INTERVIEW TIP: If you know the approximate size of your data upfront,
 * initialize with: new HashMap<>(expectedSize * 4 / 3 + 1)
 * This avoids ALL resizing. Show this optimization in interviews.
 *
 *
 * -------------------------------------------------------
 * 1.5  WHY CAPACITY MUST BE A POWER OF 2
 * -------------------------------------------------------
 *
 * The bucket index formula: index = (capacity - 1) & hash
 *
 * This works as modulo ONLY when capacity is a power of 2.
 *
 * Example with capacity = 16:
 *   16 - 1 = 15 = 0b00001111
 *   hash & 15 keeps only the last 4 bits → values 0..15 → perfect modulo
 *
 * If capacity were 17:
 *   17 - 1 = 16 = 0b00010000
 *   hash & 16 gives only 0 or 16 → terrible distribution!
 *
 * Java ENFORCES power-of-2 capacity: if you pass new HashMap<>(10),
 * it rounds up to 16 internally (tableSizeFor() method).
 *
 * The bitwise AND is used instead of `%` because:
 *   - `%` is a division operation → ~20-40 CPU cycles
 *   - `&` is a bitwise operation → 1 CPU cycle
 *   This matters when you're doing millions of lookups.
 */
public class S1_HashMapInternals {

    /**
     * Simplified implementation demonstrating HashMap internals.
     * NOT production code — this is for understanding the concepts.
     */
    static class SimpleHashMap<K, V> {
        // Each bucket entry
        static class Node<K, V> {
            final int hash;
            final K key;
            V value;
            Node<K, V> next; // chain pointer for collisions

            Node(int hash, K key, V value, Node<K, V> next) {
                this.hash = hash;
                this.key = key;
                this.value = value;
                this.next = next;
            }
        }

        private Node<K, V>[] table;
        private int size;
        private int capacity;
        private final float loadFactor;

        @SuppressWarnings("unchecked")
        public SimpleHashMap(int initialCapacity, float loadFactor) {
            // Round up to nearest power of 2
            this.capacity = Integer.highestOneBit(initialCapacity - 1) << 1;
            if (this.capacity < 16) this.capacity = 16;
            this.loadFactor = loadFactor;
            this.table = new Node[this.capacity];
            this.size = 0;
        }

        public SimpleHashMap() {
            this(16, 0.75f);
        }

        /**
         * The hash spread function — same as Java's HashMap.
         * XOR the high 16 bits into the low 16 bits to reduce collisions.
         */
        private int hash(K key) {
            if (key == null) return 0; // HashMap allows one null key at index 0
            int h = key.hashCode();
            return h ^ (h >>> 16); // spread the higher bits
        }

        /**
         * Get the bucket index for a given hash.
         * Uses bitwise AND instead of modulo — only works with power-of-2 capacity.
         */
        private int bucketIndex(int hash) {
            return (capacity - 1) & hash;
        }

        /**
         * PUT operation — the core of HashMap.
         *
         * 1. Compute hash and bucket index
         * 2. If bucket is empty, create new node
         * 3. If bucket has entries, walk chain looking for matching key
         * 4. If key found, update value
         * 5. If key not found, append to chain
         * 6. If size exceeds threshold, resize
         */
        public V put(K key, V value) {
            int hash = hash(key);
            int index = bucketIndex(hash);

            // Walk the chain at this bucket
            Node<K, V> current = table[index];
            while (current != null) {
                // Found existing key — update value
                if (current.hash == hash && // check hash first (fast)
                    (current.key == key || (key != null && key.equals(current.key)))) {
                    V oldValue = current.value;
                    current.value = value;
                    return oldValue;
                }
                current = current.next;
            }

            // Key not found — insert at the HEAD of the chain (O(1))
            table[index] = new Node<>(hash, key, value, table[index]);
            size++;

            // Resize if needed
            if (size > capacity * loadFactor) {
                resize();
            }
            return null;
        }

        /**
         * GET operation.
         *
         * 1. Compute hash and bucket index
         * 2. Walk the chain looking for matching key
         * 3. Return value if found, null otherwise
         */
        public V get(K key) {
            int hash = hash(key);
            int index = bucketIndex(hash);

            Node<K, V> current = table[index];
            while (current != null) {
                if (current.hash == hash &&
                    (current.key == key || (key != null && key.equals(current.key)))) {
                    return current.value;
                }
                current = current.next;
            }
            return null; // not found
        }

        /**
         * RESIZE — double the capacity and rehash all entries.
         */
        @SuppressWarnings("unchecked")
        private void resize() {
            int newCapacity = capacity * 2;
            Node<K, V>[] newTable = new Node[newCapacity];

            // Rehash every entry
            for (int i = 0; i < capacity; i++) {
                Node<K, V> current = table[i];
                while (current != null) {
                    Node<K, V> next = current.next;  // save next before we modify
                    int newIndex = (newCapacity - 1) & current.hash;
                    current.next = newTable[newIndex]; // insert at head of new chain
                    newTable[newIndex] = current;
                    current = next;
                }
            }

            table = newTable;
            capacity = newCapacity;
        }

        public int size() { return size; }

        /** Debug: show the table structure. */
        public void debugPrint() {
            for (int i = 0; i < capacity; i++) {
                if (table[i] != null) {
                    System.out.print("  [" + i + "] → ");
                    Node<K, V> curr = table[i];
                    while (curr != null) {
                        System.out.print("{" + curr.key + "=" + curr.value + "} → ");
                        curr = curr.next;
                    }
                    System.out.println("null");
                }
            }
        }
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== HASH SPREAD DEMO ===");
        String key = "hello";
        int h = key.hashCode();
        int spread = h ^ (h >>> 16);
        int index16 = (16 - 1) & spread;    // bucket with capacity 16
        int index32 = (32 - 1) & spread;    // bucket with capacity 32

        System.out.println("Key:          \"" + key + "\"");
        System.out.println("hashCode():   " + h);
        System.out.println("After spread: " + spread);
        System.out.println("Bucket (cap=16): " + index16);
        System.out.println("Bucket (cap=32): " + index32);

        System.out.println("\n=== COLLISION DEMO ===");
        // "Aa" and "BB" have the same hashCode in Java
        System.out.println("\"Aa\".hashCode() = " + "Aa".hashCode());
        System.out.println("\"BB\".hashCode() = " + "BB".hashCode());
        System.out.println("Same? " + ("Aa".hashCode() == "BB".hashCode())); // true!

        System.out.println("\n=== SIMPLE HASHMAP DEMO ===");
        SimpleHashMap<String, Integer> map = new SimpleHashMap<>();
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put("Aa", 10);   // these collide
        map.put("BB", 20);   // with "Aa"

        System.out.println("get(\"apple\"):  " + map.get("apple"));   // 1
        System.out.println("get(\"Aa\"):     " + map.get("Aa"));      // 10
        System.out.println("get(\"BB\"):     " + map.get("BB"));      // 20
        System.out.println("get(\"miss\"):   " + map.get("miss"));    // null
        System.out.println("Size: " + map.size());

        System.out.println("\nBucket layout:");
        map.debugPrint();

        // Trigger resize by adding many entries
        System.out.println("\n=== RESIZE DEMO ===");
        SimpleHashMap<Integer, String> numMap = new SimpleHashMap<>();
        for (int i = 0; i < 20; i++) {
            numMap.put(i, "val" + i);
        }
        System.out.println("After 20 inserts, size = " + numMap.size());
        System.out.println("get(15) = " + numMap.get(15)); // "val15"
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — HASHMAP INTERNALS                          │
 * │                                                             │
 * │ 1. The three things to say immediately: "array of buckets,  │
 * │    chaining for collisions, load factor triggers resize."   │
 * │    This shows you know the fundamentals.                    │
 * │                                                             │
 * │ 2. Mention the Java 8 treeification (chain → Red-Black Tree │
 * │    at 8 nodes). This shows you're current.                  │
 * │                                                             │
 * │ 3. The hash spread (h ^ h>>>16) is a great detail that      │
 * │    separates "I read the source code" from "I read a blog." │
 * │                                                             │
 * │ 4. Know the numbers: capacity 16, load factor 0.75,         │
 * │    treeify at 8, untreeify at 6. These come up.             │
 * │                                                             │
 * │ 5. "Why power of 2?" is a common follow-up. Answer: bitwise │
 * │    AND for modulo is 20-40x faster than %. Capacity must be │
 * │    power-of-2 for (n-1)&hash to work as modulo.            │
 * └─────────────────────────────────────────────────────────────┘
 */
