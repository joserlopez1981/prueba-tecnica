# ADR-008: Externalización de la configuración de caché

| Campo | Valor |
|-------|-------|
| **Estado** | Aceptado |
| **Fecha** | 2026-06-17 |
| **Decisores** | Equipo de arquitectura |
| **Relacionado con** | [ADR-007](ADR-007-cache-caffeine.md) |

---

## Contexto

ADR-007 introdujo una caché Caffeine para `PricePersistenceAdapter.findCandidatePrices()`. Los parámetros de la caché (TTL y tamaño máximo) estaban definidos como literales en `CacheConfig`:

```java
Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .maximumSize(1000)
    .build()
```

Este enfoque presenta los siguientes problemas:

- **Inflexibilidad por entorno:** Ajustar el TTL para producción (carga mayor) o para desarrollo (caché desactivada o muy corta) requiere recompilar y redesplegar.
- **Valores opacos:** No hay una fuente de verdad explícita para la configuración operacional del servicio. Un operador que lee `application.yml` no puede saber qué valores usa la caché sin leer el código Java.
- **Viola la metodología 12-factor** (factor III — configuración): la configuración que varía entre entornos debe residir en el entorno, no en el código.

## Decisión

Los parámetros de la caché se externalizan a `application.yml` mediante `@ConfigurationProperties`:

```java
@ConfigurationProperties(prefix = "app.cache.prices")
@Validated
public record CacheProperties(
        @Min(1) int ttlMinutes,
        @Min(1) long maxSize
) {}
```

```yaml
app:
  cache:
    prices:
      ttl-minutes: 5
      max-size: 1000
```

`CacheConfig` habilita el binding con `@EnableConfigurationProperties(CacheProperties.class)` y recibe `CacheProperties` como parámetro del bean `pricesCache()`.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| `@Value("${app.cache.prices.ttl-minutes}")` | Funciona, pero no agrupa propiedades relacionadas ni permite validación declarativa con `@Validated` |
| Clase con `@Data` de Lombok | La configuración es inmutable por naturaleza; un record expresa esa semántica con menos código |
| Valores por defecto en el record (`@DefaultValue`) | Se prefiere fallo rápido: si la propiedad no está configurada el contexto no arranca, evitando comportamientos silenciosos |

## Consecuencias

**Positivas:**
- Los parámetros de la caché son visibles y ajustables en `application.yml` sin tocar código Java.
- `@Min(1)` garantiza que valores inválidos (cero o negativos) se detectan en el arranque del contexto Spring, no en runtime.
- Preparado para perfiles Spring (`application-prod.yml`, `application-dev.yml`) con valores específicos por entorno.
- Consistente con el estilo del proyecto: usa Java records, igual que el modelo de dominio `Price`.

**Negativas / trade-offs:**
- Añade un fichero de configuración adicional (`CacheProperties.java`), aunque mínimo (una clase de 6 líneas).
- Requiere que `application.yml` (o el entorno de despliegue) defina siempre las propiedades; no hay fallback en código.
