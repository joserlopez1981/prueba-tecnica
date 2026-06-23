# ADR-006: Selección de precio por prioridad en el dominio

| Campo | Valor |
|-------|-------|
| **Estado** | Aceptado |
| **Fecha** | 2026-06-17 |
| **Decisores** | Equipo de arquitectura |
| **Supersede a** | — (selección en SQL, práctica anterior eliminada) |

---

## Contexto

La solución anterior delegaba la selección del precio de mayor prioridad a la base de datos mediante `ORDER BY p.priority DESC LIMIT 1`. Si bien esto era eficiente en términos de transferencia de datos, ubicaba una regla de negocio explícita — "ante solapamiento de tarifas, gana la de mayor prioridad" — en una query JPQL de infraestructura.

Esto implicaba dos problemas:

1. **Testabilidad**: la lógica de selección no podía probarse de forma unitaria sin levantar una base de datos.
2. **Responsabilidad**: el dominio era un contenedor pasivo de datos; la decisión de negocio quedaba en la capa de infraestructura.

Las recomendaciones de revisión del puesto solicitaban que el dominio fuera más protagonista y que la lógica de negocio estuviera aislada y explícita.

## Decisión

La selección del precio de mayor prioridad se mueve al **caso de uso** (`GetApplicablePriceUseCaseImpl`), que actúa sobre una lista de candidatos devuelta por el puerto de salida.

**Contrato del puerto modificado:**
```java
// Antes
Optional<Price> findApplicablePrice(Instant applicationDate, Long productId, Long brandId);

// Ahora (ADR-006)
List<Price> findCandidatePrices(Instant applicationDate, Long productId, Long brandId);
```

**Lógica de selección en el caso de uso:**
```java
return candidates.stream()
        .filter(p -> p.isApplicableAt(applicationDate))   // filtro defensivo en dominio
        .max(Comparator.comparingInt(Price::priority)
                .thenComparing(Price::startDate))          // desempate: startDate más reciente
        .orElseThrow(() -> new PriceNotFoundException(productId, brandId));
```

**Lógica de fechas encapsulada en el dominio:**
```java
// Price.java (record)
public boolean isApplicableAt(Instant date) {
    return !date.isBefore(startDate) && !date.isAfter(endDate);
}
```

El filtro por rango de fechas **permanece en SQL** (rendimiento: reduce filas transferidas). Solo la decisión de desambiguación por prioridad sube al dominio.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Mantener lógica en SQL (enfoque anterior) | La regla de negocio no es testable unitariamente; el dominio permanece pasivo |
| Servicio de dominio `PriceSelector` | YAGNI para esta complejidad; un método de stream en el caso de uso es suficiente y más legible |
| `ORDER BY` en SQL + `LIMIT` en Java | Mezcla de responsabilidades sin beneficio claro |

## Consecuencias

**Positivas:**
- La regla "mayor prioridad gana" vive en el dominio y es testable de forma unitaria pura (sin BD).
- `Price.isApplicableAt()` permite tests directos sobre el Value Object.
- Cambiar la regla de desambiguación (p. ej. combinar precios) solo requiere modificar el caso de uso.
- El dominio es más rico y cumple con el principio de Tell, Don't Ask.

**Negativas / trade-offs:**
- Múltiples filas viajan de la BD a Java (en lugar de una sola). Mitigado por `@Cacheable` con Caffeine (ver ADR-007).
- La query SQL pierde el `LIMIT 1` y puede retornar N filas. En producción con millones de tarifas, añadir un índice compuesto sobre `(product_id, brand_id, start_date, end_date)` sigue siendo recomendable.
