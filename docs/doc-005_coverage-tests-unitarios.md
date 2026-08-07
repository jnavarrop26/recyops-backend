# Coverage de los tests unitarios de la capa service

**Tema:** testing

> Snapshot de cobertura tras implementar los 13 `*ServiceImplTest` (JUnit 5 +
> Mockito + AssertJ) que antes eran clases vacías en `src/test/java/com/recyops/api/unit/*/service/`.
> Generado con JaCoCo (`jacoco-maven-plugin`, agregado en `pom.xml`, atado a la
> fase `test` — no requiere Docker/Testcontainers).

## Cómo regenerarlo

```bash
./mvnw test
# reporte HTML navegable:
open target/site/jacoco/index.html
# datos crudos:
target/site/jacoco/jacoco.csv
target/site/jacoco/jacoco.xml
```

`target/` no se versiona; este archivo es el registro persistente del
resultado en el momento en que se escribió.

## Resultado: 179 tests, 0 fallos

## Cobertura por clase (`*ServiceImpl`)

| Clase | Instrucciones | Líneas | Branches | Tests |
|---|---:|---:|---:|---:|
| BodegaServiceImpl | 100.0% | 100.0% | 0.0% | 6 |
| TareaServiceImpl | 100.0% | 100.0% | 96.2% | 28 |
| PlataformaServiceImpl | 95.9% | 93.9% | 85.0% | 8 |
| UsuarioServiceImpl | 100.0% | 100.0% | 95.0% | 21 |
| DashboardServiceImpl | 100.0% | 100.0% | 100.0% | 6 |
| EntregaServiceImpl | 100.0% | 100.0% | 100.0% | 18 |
| IngresoServiceImpl | 99.0% | 100.0% | 89.3% | 21 |
| ProveedorServiceImpl | 100.0% | 100.0% | 75.0% | 8 |
| ConvenioServiceImpl | 97.8% | 100.0% | 100.0% | 9 |
| LogServiceImpl | 95.2% | 93.7% | 81.8% | 5 |
| InventarioServiceImpl | 100.0% | 100.0% | 100.0% | 26 |
| MaterialServiceImpl | 96.4% | 100.0% | 25.0% | 11 |
| AuthServiceImpl | 94.2% | 97.1% | 81.2% | 12 |
| **Total (13 clases)** | **98.4%** | **98.8%** | **89.9%** | **179** |

Cobertura del proyecto completo (incluye controllers, DTOs, config,
`ClienteSupabaseAdmin`, `GeneradorReciboPdf`, etc. — fuera del alcance de esta
tanda): **69.1% instrucciones**.

## Gaps conocidos, pendientes de una vuelta futura

- **`MaterialServiceImpl` (25% branch)**: línea/instrucción ~100%, pero varias
  ramas de null-check quedan sin ejercitar.
- **`BodegaServiceImpl` (0% branch)**: no tiene branches reales que cubrir con
  los casos actuales (métodos mayormente lineales); revisar si vale la pena
  añadir casos límite.
- **`LogServiceImpl`**: `LogServiceImpl` no recibe la ruta del archivo de log
  por inyección de dependencia, la tiene *hardcodeada*. El test hace backup y
  restore de `logs/recyops-api.log` alrededor de cada caso (bloque `finally`)
  para no perder el log real — documentado en el javadoc del test. Sería más
  limpio si la clase aceptara la ruta como dependencia inyectable.
- **Fuera de alcance**: controllers, DTOs, `ClienteSupabaseAdmin`,
  `GeneradorReciboPdf` — no tenían stub de test previo y no se tocaron en esta
  tanda.
