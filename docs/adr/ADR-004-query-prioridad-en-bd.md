# ADR-004: Selección de precio por prioridad delegada a la base de datos

| Campo | Valor |
|-------|-------|
| **Estado** | Aceptado |
| **Fecha** | 2026-06-15 |
| **Decisores** | Equipo de arquitectura |
| **Sustituye a** | — |

---

## Contexto

Cuando dos o más tarifas aplican simultáneamente a la misma combinación de `productId` + `brandId` + `applicationDate`, el sistema debe retornar únicamente la de **mayor prioridad** (campo `PRIORITY`, mayor valor numérico = mayor precedencia).

Se necesita decidir **dónde** se resuelve esta lógica de desambiguación: en la base de datos o en la capa de aplicación Java.

## Decisión

La selección del precio de mayor prioridad se delega completamente a la **base de datos** mediante una consulta JPQL que aplica todos los filtros y ordena por prioridad descendente, retornando un único resultado:

```sql
SELECT p FROM PriceEntity p
WHERE p.productId      = :productId
  AND p.brandId        = :brandId
  AND p.startDate     <= :applicationDate
  AND p.endDate       >= :applicationDate
ORDER BY p.priority DESC
LIMIT 1
```

La query se implementa en `PriceJpaRepository` como método anotado con `@Query`, y devuelve `Optional<PriceEntity>` — exactamente 0 o 1 resultados.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Traer todas las filas que aplican y ordenar en Java | Transfiere N filas innecesarias a memoria. Escala mal cuando hay muchas tarifas solapadas. La lógica de negocio de "mayor prioridad gana" quedaría duplicada entre SQL y Java. |
| `findTop1ByProductIdAndBrandIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityDesc` (Spring Data method naming) | Nombre excesivamente largo y frágil ante refactorizaciones. La query `@Query` es más legible y explícita. |
| Columna `is_active` gestionada por triggers | Añade complejidad en la BD y lógica oculta. El enunciado no contempla escrituras; es un antipatrón para un servicio de solo lectura. |
| Vista materializada con prioridad pre-computada | Sobra para una BD en memoria con pocos registros. Valorable en producción con millones de tarifas y alta concurrencia. |

## Consecuencias

**Positivas:**
- Una sola fila viaja de la BD a la aplicación por cada petición.
- La lógica de desambiguación queda expresada en un único lugar (la query), sin posibilidad de divergencia.
- El motor de BD optimiza el plan de ejecución (índice sobre `productId`, `brandId`, `startDate`, `endDate`).
- `Optional<PriceEntity>` en el repositorio obliga al caso de uso a manejar explícitamente el caso de ausencia de precio.

**Negativas / trade-offs:**
- La lógica de "mayor prioridad gana" vive en la capa de infraestructura (query SQL), no en el dominio. Si la regla de negocio cambiase (p. ej. combinar precios en lugar de elegir uno), requeriría modificar la query y posiblemente mover lógica al dominio.
- `LIMIT 1` es una cláusula no estándar en JPQL; en Spring Data con Hibernate sobre H2/PostgreSQL funciona correctamente, pero requiere validación contra otros dialectos SQL si se cambia la BD.

## Nota sobre índices en producción

Para un despliegue productivo con volumen real de datos, se recomienda crear un índice compuesto:

```sql
CREATE INDEX idx_prices_lookup
    ON PRICES (PRODUCT_ID, BRAND_ID, START_DATE, END_DATE);
```

En el entorno actual (H2 en memoria con 4 filas) no es necesario.
