# TuApellido-post1-u4

Post-Contenido 1 — Unidad 4: Estructuras de Datos Avanzadas y su Impacto en Diseño  
Curso: Diseño de Algoritmos y Sistemas — Ingeniería de Sistemas, UDES 2026

---

## Descripción

Implementación desde cero de un Bloom filter parametrizable en Java 17 comparado
contra HashSet<String> mediante benchmark JMH, midiendo throughput de inserción
y consulta, uso de memoria estimado y tasa real de falsos positivos.

---

## Cómo compilar y ejecutar

### Prerrequisitos
- Java 17+
- Maven 3.8+

### Ejecutar los tests
```bash
mvn test
```

### Ejecutar la demostración de parámetros y falsos positivos
```bash
mvn compile
java -cp target/classes edu.bloomfilter.Main
```

### Ejecutar el benchmark JMH completo
```bash
mvn clean package
java -jar target/benchmarks.jar -o results.txt
```

### Ejecutar el benchmark rápido (para prueba)
```bash
java -jar target/benchmarks.jar -wi 1 -i 2 -f 1
```

---

## Parámetros calculados del Bloom Filter

Los parámetros óptimos m y k se calculan con las siguientes fórmulas:

- **m** (bits) = ceil(-n × ln(ε) / ln(2)²)
- **k** (funciones hash) = round((m/n) × ln(2))

| n         | ε     | m (bits)   | k  | Memoria (bytes) |
|-----------|-------|------------|----|-----------------|
| 1 000     | 0.100 | 4 793      | 3  | 600             |
| 1 000     | 0.010 | 9 586      | 7  | 1 199           |
| 1 000     | 0.001 | 14 378     | 10 | 1 798           |
| 10 000    | 0.100 | 47 926     | 3  | 5 991           |
| 10 000    | 0.010 | 95 851     | 7  | 11 982          |
| 10 000    | 0.001 | 143 776    | 10 | 17 972          |
| 100 000   | 0.100 | 479 253    | 3  | 59 907          |
| 100 000   | 0.010 | 958 506    | 7  | 119 814         |
| 100 000   | 0.001 | 1 437 759  | 10 | 179 720         |
| 1 000 000 | 0.100 | 4 792 530  | 3  | 599 067         |
| 1 000 000 | 0.010 | 9 585 059  | 7  | 1 198 133       |
| 1 000 000 | 0.001 | 14 377 588 | 10 | 1 797 199       |

Se observa que k permanece constante para un mismo ε independientemente de n,
lo que confirma que el número óptimo de funciones hash depende únicamente de la
razón m/n y no del tamaño absoluto del conjunto.

---

## Tasa de falsos positivos (n = 100 000, ε = 1%)

| Métrica              | Valor               |
|----------------------|---------------------|
| Tasa teórica (ε)     | 0.0100 (1.00%)      |
| Tasa empírica        | 0.0099 (0.99%)      |
| Relación emp / teo   | 0.99×               |
| Resultado            | ✓ Dentro del rango aceptable (≤ 2× epsilon) |

La tasa empírica de 0.99% es prácticamente igual a la tasa teórica de 1.00%,
con una relación de 0.99×. Esto valida que las fórmulas de cálculo de m y k
producen exactamente el comportamiento probabilístico esperado.

---

## Comparación de memoria (n = 1 000 000, ε = 1%)

| Estructura        | Memoria estimada       | Parámetros            |
|-------------------|------------------------|-----------------------|
| BloomFilter       | 1 198 133 bytes (~1.1 MB) | m = 9 585 059 bits, k = 7 |
| HashSet\<String\> | 32 000 000 bytes (~30.5 MB) | ~32 bytes por entrada |
| **Factor**        | **BloomFilter usa 26.7× menos memoria** | — |

---

## Análisis y conclusión de diseño

### (a) Throughput de consulta

El BloomFilter aplica k = 7 funciones hash por consulta usando double-hashing
sobre un BitSet, mientras que HashSet ejecuta una sola operación de hash seguida
de una búsqueda en tabla. En la práctica, para conjuntos de gran tamaño como
n = 1 000 000, el HashSet puede sufrir más cache misses debido a su mayor
footprint en memoria (~30.5 MB), mientras que el BitSet del BloomFilter (~1.1 MB)
cabe completamente en la caché L2/L3 del procesador, lo que puede favorecer
su throughput real en escenarios de alta concurrencia de consultas.

### (b) Memoria

Los resultados confirman la ventaja teórica: para n = 1 000 000 con ε = 1%,
el BloomFilter usa 1 198 133 bytes (~1.1 MB) frente a los 32 000 000 bytes
(~30.5 MB) estimados para HashSet, una reducción de 26.7×. Esta ventaja es
especialmente relevante en sistemas con memoria limitada o cuando el conjunto
debe residir en memoria distribuida o caché compartida. Para ε = 0.1% el filtro
usa solo 4 792 530 bits (~599 KB), mostrando que incluso con una tasa de error
diez veces menor el consumo de memoria sigue siendo drásticamente inferior
al de un HashSet exacto.

### (c) Tasa real de FP vs. tasa teórica

La tasa empírica medida fue de 0.99%, prácticamente idéntica al 1.00% teórico
(relación 0.99×). Esto demuestra que la implementación con double-hashing
sobre hashCode() de String es suficientemente uniforme para que las garantías
probabilísticas del modelo teórico se cumplan en la práctica. El resultado está
muy por debajo del umbral de 2× epsilon establecido como criterio de aceptación,
lo que indica que el filtro funciona correctamente.

### (d) Cuándo usar cada estructura

Se debe usar **BloomFilter** cuando:
- La memoria es un recurso crítico y se puede tolerar una pequeña tasa de falsos positivos.
- Se necesita pre-filtrar un conjunto masivo antes de consultar una fuente costosa (base de datos, disco, red), evitando la mayoría de los lookups innecesarios.
- El costo de un falso positivo es bajo, por ejemplo un lookup adicional de verificación.
- El conjunto tiene millones de elementos y debe residir en memoria RAM limitada.

Se debe usar **HashSet** cuando:
- Se requiere exactitud total (cero falsos positivos) y el resultado final no puede tener errores.
- El conjunto es pequeño y la memoria disponible no es un factor limitante.
- Se necesita recuperar, iterar o eliminar elementos, operaciones que el BloomFilter no soporta.
- La lógica de negocio no puede tolerar ningún falso positivo aunque sea de baja probabilidad.

---

## Estructura del proyecto
```
apellido-post1-u4/
├── pom.xml
├── README.md
├── results.txt
└── src/
    ├── main/java/edu/bloomfilter/
    │   ├── BloomFilter.java
    │   ├── BloomFilterValidator.java
    │   ├── Main.java
    │   └── bench/
    │       └── MembershipBenchmark.java
    └── test/java/edu/bloomfilter/
        └── BloomFilterTest.java
```