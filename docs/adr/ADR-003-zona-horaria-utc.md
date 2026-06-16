# ADR-003: Almacenamiento y operación de fechas en UTC

| Campo | Valor |
|-------|-------|
| **Estado** | Aceptado |
| **Fecha** | 2026-06-15 |
| **Decisores** | Equipo de arquitectura |
| **Sustituye a** | — |

---

## Contexto

Las fechas del enunciado (`START_DATE`, `END_DATE`) están expresadas en hora local de **Europe/Madrid**:
- **Verano (CEST, junio)**: UTC+2 → `2020-06-14 00:00:00 Madrid` = `2020-06-13 22:00:00 UTC`
- **Invierno (CET, diciembre)**: UTC+1 → `2020-12-31 23:59:59 Madrid` = `2020-12-31 22:59:59 UTC`

La aplicación puede ejecutarse en entornos con zona horaria diferente (servidor Linux UTC, contenedor Docker, máquina de desarrollador en Colombia UTC-5). Si las fechas se almacenan en hora local sin conversión explícita, el resultado de las consultas varía según el entorno de ejecución, lo que hace los tests no deterministas.

Además, `java.time.Instant` (tipo usado en el modelo de dominio y la API REST) representa siempre un instante absoluto en UTC, sin zona horaria asociada.

## Decisión

**Todas las fechas se almacenan y operan en UTC** en todos los componentes de la aplicación:

1. **`data.sql`**: los timestamps se convierten explícitamente a UTC antes de insertarlos, con comentarios que documentan la conversión:
   ```sql
   -- Price list 1: 2020-06-14 00:00:00 Madrid (UTC+2) → 2020-06-13 22:00:00 UTC
   INSERT INTO PRICES (..., START_DATE, ...) VALUES (..., '2020-06-13 22:00:00', ...);
   ```

2. **Hibernate**: configurado con `hibernate.jdbc.time_zone=UTC` para que la comunicación JDBC use UTC en ambos sentidos (lectura y escritura).

3. **Tests**: la JVM de test se lanza con `-Duser.timezone=UTC` para que H2 interprete los literales de `data.sql` en UTC, garantizando que el entorno de test sea independiente de la zona horaria del sistema operativo.

4. **API REST**: el parámetro `applicationDate` acepta exclusivamente formato ISO-8601 con designador UTC (`Z` o `+00:00`). Los clientes son responsables de convertir la hora local del usuario antes de llamar al endpoint.

5. **Modelo de dominio**: `java.time.Instant` (sin zona horaria) se usa en `Price`, `PriceEntity` y `PriceResponse`. Nunca `LocalDateTime`.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Almacenar en hora local Madrid (`LocalDateTime`) | No determinista: el resultado de las consultas depende de la JVM timezone. Tests fallarían en servidores UTC. |
| Almacenar en hora local con columna de timezone explícita | H2 no soporta `TIMESTAMP WITH TIME ZONE` con la misma semántica que PostgreSQL. Añade complejidad sin beneficio real para este scope. |
| Convertir en la capa de aplicación (no en BD) | Delega la responsabilidad de conversión al código Java en cada consulta, aumentando el riesgo de errores y dificultando la depuración. |

## Consecuencias

**Positivas:**
- Comportamiento 100% determinista independientemente del entorno (local, CI, Docker, producción).
- Los tests de integración son reproducibles en cualquier zona horaria.
- Alineado con la práctica estándar de la industria para sistemas distribuidos.
- El modelo de dominio con `Instant` expresa claramente que se trata de instantes absolutos, no horas locales.

**Negativas / trade-offs:**
- Los datos de `data.sql` no son legibles directamente en hora local de Madrid sin hacer la conversión mental (mitigado con comentarios explícitos en el SQL).
- Los clientes de la API deben gestionar la conversión de zona horaria en su lado antes de llamar al endpoint.

## Tabla de conversiones aplicadas

| PRICE_LIST | START Madrid | START UTC | END Madrid | END UTC |
|------------|-------------|-----------|------------|---------|
| 1 | 14/06 00:00 (UTC+2) | 13/06 22:00 | 31/12 23:59 (UTC+1) | 31/12 22:59 |
| 2 | 14/06 15:00 (UTC+2) | 14/06 13:00 | 14/06 18:30 (UTC+2) | 14/06 16:30 |
| 3 | 15/06 00:00 (UTC+2) | 14/06 22:00 | 15/06 11:00 (UTC+2) | 15/06 09:00 |
| 4 | 15/06 16:00 (UTC+2) | 15/06 14:00 | 31/12 23:59 (UTC+1) | 31/12 22:59 |
