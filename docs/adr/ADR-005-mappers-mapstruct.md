# ADR-005: Mappers de conversión separados con MapStruct

| Campo | Valor |
|-------|-------|
| **Estado** | Aceptado |
| **Fecha** | 2026-06-15 |
| **Decisores** | Equipo de arquitectura |
| **Sustituye a** | — |

---

## Contexto

La arquitectura hexagonal introduce tres representaciones distintas del mismo concepto "precio":

| Representación | Capa | Propósito |
|---|---|---|
| `PriceEntity` | Infraestructura / Persistencia | Mapeada a la tabla `PRICES` vía JPA. Contiene anotaciones `@Entity`, `@Column`, etc. |
| `Price` (record) | Dominio | Modelo de negocio puro, sin dependencias de framework. |
| `PriceResponse` (record) | Infraestructura / REST | DTO de salida del endpoint, serializable a JSON. |

Se necesita decidir cómo gestionar las conversiones entre estas representaciones.

## Decisión

Se usan **dos mappers independientes generados por MapStruct**, cada uno con una única responsabilidad:

- `PricePersistenceMapper`: convierte `PriceEntity` → `Price` (dominio). Reside en el paquete de persistencia.
- `PriceRestMapper`: convierte `Price` → `PriceResponse`. Reside en el paquete REST.

```java
@Mapper(componentModel = "spring")
public interface PricePersistenceMapper {
    Price toDomain(PriceEntity entity);
}

@Mapper(componentModel = "spring")
public interface PriceRestMapper {
    PriceResponse toResponse(Price price);
}
```

MapStruct genera las implementaciones en tiempo de compilación (no reflexión en runtime), lo que implica:
- Cero coste de conversión en runtime (código Java directo generado).
- Errores de mapeo detectados en **tiempo de compilación**, no en producción.
- Las implementaciones generadas (`*MapperImpl`) se excluyen del cálculo de cobertura JaCoCo (son código generado, no escrito por el equipo).

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| ModelMapper (reflexión en runtime) | Lento, propenso a errores silenciosos en runtime. No hay validación en tiempo de compilación. |
| Conversión manual en el adaptador (método `toDomain()` escrito a mano) | Funcional para pocos campos, pero escala mal. Requiere tests adicionales de mapeo y mantenimiento manual ante cambios de modelo. |
| Un único mapper que haga ambas conversiones | Viola el principio de responsabilidad única. El mapper de persistencia accedería a tipos del paquete REST y viceversa, creando acoplamiento entre capas de infraestructura. |
| Usar directamente `PriceEntity` como respuesta REST | Expone detalles de persistencia (nombre de columnas, campo `ID` interno, anotaciones JPA) en la API pública. Rompe el encapsulamiento y dificulta la evolución independiente del schema y el contrato API. |

## Consecuencias

**Positivas:**
- Cada mapper tiene exactamente una razón para cambiar.
- Cambios en el schema de BD no afectan al contrato REST y viceversa.
- El código generado es fácilmente inspeccionable en `build/generated/sources/annotationProcessor/`.
- Rendimiento óptimo: sin reflexión, sin proxies dinámicos.

**Negativas / trade-offs:**
- Dos interfaces adicionales en el proyecto.
- Las implementaciones `*MapperImpl` aparecen en el classpath y deben excluirse explícitamente de JaCoCo (ver [ADR-006](ADR-006-modelo-imperativo-sobre-reactivo.md) para la estrategia de cobertura).
- Si los campos del record y la entidad tienen el mismo nombre y tipo, MapStruct los mapea automáticamente; si difieren, requieren anotaciones `@Mapping` explícitas.
