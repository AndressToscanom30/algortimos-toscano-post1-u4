package edu.bloomfilter.bench;

import java.util.BitSet;
import java.util.function.ToIntFunction;

/**
 * Bloom filter parametrizable genérico usando double-hashing sobre BitSet.
 *
 * <p>Un Bloom filter es una estructura de datos probabilística que permite
 * verificar pertenencia con posibilidad de falsos positivos pero nunca
 * falsos negativos. El trade-off clave es: usa mucha menos memoria que un
 * HashSet a cambio de una tasa controlada de falsos positivos.</p>
 *
 * <p>Los parámetros óptimos m y k se calculan automáticamente a partir de
 * n (número esperado de elementos) y ε (tasa deseada de falsos positivos):</p>
 * <ul>
 *   <li>m = ceil(-n × ln(ε) / ln(2)²)  — tamaño del arreglo de bits</li>
 *   <li>k = round((m/n) × ln(2))        — número de funciones hash</li>
 * </ul>
 *
 * <p>Complejidad de add y mightContain: O(k) donde k es constante para
 * parámetros fijos. Uso de memoria: O(m) bits = m/8 bytes.</p>
 *
 * @param <T> tipo de elementos a insertar
 */
public class BloomFilter<T> {

    /** Arreglo de bits que representa el filtro. */
    private final BitSet bits;

    /** Tamaño del arreglo de bits (calculado óptimamente). */
    private final int m;

    /** Número de funciones hash (calculado óptimamente). */
    private final int k;

    /** Función hash base provista por el usuario. */
    private final ToIntFunction<T> hashFn;

    /**
     * Construye un Bloom filter óptimo para n elementos y tasa de falsos
     * positivos epsilon.
     *
     * <p>Fórmulas utilizadas:</p>
     * <pre>
     *   m = ceil(-n * ln(eps) / (ln(2)^2))
     *   k = round((m / n) * ln(2))
     * </pre>
     *
     * <p>Complejidad: O(m) para inicializar el BitSet.</p>
     *
     * @param n       número esperado de elementos a insertar (n >= 1)
     * @param epsilon tasa de falsos positivos deseada (0 < epsilon < 1)
     * @param hashFn  función hash base que mapea T a un entero
     * @pre  n >= 1 y 0 < epsilon < 1 y hashFn != null
     * @post construye un Bloom filter con m bits y k funciones hash óptimos
     */
    public BloomFilter(int n, double epsilon, ToIntFunction<T> hashFn) {
        this.m = (int) Math.ceil(
                -n * Math.log(epsilon) / (Math.log(2) * Math.log(2)));
        this.k = (int) Math.max(1,
                Math.round((double) m / n * Math.log(2)));
        this.bits   = new BitSet(m);
        this.hashFn = hashFn;
    }

    /**
     * Calcula la posición i-ésima usando double-hashing.
     *
     * <p>h_i(x) = (h1(x) + i × h2(x)) mod m</p>
     * <p>h2 se deriva de h1 invirtiendo sus bits y forzando imparidad (OR 1)
     * para garantizar que todas las posiciones sean alcanzables.</p>
     *
     * <p>Complejidad: O(1).</p>
     *
     * @param h1 primer hash del elemento
     * @param h2 segundo hash del elemento (debe ser impar)
     * @param i  índice de la función hash (0 <= i < k)
     * @return posición en el arreglo de bits en rango [0, m-1]
     */
    private int hash(int h1, int h2, int i) {
        // Math.floorMod garantiza resultado no negativo incluso con h1/h2 negativos
        return Math.floorMod(h1 + i * h2, m);
    }

    /**
     * Inserta un elemento en el Bloom filter activando k bits.
     *
     * <p>Nunca produce falsos negativos: si add(x) fue llamado, entonces
     * mightContain(x) siempre retornará true.</p>
     *
     * <p>Complejidad temporal: O(k). Complejidad espacial: O(1) adicional.</p>
     *
     * @param element elemento a insertar (no nulo)
     * @pre  element != null
     * @post los k bits correspondientes a element están activados en bits[]
     */
    public void add(T element) {
        int h1 = hashFn.applyAsInt(element);
        int h2 = Integer.reverse(h1) | 1; // h2 impar para cubrir todas las posiciones
        for (int i = 0; i < k; i++) {
            bits.set(hash(h1, h2, i));
        }
    }

    /**
     * Consulta si un elemento posiblemente está en el conjunto.
     *
     * <p>Retorna false con certeza si el elemento NO está (cero falsos negativos).
     * Retorna true si el elemento PROBABLEMENTE está, con tasa de error <= epsilon.</p>
     *
     * <p>Complejidad temporal: O(k). Complejidad espacial: O(1).</p>
     *
     * @param element elemento a consultar (no nulo)
     * @pre  element != null
     * @post retorna false solo si element definitivamente no fue insertado
     * @return true si el elemento posiblemente está; false si definitivamente no está
     */
    public boolean mightContain(T element) {
        int h1 = hashFn.applyAsInt(element);
        int h2 = Integer.reverse(h1) | 1;
        for (int i = 0; i < k; i++) {
            if (!bits.get(hash(h1, h2, i))) return false; // bit apagado = no insertado
        }
        return true; // todos los bits activos = posiblemente insertado
    }

    /**
     * Retorna el tamaño en bits del arreglo interno (m).
     *
     * @return número de bits del filtro
     */
    public int getBitCount() {
        return m;
    }

    /**
     * Retorna el número de funciones hash utilizadas (k).
     *
     * @return número de funciones hash
     */
    public int getHashCount() {
        return k;
    }

    /**
     * Estima el uso de memoria del filtro en bytes.
     *
     * <p>Usa la fórmula ceil(m / 8) ya que cada bit ocupa 1/8 de byte.</p>
     *
     * @return bytes estimados usados por el BitSet interno
     */
    public long memoryBytes() {
        return (long) Math.ceil(m / 8.0);
    }
}