package edu.bloomfilter.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Clase principal que demuestra el BloomFilter y mide uso de memoria.
 * Útil para verificar el funcionamiento antes de ejecutar el benchmark JMH.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== Bloom Filter vs HashSet — Análisis ===\n");

        demoParametros();
        System.out.println();
        demoFalsosPositivos();
        System.out.println();
        demoMemoria();
    }

    private static void demoParametros() {
        System.out.println("--- Parámetros calculados para distintos n y epsilon ---");
        System.out.printf("%-10s %-10s %-12s %-8s %-15s%n",
                "n", "epsilon", "m (bits)", "k", "memoria (bytes)");
        System.out.println("-".repeat(55));

        int[] ns       = {1_000, 10_000, 100_000, 1_000_000};
        double[] epsilons = {0.10, 0.01, 0.001};

        for (int n : ns) {
            for (double eps : epsilons) {
                BloomFilter<String> f = new BloomFilter<>(n, eps, String::hashCode);
                System.out.printf("%-10d %-10.3f %-12d %-8d %-15d%n",
                        n, eps, f.getBitCount(), f.getHashCount(), f.memoryBytes());
            }
        }
    }

    private static void demoFalsosPositivos() {
        System.out.println("--- Tasa empírica de falsos positivos (n=100 000, epsilon=1%) ---");

        int n = 100_000;
        double epsilon = 0.01;

        List<String> elements = new ArrayList<>(n);
        Random rng = new Random(42);
        for (int i = 0; i < n; i++) {
            elements.add("elem_" + rng.nextLong());
        }

        BloomFilter<String> filter = new BloomFilter<>(n, epsilon, String::hashCode);
        double fpRate = BloomFilterValidator.measureFPRate(filter, elements, 10_000);

        System.out.printf("Tasa teórica:  %.4f (%.2f%%)%n", epsilon, epsilon * 100);
        System.out.printf("Tasa empírica: %.4f (%.2f%%)%n", fpRate, fpRate * 100);
        System.out.printf("Relación emp/teo: %.2fx%n", fpRate / epsilon);
        System.out.println(fpRate <= 2 * epsilon
                ? "✓ Dentro del rango aceptable (<= 2× epsilon)"
                : "✗ Fuera del rango esperado");
    }

    private static void demoMemoria() {
        System.out.println("--- Comparación de memoria estimada (n=1 000 000, epsilon=1%) ---");

        int n = 1_000_000;
        BloomFilter<String> filter = new BloomFilter<>(n, 0.01, String::hashCode);

        long bloomBytes   = filter.memoryBytes();
        long hashSetBytes = (long) n * 32; // ~32 bytes por entrada en HashSet

        System.out.printf("BloomFilter:  %,d bytes  (~%.1f MB)%n",
                bloomBytes, bloomBytes / 1_048_576.0);
        System.out.printf("HashSet est:  %,d bytes  (~%.1f MB)%n",
                hashSetBytes, hashSetBytes / 1_048_576.0);
        System.out.printf("Factor de reducción: %.1fx menos memoria con BloomFilter%n",
                (double) hashSetBytes / bloomBytes);
        System.out.printf("Parámetros: m=%d bits, k=%d funciones hash%n",
                filter.getBitCount(), filter.getHashCount());
    }
}