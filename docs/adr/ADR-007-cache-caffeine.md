# ADR-007: Caché en memoria con Caffeine

| Campo | Valor |
|-------|-------|
| **Estado** | Aceptado |
| **Fecha** | 2026-06-17 |
| **Decisores** | Equipo de arquitectura |
| **Relacionado con** | [ADR-006](ADR-006-logica-seleccion-en-dominio.md) |

---

## Contexto

ADR-006 cambió el contrato del puerto de salida para retornar `List<Price>` (todos los candidatos en rango de fechas) en lugar de un único resultado. Aunque el filtro por fechas sigue en SQL, ahora pueden transferirse N filas por petición en lugar de una.

El patrón de acceso del servicio es **read-heavy**: las mismas combinaciones de `(productId, brandId, applicationDate)` se consultan frecuentemente (p. ej. páginas de producto con miles de visitas por minuto). Una caché en memoria elimina la latencia de BD para consultas repetidas y reduce la carga sobre el sistema de persistencia.

## Decisión

Se aplica `@Cacheable` en `PricePersistenceAdapter.findCandidatePrices()` usando **Caffeine** como proveedor de caché:

```java
@Cacheable(value = "prices")
public List<Price> findCandidatePrices(Instant applicationDate, Long productId, Long brandId) { ... }
```

**Configuración de la caché (`CacheConfig`):**
| Parámetro | Valor | Justificación |
|-----------|-------|---------------|
| TTL (`expireAfterWrite`) | 5 minutos | Los precios cambian con poca frecuencia; 5 min equilibra frescura y rendimiento |
| `maximumSize` | 1000 entradas | Acota el consumo de heap; suficiente para catálogos de tamaño moderado |
| Nombre | `"prices"` | Aislamiento de caché por dominio |

La clave de caché es generada automáticamente por Spring `SimpleKeyGenerator` a partir de los tres parámetros del método.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Sin caché | Trade-off aceptable con BD local H2, pero no a escala real con múltiples instancias |
| Redis (caché distribuida) | Infraestructura externa innecesaria para este scope; añade complejidad operacional y latencia de red |
| EhCache | Configuración en XML más verbosa; Caffeine es más moderno y ofrece mejor rendimiento (W-TinyLFU) |
| Spring Cache con mapa `ConcurrentHashMap` | Sin TTL nativo ni límite de tamaño; riesgo de memory leak en producción |

## Consecuencias

**Positivas:**
- Reducción significativa de carga en BD para patrones de acceso repetitivos.
- Caffeine es la librería de caché en memoria con mejor rendimiento en JVM (algoritmo W-TinyLFU).
- `spring-boot-starter-cache` es la abstracción estándar de Spring; cambiar de proveedor (p. ej. a Redis en producción) solo requiere cambiar la configuración, no el código de negocio.
- La caché se ubica en la capa de infraestructura (adaptador), manteniendo el dominio y el caso de uso agnósticos a ella.

**Negativas / trade-offs:**
- Datos potencialmente obsoletos hasta que expire el TTL (aceptable: los precios rara vez cambian en ventanas de minutos).
- Caché local (no distribuida): en despliegues con múltiples instancias, cada pod tiene su propia caché. Para escenarios con muy alta consistencia, se requeriría Redis o invalidación vía eventos.
- Consumo adicional de memoria JVM (acotado por `maximumSize = 1000`).

## Nota sobre producción

Para entornos con múltiples instancias (Kubernetes, ECS), considerar:
- Migrar a **Redis** como caché compartida con el mismo contrato `@Cacheable`.
- O usar **invalidación basada en eventos** (p. ej. Kafka/SNS) cuando los precios se actualicen.
