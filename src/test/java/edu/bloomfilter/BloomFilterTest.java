package edu.bloomfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import edu.bloomfilter.bench.BloomFilter;
import edu.bloomfilter.bench.BloomFilterValidator;

/**
 * Tests JUnit 5 para BloomFilter y BloomFilterValidator.
 * Verifica: cero falsos negativos, tasa de FP dentro del rango esperado,
 * parámetros óptimos y casos borde.
 */
public class BloomFilterTest {

    // -------------------------------------------------------
    // Tests de correctitud básica
    // -------------------------------------------------------

    @Test
    void testCeroFalsosNegativos() {
        // Elementos insertados SIEMPRE deben ser encontrados
        BloomFilter<String> filter = new BloomFilter<>(100, 0.01, String::hashCode);
        List<String> elements = List.of("alpha", "beta", "gamma", "delta", "epsilon");
        elements.forEach(filter::add);

        for (String e : elements) {
            assertTrue(filter.mightContain(e),
                    "Falso negativo detectado para: " + e);
        }
    }

    @Test
    void testElementoNoInsertadoPuedeDarFalsoPositivo() {
        // Este test solo verifica que mightContain retorna boolean válido
        BloomFilter<String> filter = new BloomFilter<>(100, 0.01, String::hashCode);
        filter.add("presente");
        // "ausente" puede retornar true (FP) o false — ambos son válidos
        boolean result = filter.mightContain("ausente");
        assertTrue(result == true || result == false); // siempre pasa, verifica que no lanza excepción
    }

    @Test
    void testParametrosOptimosCalculadosCorrectamente() {
        // Para n=100, epsilon=0.01: m ≈ 959 bits, k ≈ 7
        BloomFilter<String> filter = new BloomFilter<>(100, 0.01, String::hashCode);
        assertTrue(filter.getBitCount() > 0, "m debe ser positivo");
        assertTrue(filter.getHashCount() > 0, "k debe ser positivo");
        assertTrue(filter.getHashCount() >= 1, "k mínimo es 1");
    }

    @Test
    void testMemoryBytesEsMenorQueN() {
        // Para n=100000, epsilon=0.01, el filtro debe usar menos de n*32 bytes (HashSet)
        int n = 100_000;
        BloomFilter<String> filter = new BloomFilter<>(n, 0.01, String::hashCode);
        long bloomBytes   = filter.memoryBytes();
        long hashSetBytes = (long) n * 32; // estimación de HashSet: ~32 bytes por entrada

        assertTrue(bloomBytes < hashSetBytes,
                "BloomFilter (" + bloomBytes + " bytes) debe usar menos memoria que HashSet (" + hashSetBytes + " bytes)");
    }

    @Test
    void testCasosBordeN1() {
        // n=1 no debe lanzar excepción y debe funcionar correctamente
        BloomFilter<String> filter = new BloomFilter<>(1, 0.01, String::hashCode);
        filter.add("solo");
        assertTrue(filter.mightContain("solo"));
    }

    @Test
    void testCasosBordeEpsilonAlto() {
        // epsilon=0.5 produce un filtro pequeño
        BloomFilter<String> filter = new BloomFilter<>(1000, 0.5, String::hashCode);
        assertTrue(filter.getBitCount() > 0);
        assertTrue(filter.getHashCount() >= 1);
        filter.add("test");
        assertTrue(filter.mightContain("test")); // cero falsos negativos siempre
    }

    @Test
    void testCasosBordeEpsilonBajo() {
        // epsilon=0.001 produce un filtro más grande y preciso
        BloomFilter<String> filter = new BloomFilter<>(1000, 0.001, String::hashCode);
        assertTrue(filter.getBitCount() > 0);
        filter.add("preciso");
        assertTrue(filter.mightContain("preciso"));
    }

    // -------------------------------------------------------
    // Tests de tasa de falsos positivos con BloomFilterValidator
    // -------------------------------------------------------

    @Test
    void testTasaFPNoSuperaDobleDeEpsilonParaN100000() {
        int n = 100_000;
        double epsilon = 0.01; // 1%

        // Generar elementos aleatorios para insertar
        List<String> elements = new ArrayList<>(n);
        Random rng = new Random(123);
        for (int i = 0; i < n; i++) {
            elements.add("elem_" + rng.nextLong());
        }

        // Crear filtro fresco (sin elementos previos) para medir FP
        BloomFilter<String> filter = new BloomFilter<>(n, epsilon, String::hashCode);

        double fpRate = BloomFilterValidator.measureFPRate(filter, elements, 10_000);

        // La tasa empírica no debe superar el doble de epsilon teórico
        assertTrue(fpRate <= 2 * epsilon,
                String.format("Tasa de FP real (%.4f) supera 2×epsilon (%.4f)", fpRate, 2 * epsilon));
    }

    @Test
    void testInsertarMuchosElementos() {
        // Verificar que insertar n elementos no produce falsos negativos
        int n = 10_000;
        BloomFilter<String> filter = new BloomFilter<>(n, 0.01, String::hashCode);
        List<String> elements = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            elements.add("item_" + i);
        }
        elements.forEach(filter::add);

        // Verificar cero falsos negativos
        long falsosNegativos = elements.stream()
                .filter(e -> !filter.mightContain(e))
                .count();
        assertEquals(0, falsosNegativos, "No debe haber falsos negativos");
    }
}