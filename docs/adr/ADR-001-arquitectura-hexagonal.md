# ADR-001: Arquitectura Hexagonal (Ports & Adapters)

| Campo | Valor |
|-------|-------|
| **Estado** | Aceptado |
| **Fecha** | 2026-06-15 |
| **Decisores** | Equipo de arquitectura |
| **Sustituye a** | — |

---

## Contexto

El servicio de precios forma parte de un ecosistema de microservicios de comercio electrónico. El perfil del proyecto requiere explícitamente arquitectura hexagonal, separación de responsabilidades, Clean Code y diseño orientado a DDD.

Se necesita una arquitectura que:
- Permita cambiar la tecnología de persistencia (H2 → PostgreSQL en producción) sin impacto en el dominio.
- Permita testear la lógica de negocio sin infraestructura (sin Spring context, sin base de datos).
- Exponga la lógica como casos de uso independientes del protocolo de entrada (REST hoy, mensajería mañana).
- Facilite el onboarding de nuevos desarrolladores con una estructura predecible.

## Decisión

Se adopta **Arquitectura Hexagonal (Ports & Adapters)** con tres capas bien diferenciadas:

```
┌──────────────────────────────────────────────────────────┐
│                      INFRASTRUCTURE                       │
│  ┌──────────────────┐          ┌─────────────────────┐   │
│  │  REST Controller │          │ Persistence Adapter │   │
│  │  (entrypoint)    │          │ (JPA / H2)          │   │
│  └────────┬─────────┘          └──────────┬──────────┘   │
│           │ <<Port IN>>                   │ <<Port OUT>>  │
├───────────┼───────────────────────────────┼──────────────┤
│           │           APPLICATION         │              │
│  ┌────────▼──────────────────────────────▼────────────┐  │
│  │          GetApplicablePriceUseCaseImpl              │  │
│  └────────────────────────┬───────────────────────────┘  │
├───────────────────────────┼──────────────────────────────┤
│                           │           DOMAIN             │
│  ┌────────────────────────▼───────────────────────────┐  │
│  │ Price · PriceNotFoundException · Ports (interfaces)│  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

**Regla de dependencia**: las capas externas dependen de las internas, nunca al revés. El dominio no importa nada de Spring, JPA ni HTTP.

**Puertos:**
- `GetApplicablePriceUseCase` — puerto de entrada (driving port). Define el contrato que el adaptador REST consume.
- `FindPricePort` — puerto de salida (driven port). Define el contrato que el adaptador de persistencia implementa.

**Adaptadores:**
- `PriceController` — adaptador de entrada. Traduce HTTP → caso de uso.
- `PricePersistenceAdapter` — adaptador de salida. Traduce caso de uso → JPA/H2.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Arquitectura en capas clásica (Controller → Service → Repository) | El dominio quedaría acoplado a Spring (`@Service`, `@Repository`). Dificulta el testing unitario puro y limita la intercambiabilidad de adaptadores. |
| CQRS con buses de comandos | Complejidad excesiva para un único caso de uso de lectura. Valorable si el servicio evoluciona a escrituras complejas. |
| Clean Architecture estricta con capa de Use Cases separada de Application | Sin diferencia práctica para este scope; añade paquetes sin valor. |

## Consecuencias

**Positivas:**
- El caso de uso `GetApplicablePriceUseCaseImpl` se puede probar con Mockito puro, sin levantar Spring.
- Cambiar H2 por PostgreSQL requiere solo un nuevo `PricePersistenceAdapter`; el dominio no se toca.
- La estructura es reconocible por cualquier desarrollador familiarizado con DDD/Hexagonal.

**Negativas / trade-offs:**
- Más archivos que una arquitectura en capas simple (ports, adapters, mappers separados).
- El `BeanConfig` explícito es necesario para mantener el dominio libre de anotaciones Spring.
