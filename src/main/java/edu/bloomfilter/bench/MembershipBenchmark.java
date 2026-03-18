package edu.bloomfilter.bench;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark JMH que compara el throughput de consulta entre BloomFilter y HashSet.
 *
 * <p>Se mide el throughput (operaciones por milisegundo) de mightContain/contains
 * para ambas estructuras cargadas con N = 1 000 000 de strings. El dataset de
 * queries incluye 50% de elementos presentes y 50% ausentes para simular
 * un escenario realista.</p>
 *
 * <p>Ejecutar con: mvn clean package &amp;&amp; java -jar target/benchmarks.jar</p>
 *
 * <p>Para guardar resultados: java -jar target/benchmarks.jar -o results.txt</p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class MembershipBenchmark {

    /** Número de elementos insertados en ambas estructuras. */
    static final int N = 1_000_000;

    /** Lista de datos de entrenamiento generados aleatoriamente. */
    List<String> data;

    /** Bloom filter cargado con N elementos, epsilon = 1%. */
    BloomFilter<String> bloom;

    /** HashSet cargado con los mismos N elementos. */
    HashSet<String> hashSet;

    /**
     * Array de queries para el benchmark: 500 presentes + 500 ausentes.
     * Se accede con índice pseudo-aleatorio basado en nanoTime para evitar
     * que el JIT optimice el acceso como constante.
     */
    String[] queries;

    /**
     * Prepara los datos antes de cada trial del benchmark.
     *
     * <p>Se ejecuta una vez por fork. Inicializa data, bloom y hashSet
     * con los mismos N elementos, y prepara el array de queries.</p>
     */
    @Setup(Level.Trial)
    public void setup() {
        // Generar N strings aleatorios reproducibles
        data = new ArrayList<>(N);
        Random rng = new Random(0);
        for (int i = 0; i < N; i++) {
            data.add("element-" + rng.nextLong());
        }

        // Construir Bloom filter con epsilon = 1%
        bloom = new BloomFilter<>(N, 0.01, s -> s.hashCode());
        data.forEach(bloom::add);

        // Construir HashSet con los mismos datos
        hashSet = new HashSet<>(data);

        // Preparar queries: mitad presentes, mitad ausentes
        queries = new String[1000];
        for (int i = 0; i < 500; i++) {
            queries[i] = data.get(i);                      // presente
        }
        for (int i = 500; i < 1000; i++) {
            queries[i] = "absent-" + rng.nextLong();       // ausente
        }
    }

    /**
     * Benchmark de consulta sobre BloomFilter.
     *
     * <p>Mide throughput de mightContain(). El índice se calcula con
     * nanoTime % 1000 para acceder a queries de forma pseudo-aleatoria
     * y evitar que el JIT elimine la llamada como código muerto.</p>
     *
     * @return resultado de la consulta (consumido por JMH para evitar optimización)
     */
    @Benchmark
    public boolean bloomQuery() {
        return bloom.mightContain(queries[(int) (System.nanoTime() % 1000)]);
    }

    /**
     * Benchmark de consulta sobre HashSet.
     *
     * <p>Mide throughput de contains(). Usa el mismo patrón de acceso
     * que bloomQuery para una comparación justa.</p>
     *
     * @return resultado de la consulta (consumido por JMH)
     */
    @Benchmark
    public boolean hashSetQuery() {
        return hashSet.contains(queries[(int) (System.nanoTime() % 1000)]);
    }

    /**
     * Benchmark de inserción sobre BloomFilter.
     *
     * <p>Crea un filtro nuevo en cada invocación para medir add() puro.
     * Usa @Benchmark con un filtro de tamaño reducido para mantener
     * el tiempo por operación medible.</p>
     *
     * @return el filtro resultante (consumido por JMH)
     */
    @Benchmark
    public BloomFilter<String> bloomInsert() {
        BloomFilter<String> f = new BloomFilter<>(1000, 0.01, s -> s.hashCode());
        for (int i = 0; i < 100; i++) {
            f.add(queries[i % 1000]);
        }
        return f;
    }

    /**
     * Benchmark de inserción sobre HashSet.
     *
     * <p>Crea un HashSet nuevo en cada invocación para comparar con bloomInsert.</p>
     *
     * @return el HashSet resultante (consumido por JMH)
     */
    @Benchmark
    public HashSet<String> hashSetInsert() {
        HashSet<String> s = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            s.add(queries[i % 1000]);
        }
        return s;
    }
}