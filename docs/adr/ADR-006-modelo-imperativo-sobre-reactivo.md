# ADR-006: Modelo de programación imperativo sobre reactivo

| Campo | Valor |
|-------|-------|
| **Estado** | Aceptado |
| **Fecha** | 2026-06-15 |
| **Decisores** | Equipo de arquitectura |
| **Sustituye a** | — |
| **Revisión sugerida** | Si el servicio migra a BD remota con alta concurrencia (> 1.000 req/s) |

---

## Contexto

Spring Boot ofrece dos modelos de programación para servicios web:

- **Imperativo (bloqueante)**: `spring-boot-starter-web` (Tomcat thread-per-request), Spring Data JPA, JDBC.
- **Reactivo (no bloqueante)**: `spring-boot-starter-webflux` (Netty event-loop), Spring Data R2DBC, drivers R2DBC.

El equipo debe decidir cuál adoptar para este servicio de consulta de precios, considerando:
- El volumen y naturaleza de las operaciones (lectura simple, BD en memoria).
- La complejidad operacional de cada modelo.
- La experiencia del equipo y la facilidad de debugging.
- La escalabilidad futura del servicio.

## Decisión

Se adopta el **modelo imperativo bloqueante** con la pila:

| Componente | Implementación elegida |
|---|---|
| HTTP runtime | `spring-boot-starter-web` + Tomcat |
| Controlador | `ResponseEntity<PriceResponse>` síncrono |
| Acceso a datos | Spring Data JPA + Hibernate |
| Driver de BD | JDBC H2 |
| Tipo de retorno del dominio | `Optional<Price>` / `Price` |

## Análisis de la decisión

### ¿Cuándo el modelo reactivo aporta valor real?

El modelo reactivo (WebFlux + R2DBC) aporta beneficios medibles cuando **concurren** las siguientes condiciones:

| Condición | ¿Aplica en este servicio? |
|---|---|
| Alta concurrencia sostenida (miles de req/s simultáneas) | ❌ No especificado en el enunciado |
| I/O lento: BD remota con latencia de red (ms) | ❌ H2 en memoria, latencia ~µs |
| Múltiples llamadas a servicios externos (fan-out) | ❌ Una única consulta a BD |
| Streaming de datos (grandes volúmenes, SSE, WebSocket) | ❌ Respuesta JSON puntual |
| Backpressure entre productor y consumidor | ❌ Un resultado por petición |

**Ninguna de estas condiciones aplica** en el contexto actual.

### ¿Qué añadiría el modelo reactivo aquí?

Adoptar WebFlux + R2DBC en este servicio introduciría:

1. **Complejidad accidental** en el código:
   - Los tipos de retorno `Mono<T>` / `Flux<T>` requieren composición funcional (`flatMap`, `map`, `switchIfEmpty`) donde hoy hay código lineal.
   - El `Optional<Price>` del caso de uso debería convertirse a `Mono<Price>`, propagando el modelo reactivo hasta el dominio o forzando conversiones en el adaptador.

2. **Mayor dificultad de debugging**:
   - Las trazas de pila reactivas (Project Reactor) son significativamente más difíciles de leer que las síncronas.
   - El modelo de threading (event-loop) requiere cuidado especial para no bloquear el loop con operaciones síncronas accidentales.

3. **Dependencias adicionales** sin uso real:
   - `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `io.r2dbc:r2dbc-h2`.
   - Mayor superficie de dependencias = mayor riesgo de vulnerabilidades.

4. **Complejidad en tests**:
   - MockMvc se sustituye por `WebTestClient`, con una API diferente.
   - Los tests unitarios requieren `StepVerifier` de Reactor Test.

### Relación coste-beneficio

```
Modelo imperativo:
  Coste: threads bloqueados durante la consulta (~microsegundos con H2)
  Beneficio: código lineal, fácil de entender, debuggear y testear

Modelo reactivo:
  Coste: complejidad de Reactor, debugging complejo, mayor curva de aprendizaje
  Beneficio: throughput superior con I/O lento (NO aplica con H2 en memoria)
```

La conclusión es clara: el modelo reactivo es la herramienta correcta para el problema incorrecto en este contexto.

## Ruta de migración futura

Si el servicio evoluciona hacia un escenario donde el modelo reactivo sí aporte valor (BD remota, alta concurrencia), la arquitectura hexagonal facilita la migración **sin tocar el dominio**:

1. Cambiar `spring-boot-starter-web` → `spring-boot-starter-webflux`.
2. Reemplazar `PricePersistenceAdapter` (JPA) por una nueva implementación de `FindPricePort` basada en R2DBC.
3. Adaptar `PriceController` para retornar `Mono<ResponseEntity<PriceResponse>>`.
4. El dominio (`Price`, `GetApplicablePriceUseCase`, `FindPricePort`) permanece **sin cambios**.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| WebFlux + R2DBC desde el inicio | Complejidad accidental sin beneficio operacional demostrable en este contexto. Viola el principio de YAGNI (You Aren't Gonna Need It). |
| Modelo virtual threads (Project Loom, Java 21+) | Alternativa interesante que combina modelo imperativo con no-bloqueo real. Descartada por requerir Java 21 (el entorno de CI disponible tiene JDK 17). Valorable en una revisión futura. |
| Modelo híbrido (WebFlux para HTTP, JPA para datos) | Antipatrón: mezclar un event-loop no bloqueante con drivers JDBC bloqueantes provoca bloqueo del event-loop y peor rendimiento que el modelo imperativo puro. |

## Consecuencias

**Positivas:**
- Código lineal, fácil de leer y mantener.
- Debugging sencillo con trazas de pila convencionales.
- Tests con MockMvc estándar y familiaridad del equipo.
- Sin dependencias adicionales.

**Negativas / trade-offs:**
- Si el servicio recibe carga masiva con BD remota, el modelo thread-per-request de Tomcat saturará el pool de threads antes que un event-loop reactivo.
- Adoptar virtual threads (Loom, Java 21) sería la evolución natural sin cambiar el modelo de programación.
