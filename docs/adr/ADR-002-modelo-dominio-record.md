# ADR-002: Modelo de dominio con Java Record (Value Object inmutable)

| Campo | Valor |
|-------|-------|
| **Estado** | Aceptado |
| **Fecha** | 2026-06-15 |
| **Decisores** | Equipo de arquitectura |
| **Sustituye a** | — |

---

## Contexto

El modelo de dominio `Price` representa el resultado de una consulta de precio aplicable. Necesitamos decidir cómo representar este objeto en el dominio: clase mutable con getters/setters, clase inmutable POJO o Java record.

Los requisitos para el objeto de dominio son:
- Debe ser **inmutable**: un precio consultado no debe poder modificarse una vez construido.
- No debe contener **lógica de persistencia** ni anotaciones de framework.
- Debe ser **comparable por valor** (dos precios con los mismos datos son equivalentes).
- Debe minimizar el **boilerplate** en el código de dominio.

## Decisión

`Price` se modela como un **Java record** (disponible desde Java 16, GA):

```java
public record Price(
        Long brandId,
        Long productId,
        Integer priceList,
        Integer priority,
        Instant startDate,
        Instant endDate,
        BigDecimal price,
        String currency
) {}
```

Un record en Java proporciona automáticamente:
- Constructor canónico.
- Métodos `equals()` y `hashCode()` basados en los campos.
- Método `toString()` con representación legible.
- Inmutabilidad: todos los campos son `final` por defecto.

La clase `PriceEntity` (JPA) existe de forma independiente en la capa de infraestructura, separada del record de dominio. `MapStruct` gestiona la conversión entre ambas representaciones.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Clase POJO con Lombok `@Value` | `@Value` es inmutabilidad via Lombok, no nativa. Añade dependencia de compilación al dominio y oculta la intención. Los records expresan el intent explícitamente. |
| Clase POJO mutable con `@Getter`/`@Setter` | Viola el principio de inmutabilidad de los Value Objects en DDD. Permite mutación accidental del precio tras su construcción. |
| Clase JPA directamente como dominio (`@Entity` en dominio) | Rompe la arquitectura hexagonal: introduce dependencias de Jakarta Persistence en el dominio. |

## Consecuencias

**Positivas:**
- El dominio queda completamente libre de anotaciones de framework.
- La igualdad por valor es automática y correcta (`equals`/`hashCode`).
- El código es más expresivo y conciso.
- Compatible con Java 17+ (LTS vigente).

**Negativas / trade-offs:**
- Los records no pueden extenderse (son `final` implícitamente). Si se necesita jerarquía de precios en el futuro, habría que migrar a clases.
- La separación entre `Price` (record) y `PriceEntity` (JPA entity) requiere un mapper explícito (`PricePersistenceMapper`).
