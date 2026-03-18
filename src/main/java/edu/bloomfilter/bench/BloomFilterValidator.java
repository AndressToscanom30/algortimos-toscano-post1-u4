package edu.bloomfilter.bench;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Herramienta de validación empírica para BloomFilter.
 *
 * <p>Permite medir la tasa real de falsos positivos insertando n elementos
 * en el filtro y consultando queryCount strings que definitivamente no fueron
 * insertados, contando cuántos son reportados como presentes por error.</p>
 *
 * <p>El objetivo es verificar que la tasa empírica sea cercana a la tasa
 * teórica epsilon con la que fue construido el filtro.</p>
 */
public class BloomFilterValidator {

    /**
     * Mide la tasa empírica de falsos positivos de un Bloom filter.
     *
     * <p>Algoritmo:</p>
     * <ol>
     *   <li>Inserta todos los elementos de la lista {@code inserted} en el filtro.</li>
     *   <li>Genera {@code queryCount} strings aleatorios que NO están en inserted.</li>
     *   <li>Consulta cada uno y cuenta cuántos son reportados como presentes (falso positivo).</li>
     *   <li>Retorna fpCount / queryCount.</li>
     * </ol>
     *
     * <p>Complejidad: O(n + queryCount × k) donde k es el número de hash del filtro.</p>
     *
     * @param filter     Bloom filter ya construido (no nulo)
     * @param inserted   lista de strings que fueron insertados en el filtro
     * @param queryCount número de queries negativas a realizar
     * @pre  filter != null, inserted != null, queryCount > 0
     * @post retorna un valor en [0.0, 1.0] representando la tasa de FP real
     * @return tasa empírica de falsos positivos
     */
    public static double measureFPRate(BloomFilter<String> filter,
                                        List<String> inserted,
                                        int queryCount) {
        // Insertar todos los elementos en el filtro
        inserted.forEach(filter::add);

        // Construir un set para verificar rápidamente si un query es realmente nuevo
        Set<String> insertedSet = new HashSet<>(inserted);

        long fpCount = 0;
        long total   = 0;
        Random rng   = new Random(42); // semilla fija para reproducibilidad

        // Generar queries que definitivamente NO están en el filtro
        while (total < queryCount) {
            String query = "query_" + rng.nextLong();
            if (!insertedSet.contains(query)) {
                if (filter.mightContain(query)) fpCount++; // falso positivo
                total++;
            }
        }

        return (double) fpCount / total;
    }

    /**
     * Mide el uso de memoria en bytes de una estructura durante su construcción.
     *
     * <p>Usa Runtime para medir la diferencia de memoria libre antes y después
     * de ejecutar el bloque de construcción. Los resultados son aproximados
     * debido al comportamiento del GC de la JVM.</p>
     *
     * @param builder bloque de código que construye la estructura a medir
     * @return bytes aproximados usados, o -1 si la medición no fue confiable
     */
    public static long measureMemoryBytes(Runnable builder) {
        System.gc(); // solicitar GC antes de medir (no garantizado)
        Runtime rt = Runtime.getRuntime();
        long before = rt.totalMemory() - rt.freeMemory();
        builder.run();
        System.gc();
        long after = rt.totalMemory() - rt.freeMemory();
        return after - before;
    }
}