# Price Service

Microservicio REST construido con **Spring Boot 3.5** que expone un endpoint para consultar el precio aplicable a un producto de una cadena comercial en una fecha y hora determinadas. Desarrollado como prueba técnica para el perfil de **Arquitecto Java** en Inditex.

---

## Índice

1. [Contexto y requisitos](#1-contexto-y-requisitos)
2. [Decisiones de arquitectura (ADRs)](#2-decisiones-de-arquitectura-adrs)
3. [Estructura del proyecto](#3-estructura-del-proyecto)
4. [Modelo de datos](#4-modelo-de-datos)
5. [API REST](#5-api-rest)
6. [Conversión de zona horaria](#6-conversión-de-zona-horaria)
7. [Tests](#7-tests)
8. [Colección Postman](#8-colección-postman)
9. [Requisitos previos](#9-requisitos-previos)
10. [Ejecución local](#10-ejecución-local)
11. [Despliegue con Docker](#11-despliegue-con-docker)
12. [Stack tecnológico](#12-stack-tecnológico)

---

## 1. Contexto y requisitos

### Enunciado

La tabla `PRICES` de la base de datos de comercio electrónico refleja el precio final (PVP) y la tarifa que aplica a un producto de una cadena entre unas fechas determinadas.

| BRAND_ID | START_DATE (Madrid) | END_DATE (Madrid) | PRICE_LIST | PRODUCT_ID | PRIORITY | PRICE | CURR |
|----------|---------------------|-------------------|------------|------------|----------|-------|------|
| 1 | 2020-06-14 00:00:00 | 2020-12-31 23:59:59 | 1 | 35455 | 0 | 35.50 | EUR |
| 1 | 2020-06-14 15:00:00 | 2020-06-14 18:30:00 | 2 | 35455 | 1 | 25.45 | EUR |
| 1 | 2020-06-15 00:00:00 | 2020-06-15 11:00:00 | 3 | 35455 | 1 | 30.50 | EUR |
| 1 | 2020-06-15 16:00:00 | 2020-12-31 23:59:59 | 4 | 35455 | 1 | 38.95 | EUR |

**Glosario de campos:**
- `BRAND_ID`: Identificador de la cadena del grupo (1 = ZARA).
- `START_DATE` / `END_DATE`: Rango de fechas en el que aplica la tarifa.
- `PRICE_LIST`: Identificador de la tarifa de precios aplicable.
- `PRODUCT_ID`: Identificador del producto.
- `PRIORITY`: Desambiguador. Si dos tarifas coinciden en rango de fechas, se aplica la de mayor valor numérico.
- `PRICE`: Precio final de venta.
- `CURR`: ISO de la moneda.

### Requisitos funcionales

- El endpoint acepta: `fecha de aplicación`, `identificador de producto`, `identificador de cadena`.
- Devuelve: `productId`, `brandId`, `priceList`, `startDate`, `endDate`, `amount`, `currency`.
- Base de datos en memoria (H2) inicializada con los datos del ejemplo.
- Cuando dos tarifas se solapan, se aplica la de **mayor prioridad**.

### Casos de test requeridos (hora Madrid, UTC+2 en junio)

| Test | Fecha/Hora Madrid | Tarifa esperada | Precio |
|------|-------------------|-----------------|--------|
| 1 | 14/06 10:00 | 1 | 35.50 EUR |
| 2 | 14/06 16:00 | 2 | 25.45 EUR |
| 3 | 14/06 21:00 | 1 | 35.50 EUR |
| 4 | 15/06 10:00 | 3 | 30.50 EUR |
| 5 | 16/06 21:00 | 4 | 38.95 EUR |

---

## 2. Decisiones de arquitectura (ADRs)

Todas las decisiones de arquitectura significativas están documentadas como **Architecture Decision Records (ADR)** en [`docs/adr/`](docs/adr/). El formato sigue la plantilla estándar de Michael Nygard: contexto → decisión → alternativas consideradas → consecuencias.

| ADR | Título | Estado |
|-----|--------|--------|
| [ADR-001](docs/adr/ADR-001-arquitectura-hexagonal.md) | Arquitectura Hexagonal (Ports & Adapters) | ✅ Aceptado |
| [ADR-002](docs/adr/ADR-002-modelo-dominio-record.md) | Modelo de dominio con Java Record (Value Object inmutable) | ✅ Aceptado |
| [ADR-003](docs/adr/ADR-003-zona-horaria-utc.md) | Almacenamiento y operación de fechas en UTC | ✅ Aceptado |
| [ADR-004](docs/adr/ADR-004-mappers-mapstruct.md) | Mappers de conversión separados con MapStruct | ✅ Aceptado |
| [ADR-005](docs/adr/ADR-005-modelo-imperativo-sobre-reactivo.md) | Modelo de programación imperativo sobre reactivo | ✅ Aceptado |
| [ADR-006](docs/adr/ADR-006-logica-seleccion-en-dominio.md) | Selección de precio por prioridad en el dominio | ✅ Aceptado |
| [ADR-007](docs/adr/ADR-007-cache-caffeine.md) | Caché en memoria con Caffeine | ✅ Aceptado |

### Resumen ejecutivo de decisiones

#### 2.1 Arquitectura Hexagonal → [ADR-001](docs/adr/ADR-001-arquitectura-hexagonal.md)

Se adopta arquitectura hexagonal con tres capas (dominio, aplicación, infraestructura) y dos puertos explícitos (`GetApplicablePriceUseCase` de entrada, `FindPricePort` de salida). El dominio no importa nada de Spring, JPA ni HTTP.

```
┌────────────────────────────────────────────────────────┐
│                     INFRASTRUCTURE                      │
│  ┌─────────────────┐        ┌────────────────────────┐ │
│  │  REST Adapter   │        │  Persistence Adapter   │ │
│  │  (entrypoint)   │        │  (JPA / H2)            │ │
│  └────────┬────────┘        └───────────┬────────────┘ │
│           │ Port IN                     │ Port OUT      │
├───────────┼─────────────────────────────┼──────────────┤
│           │         APPLICATION         │              │
│  ┌────────▼─────────────────────────────▼────────────┐ │
│  │           GetApplicablePriceUseCaseImpl            │ │
│  └───────────────────────┬────────────────────────────┘ │
├───────────────────────────┼──────────────────────────────┤
│                           │         DOMAIN               │
│  ┌───────────────────────▼────────────────────────────┐ │
│  │  Price (record) · PriceNotFoundException · Ports   │ │
│  └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

#### 2.2 Modelo de dominio con `record` → [ADR-002](docs/adr/ADR-002-modelo-dominio-record.md)

`Price` es un Java record: inmutable por diseño, con `equals`/`hashCode` por valor, sin anotaciones de framework. La entidad JPA (`PriceEntity`) existe por separado en la capa de infraestructura.

#### 2.3 Fechas en UTC → [ADR-003](docs/adr/ADR-003-zona-horaria-utc.md)

Todas las fechas se almacenan en UTC. Las del enunciado (hora Madrid) se convierten explícitamente en `data.sql`. Hibernate y los tests JVM se configuran con timezone UTC para garantizar comportamiento determinista en cualquier entorno.

#### 2.4 Selección de precio por prioridad en el dominio → [ADR-006](docs/adr/ADR-006-logica-seleccion-en-dominio.md)

La query JPQL filtra por rango de fechas y devuelve todos los candidatos (`List<Price>`). La selección del ganador — "ante solapamiento, gana la tarifa de mayor prioridad" — ocurre en `GetApplicablePriceUseCaseImpl` mediante un `stream().filter().max()`. El record `Price` encapsula la lógica de validación de fechas en `isApplicableAt(Instant date)`, habilitando tests unitarios puros sin base de datos.

#### 2.5 Mappers separados con MapStruct → [ADR-004](docs/adr/ADR-004-mappers-mapstruct.md)

Dos interfaces MapStruct independientes: `PricePersistenceMapper` (Entity→Domain) y `PriceRestMapper` (Domain→DTO). Código generado en compilación, sin reflexión en runtime.

#### 2.6 Manejo de errores centralizado

`GlobalExceptionHandler` con `@RestControllerAdvice` centraliza las respuestas de error con el record tipado `ErrorResponse`. Cubre `404`, `400` (validación, tipo, parámetro ausente) y `500` sin exponer detalles internos.

#### 2.7 Validación en el borde del sistema

`@NotNull` y `@Positive` en los parámetros del controlador con `spring-boot-starter-validation`. La validación ocurre en la frontera de infraestructura, nunca en el dominio.

#### 2.8 Caché en memoria con Caffeine → [ADR-007](docs/adr/ADR-007-cache-caffeine.md)

`@Cacheable` en `PricePersistenceAdapter.findCandidatePrices()` con **Caffeine** (TTL = 5 min, maxSize = 1000). Mitiga el impacto de retornar múltiples filas desde la BD (consecuencia de mover la selección por prioridad al dominio, ADR-006) y reduce la carga en patrones de acceso repetitivos. La abstracción `spring-boot-starter-cache` permite migrar a Redis en producción sin modificar código de negocio.

#### 2.9 Modelo de programación: imperativo sobre reactivo → [ADR-005](docs/adr/ADR-005-modelo-imperativo-sobre-reactivo.md)

**Decisión**: pila **bloqueante/imperativa** (`spring-boot-starter-web` + Tomcat + JPA + JDBC).

| Componente | Elegido | Alternativa reactiva descartada |
|---|---|---|
| HTTP runtime | `spring-boot-starter-web` (Tomcat) | `spring-boot-starter-webflux` (Netty) |
| Controlador | `ResponseEntity<T>` síncrono | `Mono<T>` / `Flux<T>` |
| Acceso a datos | Spring Data JPA + JDBC | Spring Data R2DBC |
| Driver BD | H2 JDBC | `io.r2dbc:r2dbc-h2` |

**Razones técnicas detalladas** (ver [ADR-005](docs/adr/ADR-005-modelo-imperativo-sobre-reactivo.md)):

 El modelo reactivo aporta valor medible únicamente cuando **todas** estas condiciones concurren:
- Alta concurrencia sostenida con cientos o miles de peticiones simultáneas.
- I/O lento: base de datos remota con latencia de red en milisegundos.
- Múltiples llamadas asíncronas a servicios externos (fan-out).
- Necesidad de backpressure entre productor y consumidor.
- Streaming de grandes volúmenes de datos.

En este servicio **ninguna aplica**: H2 opera en memoria (latencia de microsegundos), hay una única consulta por petición y no existe integración con sistemas externos. Introducir WebFlux añadiría:
- Complejidad accidental en el código (`flatMap`, `switchIfEmpty`, `StepVerifier` en tests).
- Trazas de pila reactivas difíciles de depurar.
- Riesgo de bloquear el event-loop accidentalmente.
- Mayor superficie de dependencias sin retorno operacional.

**Ruta de migración futura**: gracias al aislamiento de la arquitectura hexagonal, migrar a R2DBC en el futuro solo requiere reemplazar `PricePersistenceAdapter` y el controlador. El dominio (`Price`, `GetApplicablePriceUseCase`, `FindPricePort`) no se toca. Como alternativa intermedia, **Project Loom (virtual threads, Java 21)** permitiría escalar el modelo imperativo sin adoptar programación reactiva.

---

## 3. Estructura del proyecto

```
price-service/
├── docker/
│   └── Dockerfile                          # Build multi-stage (JDK builder + JRE runtime)
├── docs/
│   └── adr/                                # Architecture Decision Records
│       ├── ADR-001-arquitectura-hexagonal.md
│       ├── ADR-002-modelo-dominio-record.md
│       ├── ADR-003-zona-horaria-utc.md
│       ├── ADR-004-mappers-mapstruct.md
│       ├── ADR-005-modelo-imperativo-sobre-reactivo.md
│       ├── ADR-006-logica-seleccion-en-dominio.md
│       └── ADR-007-cache-caffeine.md
├── postman/
│   └── PriceService.postman_collection.json  # Colección Postman v2.1 (10 requests con tests)
├── spec/
│   └── TestJava2024_1.txt                  # Enunciado original
├── src/
│   ├── main/
│   │   ├── java/com/inditex/priceservice/
│   │   │   ├── PriceServiceApplication.java
│   │   │   ├── application/
│   │   │   │   └── usecase/
│   │   │   │       └── GetApplicablePriceUseCaseImpl.java   # Caso de uso
│   │   │   ├── domain/
│   │   │   │   ├── exception/
│   │   │   │   │   └── PriceNotFoundException.java
│   │   │   │   ├── model/
│   │   │   │   │   └── Price.java                          # Value Object (record)
│   │   │   │   └── port/
│   │   │   │       ├── in/
│   │   │   │       │   └── GetApplicablePriceUseCase.java  # Puerto de entrada
│   │   │   │       └── out/
│   │   │   │           └── FindPricePort.java              # Puerto de salida
│   │   │   └── infrastructure/
│   │   │       ├── adapter/
│   │   │       │   └── persistence/
│   │   │       │       ├── adapter/
│   │   │       │       │   └── PricePersistenceAdapter.java
│   │   │       │       ├── entity/
│   │   │       │       │   └── PriceEntity.java
│   │   │       │       ├── mapper/
│   │   │       │       │   └── PricePersistenceMapper.java
│   │   │       │       └── repository/
│   │   │       │           └── PriceJpaRepository.java
│   │   │       ├── config/
│   │   │       │   ├── BeanConfig.java                     # Wiring sin anotaciones en dominio
│   │   │       │   └── CacheConfig.java                    # Caffeine cache (TTL 5min, max 1000)
│   │   │       └── entrypoint/
│   │   │           └── rest/
│   │   │               ├── controller/
│   │   │               │   ├── PriceController.java
│   │   │               │   └── GlobalExceptionHandler.java
│   │   │               ├── dto/
│   │   │               │   ├── PriceQuery.java             # Input DTO (agrupa parámetros HTTP)
│   │   │               │   ├── PriceResponse.java
│   │   │               │   └── ErrorResponse.java
│   │   │               └── mapper/
│   │   │                   └── PriceRestMapper.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── schema.sql                                  # DDL de la tabla PRICES
│   │       └── data.sql                                    # Datos iniciales (fechas en UTC)
│   └── test/
│       └── java/com/inditex/priceservice/
│           ├── integration/
│           │   └── PriceControllerIT.java                  # 10 tests de integración
│           └── unit/
│               ├── GetApplicablePriceUseCaseTest.java      # Tests unitarios del caso de uso
│               └── PriceTest.java                          # Tests unitarios del dominio Price
└── build.gradle
```

---

## 4. Modelo de datos

### Tabla `PRICES`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `ID` | `BIGINT` (PK, auto) | Identificador interno |
| `BRAND_ID` | `BIGINT` | Cadena del grupo (1 = ZARA) |
| `START_DATE` | `TIMESTAMP` | Inicio de vigencia (UTC) |
| `END_DATE` | `TIMESTAMP` | Fin de vigencia (UTC) |
| `PRICE_LIST` | `INTEGER` | Identificador de tarifa |
| `PRODUCT_ID` | `BIGINT` | Código de producto |
| `PRIORITY` | `INTEGER` | Prioridad de aplicación (mayor = preferente) |
| `PRICE` | `DECIMAL(10,2)` | Precio final de venta |
| `CURRENCY` | `VARCHAR(3)` | ISO de moneda |

> **Nota sobre fechas**: el enunciado expresa las fechas en hora local de Madrid. La aplicación las almacena en **UTC** para garantizar consistencia. La conversión aplicada es: verano (CEST, UTC+2) resta 2 horas; invierno (CET, UTC+1) resta 1 hora.

---

## 5. API REST

### `GET /api/v1/prices`

Devuelve el precio aplicable para un producto y cadena en una fecha dada.

#### Parámetros de entrada

| Parámetro | Tipo | Obligatorio | Descripción | Ejemplo |
|-----------|------|-------------|-------------|---------|
| `applicationDate` | `Instant` (ISO-8601 UTC) | Sí | Fecha y hora de consulta | `2020-06-14T08:00:00Z` |
| `productId` | `Long` (positivo) | Sí | Identificador del producto | `35455` |
| `brandId` | `Long` (positivo) | Sí | Identificador de la cadena | `1` |

#### Respuesta exitosa `200 OK`

```json
{
  "productId": 35455,
  "brandId": 1,
  "priceList": 1,
  "startDate": "2020-06-13T22:00:00Z",
  "endDate": "2020-12-31T22:59:59Z",
  "amount": 35.50,
  "currency": "EUR"
}
```

#### Respuesta de error `404 Not Found`

```json
{
  "timestamp": "2020-06-14T08:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No applicable price found for product 35455 and brand 1"
}
```

#### Respuesta de error `400 Bad Request`

```json
{
  "timestamp": "2020-06-14T08:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Required parameter 'brandId' of type 'Long' is missing"
}
```

### Documentación interactiva

Una vez iniciado el servicio, la documentación OpenAPI/Swagger está disponible en:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`

---

## 6. Conversión de zona horaria

Las fechas del enunciado están en hora **Europe/Madrid**. La siguiente tabla muestra la conversión a UTC aplicada en `data.sql` y en los tests de integración:

| PRICE_LIST | START Madrid | START UTC | END Madrid | END UTC |
|------------|-------------|-----------|------------|---------|
| 1 | 14/06 00:00 (UTC+2) | 13/06 22:00 | 31/12 23:59 (UTC+1) | 31/12 22:59 |
| 2 | 14/06 15:00 (UTC+2) | 14/06 13:00 | 14/06 18:30 (UTC+2) | 14/06 16:30 |
| 3 | 15/06 00:00 (UTC+2) | 14/06 22:00 | 15/06 11:00 (UTC+2) | 15/06 09:00 |
| 4 | 15/06 16:00 (UTC+2) | 15/06 14:00 | 31/12 23:59 (UTC+1) | 31/12 22:59 |

Los **tests de integración** envían las fechas de consulta también en UTC (hora Madrid − 2h en junio):

| Test | Hora Madrid | UTC enviado en test | Resultado esperado |
|------|-------------|---------------------|--------------------|
| 1 | 14/06 10:00 | `2020-06-14T08:00:00Z` | Price list 1 – 35.50 EUR |
| 2 | 14/06 16:00 | `2020-06-14T14:00:00Z` | Price list 2 – 25.45 EUR |
| 3 | 14/06 21:00 | `2020-06-14T19:00:00Z` | Price list 1 – 35.50 EUR |
| 4 | 15/06 10:00 | `2020-06-15T08:00:00Z` | Price list 3 – 30.50 EUR |
| 5 | 16/06 21:00 | `2020-06-16T19:00:00Z` | Price list 4 – 38.95 EUR |

---

## 7. Tests

### Tests unitarios — `PriceTest`

Prueban el dominio puro sin ninguna dependencia de framework:
- `isApplicableAt` retorna `true` exactamente en el límite de inicio (inclusivo).
- `isApplicableAt` retorna `true` exactamente en el límite de fin (inclusivo).
- `isApplicableAt` retorna `true` para fechas dentro del rango.
- `isApplicableAt` retorna `false` para fechas anteriores al rango.
- `isApplicableAt` retorna `false` para fechas posteriores al rango.

### Tests unitarios — `GetApplicablePriceUseCaseTest`

Prueban el caso de uso en aislamiento total con **Mockito**, sin Spring context:
- Retorna el precio cuando hay un único candidato en rango.
- Retorna el precio de **mayor prioridad** cuando hay múltiples candidatos solapados.
- Lanza `PriceNotFoundException` cuando el puerto no devuelve candidatos.
- Lanza `PriceNotFoundException` cuando todos los candidatos quedan fuera del rango de fechas (filtro defensivo en dominio).

### Tests unitarios — `GlobalExceptionHandlerTest`

Prueban directamente el handler de excepciones sin levantar contexto Spring, cubriendo los 5 casos manejados:
- `PriceNotFoundException` → 404.
- `ConstraintViolationException` con violaciones → 400 con detalle del campo.
- `ConstraintViolationException` sin violaciones → 400 con mensaje de fallback.
- `MethodArgumentTypeMismatchException` → 400 indicando el parámetro y valor inválido.
- `MissingServletRequestParameterException` → 400 indicando el parámetro faltante.
- `Exception` genérica → 500 sin exponer detalles internos.

### Tests de integración — `PriceControllerIT`

Levantan el contexto completo de Spring Boot con base de datos H2 en memoria y validan el endpoint end-to-end con **MockMvc**:

| # | Escenario | HTTP esperado |
|---|-----------|--------------|
| 1 | 14/06 10:00 Madrid → Price list 1, 35.50 EUR | 200 |
| 2 | 14/06 16:00 Madrid → Price list 2, 25.45 EUR | 200 |
| 3 | 14/06 21:00 Madrid → Price list 1, 35.50 EUR | 200 |
| 4 | 15/06 10:00 Madrid → Price list 3, 30.50 EUR | 200 |
| 5 | 16/06 21:00 Madrid → Price list 4, 38.95 EUR | 200 |
| 6 | Producto/brand sin precio aplicable | 404 |
| 7 | Parámetro obligatorio ausente (`brandId`) | 400 |
| 8 | Formato de fecha inválido | 400 |
| 9 | `productId = 0` (viola `@Positive`) | 400 |
| 10 | `brandId = -1` (viola `@Positive`) | 400 |

### Cobertura de código (JaCoCo)

**Resultado actual: 98% de instrucciones cubiertas** sobre el código de negocio relevante.

| Paquete | Instrucciones cubiertas | Branches cubiertas |
|---------|------------------------|--------------------|
| `application.usecase` | **100 %** | n/a |
| `domain.model` | **100 %** | n/a |
| `domain.exception` | **100 %** | n/a |
| `domain.port` | **100 %** | n/a |
| `infrastructure.config` | **100 %** | n/a |
| `infrastructure.adapter.persistence.adapter` | **100 %** | n/a |
| `infrastructure.entrypoint.rest.dto` | **100 %** | n/a |
| `infrastructure.entrypoint.rest.controller` | **97 %** | 50 % (*) |
| **Total (clases relevantes)** | **98 %** | — |

> (*) El único branch no cubierto corresponde al path `orElse` del stream vacío en `handleConstraintViolationException`, que se activa cuando `ConstraintViolationException` no contiene objetos `ConstraintViolation`. Este path es teóricamente posible según la API de Jakarta, pero no lo genera ningún mecanismo real del framework en tiempo de ejecución, por lo que cubrir ese branch requeriría un mock artificial que no refleja comportamiento productivo real.

#### Exclusiones aplicadas en JaCoCo

Dos categorías de clases se excluyen explícitamente del cálculo de cobertura:

| Clase excluida | Patrón | Motivo |
|----------------|--------|--------|
| `PriceServiceApplication` | `**/PriceServiceApplication.class` | Clase de arranque de Spring Boot. Solo contiene el método `main` que delega en `SpringApplication.run()`. No hay lógica de negocio testable; el comportamiento se valida de forma implícita en cada test de integración al levantar el contexto. |
| `*MapperImpl` | `**/*MapperImpl.class` | Implementaciones generadas automáticamente por **MapStruct** en tiempo de compilación (`PricePersistenceMapperImpl`, `PriceRestMapperImpl`). El código generado no es de responsabilidad del equipo de desarrollo y siempre sería cubierto indirectamente por los tests de integración si se incluyera. Excluirlo evita distorsionar la métrica con código boilerplate. |

Para generar el informe:

```bash
# Linux / macOS
./gradlew test jacocoTestReport

# Windows
.\gradlew.bat test jacocoTestReport
```

Informe disponible en: `build/reports/jacoco/test/html/index.html`

Para verificar el umbral mínimo de cobertura (80%) como gate de calidad:

```bash
.\gradlew.bat test jacocoTestReport jacocoTestCoverageVerification
```

---

## 8. Colección Postman

El directorio [`postman/`](postman/) contiene la colección **Postman v2.1** lista para importar y ejecutar contra el servicio en local o en Docker.

### Archivo

| Archivo | Descripción                                  |
|---------|----------------------------------------------|
| [`postman/PriceService.postman_collection.json`](postman/PriceService.postman_collection.json) | Colección con 10 requests y tests automáticos |

### Cómo importar

1. Abrir **Postman** → **Import** → seleccionar `postman/PriceService.postman_collection.json`.
2. Ajustar la variable de colección `base_url` si el servicio no corre en `http://localhost:8080`.

### Escenarios incluidos

| # | Descripción | Fecha UTC | HTTP | `amount` esperado |
|---|-------------|-----------|------|-------------------|
| Test 1 | 10:00h 14/Jun → PriceList 1 | `2020-06-14T08:00:00Z` | 200 | 35.50 EUR |
| Test 2 | 16:00h 14/Jun → PriceList 2 | `2020-06-14T14:00:00Z` | 200 | 25.45 EUR |
| Test 3 | 21:00h 14/Jun → PriceList 1 | `2020-06-14T19:00:00Z` | 200 | 35.50 EUR |
| Test 4 | 10:00h 15/Jun → PriceList 3 | `2020-06-15T08:00:00Z` | 200 | 30.50 EUR |
| Test 5 | 21:00h 16/Jun → PriceList 4 | `2020-06-16T19:00:00Z` | 200 | 38.95 EUR |
| Test 6 | Límite exacto inicio PriceList 2 | `2020-06-14T13:00:00Z` | 200 | 25.45 EUR |
| Test 7 | Límite exacto fin PriceList 2 | `2020-06-14T16:30:00Z` | 200 | 25.45 EUR |
| Test 8 | Sin precio aplicable (año 2000) | `2000-01-01T00:00:00Z` | 404 | — |
| Test 9 | Parámetro `applicationDate` ausente | — | 400 | — |
| Test 10 | Formato de fecha inválido | `not-a-date` | 400 | — |

Cada request incluye **tests automáticos de Postman** que verifican el código HTTP, los campos del body y los valores esperados. Para ejecutar toda la colección de una vez se puede usar **Newman** (CLI de Postman):

```bash
npm install -g newman
newman run postman/PriceService.postman_collection.json
```

---

## 9. Requisitos previos

### Ejecución local (sin Docker)

| Requisito | Versión mínima |
|-----------|---------------|
| JDK | 17 |
| Gradle | 9.x (incluido via Gradle Wrapper) |

> No se requiere instalación de Gradle; el proyecto incluye `gradlew` / `gradlew.bat`.

### Ejecución con Docker

| Requisito | Versión mínima |
|-----------|---------------|
| Docker Engine | 20.x |

---

## 10. Ejecución local

### Clonar y compilar

```bash
git clone <repositorio>
cd price-service
```

### Ejecutar tests

```bash
# Linux / macOS
./gradlew test

# Windows
.\gradlew.bat test
```

### Iniciar el servicio

```bash
# Linux / macOS
./gradlew bootRun

# Windows
.\gradlew.bat bootRun
```

El servicio arranca en `http://localhost:8080`.

### Verificación rápida

```bash
curl "http://localhost:8080/api/v1/prices?applicationDate=2020-06-14T08:00:00Z&productId=35455&brandId=1"
```

Respuesta esperada:
```json
{
  "productId": 35455,
  "brandId": 1,
  "priceList": 1,
  "startDate": "2020-06-13T22:00:00Z",
  "endDate": "2020-12-31T22:59:59Z",
  "amount": 35.50,
  "currency": "EUR"
}
```

### Consola H2 (desarrollo)

Disponible en `http://localhost:8080/h2-console` con:
- **JDBC URL**: `jdbc:h2:mem:pricesdb`
- **User**: `sa`
- **Password**: *(vacío)*

---

## 11. Despliegue con Docker

### Build de la imagen

El `Dockerfile` usa un **build multi-stage** para minimizar el tamaño de la imagen final:
1. **Stage builder**: compila y empaqueta el JAR con JDK 17 Alpine.
2. **Stage runtime**: ejecuta el JAR con JRE 17 Alpine (imagen más ligera y segura).

El proceso de runtime corre con un **usuario no-root** (`appuser`) por seguridad.

```bash
# Construir la imagen desde la raíz del proyecto
docker build -f docker/Dockerfile -t price-service:1.0.0 .
```

### Ejecutar el contenedor

```bash
docker run -p 8080:8080 price-service:1.0.0
```

### Variables de entorno disponibles

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `JAVA_OPTS` | `-Duser.timezone=UTC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` | Opciones JVM |
| `SERVER_PORT` | `8080` | Puerto del servidor |

Ejemplo sobreescribiendo el puerto:

```bash
docker run -p 9090:9090 -e SERVER_PORT=9090 price-service:1.0.0
```

### Docker Compose (opcional)

```yaml
services:
  price-service:
    build:
      context: .
      dockerfile: docker/Dockerfile
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Duser.timezone=UTC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

```bash
docker compose up
```

---

## 12. Stack tecnológico

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.5.0 | Framework principal |
| Spring Data JPA | (managed) | Acceso a datos |
| Spring Validation | (managed) | Validación de inputs |
| H2 Database | (managed) | Base de datos en memoria |
| MapStruct | 1.6.3 | Mapeo entre capas sin reflexión |
| Lombok | (managed) | Reducción de boilerplate |
| Spring Cache | (managed) | Abstracción de caché (`@Cacheable`) |
| Caffeine | (managed) | Proveedor de caché en memoria (TTL, LFU) |
| SpringDoc OpenAPI | 2.8.6 | Documentación Swagger automática |
| JaCoCo | (managed) | Cobertura de código |
| Mockito | (managed) | Mocks en tests unitarios |
| Gradle | 9.x | Build tool |
| Docker | 20.x+ | Contenedorización |
| eclipse-temurin | 17-alpine | Imagen base Docker |
