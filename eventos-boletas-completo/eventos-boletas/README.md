# 🎟️ Eventos y Boletas — MVP

**Proyecto Ingeniería de Software II — Mayo 2026-1**  
**Universidad del Quindío — Facultad de Ingeniería**  
**Escenario 7A:** Comprar Boleto (Cliente, Evento, Zona, Boleto, Pago)

**Integrantes:**
- Sara María Otero Echeverri
- Sara Valentina Acosta Burbano — 1080044648
- Jennifer Andrea Cortés Chavarro — 1090273061

---

## 🏗️ Arquitectura

```
src/
├── main/java/co/uniquindio/eventoboletas/
│   │
│   ├── EventosBoletasApplication.java
│   │
│   ├── domain/                                      ← Núcleo puro (sin dependencias externas)
│   │   ├── entities/
│   │   │   ├── Cliente.java                         ← RN-01, RN-06 · Factory Method + Rich Domain
│   │   │   ├── Evento.java                          ← RN-02
│   │   │   ├── Zona.java                            ← RN-03, RN-04
│   │   │   ├── Boleto.java                          ← RN-05 · emitir() solo con pago APROBADO
│   │   │   └── Pago.java
│   │   ├── enums/
│   │   │   ├── EstadoCliente.java                   ← ACTIVO · INACTIVO · BLOQUEADO
│   │   │   ├── EstadoEvento.java                    ← ACTIVO · CANCELADO · AGOTADO
│   │   │   ├── EstadoBoleto.java                    ← PAGADO · ANULADO
│   │   │   ├── EstadoPago.java                      ← APROBADO · PENDIENTE · RECHAZADO
│   │   │   └── MetodoPago.java                      ← EFECTIVO · TARJETA
│   │   ├── repositories/                            ← Puertos de salida (interfaces — Patrón DIP)
│   │   │   ├── ClienteRepository.java
│   │   │   ├── EventoRepository.java
│   │   │   ├── ZonaRepository.java
│   │   │   └── BoletoRepository.java
│   │   └── exceptions/
│   │       ├── ReglaDeNegocioException.java         ← Lanza 422
│   │       └── EntidadNoEncontradaException.java    ← Lanza 404
│   │
│   ├── application/                                 ← Casos de uso (orquestación)
│   │   ├── usecases/
│   │   │   ├── boleto/
│   │   │   │   ├── ComprarBoletoUseCase.java        ← CU-01 · Facade + Template Method
│   │   │   │   └── BuscarParaTransaccionUseCase.java
│   │   │   └── cliente/
│   │   │       └── GestionarClienteUseCase.java     ← CU-02 · CRUD completo
│   │   └── dtos/
│   │       ├── request/
│   │       │   ├── ComprarBoletoRequest.java        ← clienteId, eventoId, zonaId, metodoPago
│   │       │   └── ClienteRequest.java
│   │       └── response/
│   │           ├── BoletoResponse.java
│   │           ├── ClienteResponse.java
│   │           ├── EventoResponse.java
│   │           ├── ZonaResponse.java
│   │           ├── PagoResponse.java
│   │           └── PagedResponse.java
│   │
│   └── infrastructure/                              ← Adaptadores e implementaciones
│       ├── adapters/                                ← Patrón Adapter (implementan puertos)
│       │   ├── ClienteRepositoryAdapter.java
│       │   ├── EventoRepositoryAdapter.java
│       │   ├── ZonaRepositoryAdapter.java
│       │   └── BoletoRepositoryAdapter.java
│       ├── persistence/
│       │   ├── entities/                            ← Entidades JPA (@Entity)
│       │   │   ├── ClienteJpa.java
│       │   │   ├── EventoJpa.java
│       │   │   ├── ZonaJpa.java
│       │   │   ├── BoletoJpa.java
│       │   │   └── PagoJpa.java
│       │   └── repositories/                        ← Spring Data JPA
│       │       ├── ClienteJpaRepository.java
│       │       ├── EventoJpaRepository.java
│       │       ├── ZonaJpaRepository.java
│       │       └── BoletoJpaRepository.java
│       ├── config/
│       │   ├── DataSeeder.java                      ← Patrón Observer · carga datos al arranque
│       │   └── OpenApiConfig.java
│       └── web/
│           ├── controllers/
│           │   ├── BoletoController.java            ← POST /api/transaccion/comprar-boleto
│           │   └── ClienteController.java           ← /api/clientes/**
│           └── handlers/
│               └── GlobalExceptionHandler.java      ← Patrón Chain of Responsibility
│
└── test/java/co/uniquindio/eventoboletas/
    │
    ├── domain/
    │   └── DomainRulesTest.java                     ← RN-01..RN-06 sin Spring ni mocks
    │                                                   · 11 tests · solo instancia entidades
    │
    ├── usecases/
    │   ├── boleto/
    │   │   └── ComprarBoletoUseCaseTest.java         ← CA-01..CA-05 · Mockito
    │   │                                               · flujo feliz + 4 alternos bloqueantes
    │   └── cliente/
    │       └── GestionarClienteUseCaseTest.java      ← CA-06..CA-10 · Mockito
    │                                                   · CRUD + RN-06 + RN-07
    │
    └── integration/
        └── ComprarBoletoIntegrationTest.java         ← IT-01..IT-06 · MockMvc + H2 real
                                                        · Spring Boot completo de extremo a extremo
```

### Patrones de diseño aplicados

| Patrón | Categoría | Dónde se usa |
|--------|-----------|--------------|
| **Factory Method** | Creacional | `Cliente.crear()`, `Zona.crear()`, `Boleto.emitir()`, `Pago.crear()` |
| **Adapter** | Estructural | `ClienteRepositoryAdapter`, `EventoRepositoryAdapter`, `ZonaRepositoryAdapter`, `BoletoRepositoryAdapter` |
| **Facade** | Estructural | `ComprarBoletoUseCase` orquesta múltiples repos y entidades en una operación |
| **Repository** | Arquitectónico | Puertos de dominio + adaptadores JPA |
| **Observer** | Comportamiento | `DataSeeder implements ApplicationRunner` |
| **Chain of Responsibility** | Comportamiento | `GlobalExceptionHandler` (@RestControllerAdvice) |
| **Template Method** | Comportamiento | Flujo P1→P10 del CU-01 en `ComprarBoletoUseCase` |

### Principios SOLID aplicados

| Principio | Aplicación |
|-----------|------------|
| **SRP** | Un use case = una responsabilidad. Entidades protegen sus propias invariantes. |
| **OCP** | Reglas de negocio extensibles sin modificar casos de uso. |
| **LSP** | Los adaptadores son sustituibles por cualquier otra implementación del puerto. |
| **ISP** | Repositorios de dominio con métodos mínimos según lo que cada use case necesita. |
| **DIP** | Domain y Application dependen de interfaces, nunca de JPA o Spring. |

---

## 🚀 Cómo correr el proyecto

### Prerrequisitos

| Herramienta | Versión mínima | Cómo verificar |
|-------------|----------------|----------------|
| Java (JDK)  | 17             | `java -version` |
| Maven       | 3.8            | `mvn -version` |

> **No se requiere instalar ninguna base de datos.** El proyecto usa **H2** en memoria, incluida automáticamente con Spring Boot.

---

### Paso 1 — Descomprimir el proyecto

```bash
unzip eventos-boletas-completo.zip
cd eventos-boletas-completo/eventos-boletas
```

---

### Paso 2 — Compilar y ejecutar

Desde la carpeta donde está el `pom.xml`:

```bash
mvn spring-boot:run
```

La primera vez Maven descarga las dependencias automáticamente (requiere internet). Cuando veas esto en consola, el servidor está listo:

```
INFO  DataSeeder      : 12 clientes cargados
INFO  DataSeeder      : 4 eventos y 7 zonas cargados
INFO  TomcatWebServer : Tomcat started on port(s): 8080
```



---

### Paso 3 — Acceder a la aplicación

| Recurso | URL |
|---------|-----|
| 🖥️ Interfaz web (frontend) | Abrir `frontend-eventos-boletas-v2.html` en el navegador |
| 📋 Swagger UI (probar API) | http://localhost:8080/swagger-ui.html |
| 📄 API Docs (JSON) | http://localhost:8080/api-docs |
| 🗄️ Consola H2 (base de datos) | http://localhost:8080/h2-console |

---

### Paso 4 — Conectarse a la consola H2 (opcional)

Abrir http://localhost:8080/h2-console y usar exactamente estos datos:

| Campo | Valor |
|-------|-------|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:eventosdb` |
| User Name | `sa` |
| Password | *(dejar en blanco)* |

> ⚠️ Los datos se cargan al arrancar via `DataSeeder` y se pierden al detener el proyecto.

---

### Paso 5 — Correr los tests

```bash
# Todos los tests
mvn test
```

---

## 👤 Usuarios demo (cargados automáticamente)

### Clientes para flujo feliz — estado ACTIVO

| ID | Nombre | Email | Documento |
|----|--------|-------|-----------|
| 1 | Ana Gómez | ana.gomez@email.com | 1000001 |
| 2 | Carlos Pérez | carlos.perez@email.com | 1000002 |
| 3 | Valentina Torres | vale.torres@email.com | 1000003 |
| 4 | Andrés López | andres.lopez@email.com | 1000004 |
| 5 | María Ramírez | maria.ramirez@email.com | 1000005 |
| 6 | Felipe Castro | felipe.castro@email.com | 1000006 |
| 7 | Laura Morales | laura.morales@email.com | 1000007 |
| 8 | Juan Herrera | juan.herrera@email.com | 1000008 |
| 11 | Lucía Navarro | lucia.nav@email.com | 1000011 |
| 12 | Sergio Medina | sergio.med@email.com | 1000012 |

### Clientes para flujos de error — RN-01

| ID | Nombre | Email | Estado | Resultado esperado |
|----|--------|-------|--------|--------------------|
| 9 | Sofía Vargas | sofia.vargas@email.com | INACTIVO | 422 — no habilitado |
| 10 | Miguel Jiménez | miguel.jm@email.com | BLOQUEADO | 422 — no habilitado |

---

## 🎪 Eventos y zonas demo (cargados automáticamente)

| ID | Nombre | Estado | Zonas (ID — nombre — precio final) |
|----|--------|--------|------------------------------------|
| 1 | Festival Latinoamericano de Música | ACTIVO | 1-VIP $385.000 · 2-General $126.000 · 3-Palco $575.000 |
| 2 | UniQuindío Tech Conference 2026 | ACTIVO | 4-Premium $86.400 · 5-Libre $30.000 |
| 3 | Hamlet — Compañía Nacional de Teatro | ACTIVO | 6-Butaca $99.750 · **7-Galería $0 (cupo=0)** |
| 4 | Concierto Cancelado | CANCELADO | — (para probar RN-02) |

> Los precios finales se calculan en el servidor: `precioBase × (1 + recargoPorcentaje)`.

---

## 🔗 Endpoints REST

### CU-02 — CRUD Clientes (`/api/clientes`)

| Método | Endpoint | Status éxito | Descripción |
|--------|----------|-------------|-------------|
| GET | `/api/clientes/listar?pagina=0&tamano=10` | 200 | Listar paginado |
| GET | `/api/clientes/{id}/buscar-por-id` | 200 | Obtener por ID |
| POST | `/api/clientes/crear` | 201 | Crear cliente |
| PUT | `/api/clientes/{id}/editar` | 200 | Editar cliente |
| DELETE | `/api/clientes/{id}/eliminar` | 204 | Eliminar cliente |



### CU-01 — Transacción Comprar Boleto (`/api/transaccion`)

| Método | Endpoint | Status éxito | Descripción |
|--------|----------|-------------|-------------|
| GET | `/api/transaccion/clientes/buscar?q=ana` | 200 | Buscar cliente por nombre/documento |
| GET | `/api/transaccion/eventos/listar-activos` | 200 | Eventos activos con zonas y precios |
| POST | `/api/transaccion/comprar-boleto` | 200 | Ejecutar compra completa |


Valores válidos para `metodoPago`: `EFECTIVO`, `TARJETA`

---

## 🧪 Casos de aceptación (Given-When-Then)

Los tests están en `src/test/java/co/uniquindio/eventoboletas/` organizados así:

```
test/
├── domain/
│   └── DomainRulesTest.java              ← RN-01 a RN-06 — sin Spring, sin mocks
├── usecases/
│   ├── boleto/
│   │   └── ComprarBoletoUseCaseTest.java  ← CA-01 a CA-05 — unitarios con Mockito
│   └── cliente/
│       └── GestionarClienteUseCaseTest.java ← CA-06 a CA-10 — unitarios con Mockito
└── integration/
    └── ComprarBoletoIntegrationTest.java  ← IT-01 a IT-06 — HTTP real con MockMvc + H2
```

---

### CA-01 — Flujo feliz: compra exitosa

```
Given: Cliente id=1 (Ana Gómez) con estado ACTIVO
       Evento id=1 (Festival de Música) con estado ACTIVO
       Zona id=2 (General) con cupoDisponible=200
When:  POST /api/transaccion/comprar-boleto
       { clienteId:1, eventoId:1, zonaId:2, metodoPago:"EFECTIVO" }
Then:  200 OK
       codigoQR no vacío en el body
       precioFinal = 126.000,00  (120.000 × 1,05)
       estadoBoleto = PAGADO
       cupoDisponible de la zona decrementó en 1
```

### CA-02 — Flujo alterno bloqueante: cliente no habilitado (RN-01)

```
Given: Cliente id=10 (Miguel Jiménez) con estado BLOQUEADO
When:  POST /api/transaccion/comprar-boleto con clienteId=10
Then:  422 Unprocessable Entity
       mensaje: "El cliente no está habilitado para comprar. Estado actual: BLOQUEADO"
       No se genera ningún boleto ni pago
       Ningún repositorio posterior fue consultado (transacción abortada en paso 1)
```

> Este es el **flujo alterno bloqueante principal**: la transacción se interrumpe en el primer paso de validación aunque evento y zona estén disponibles.

### CA-03 — Zona agotada (RN-03)

```
Given: Cliente id=1 (ACTIVO), Evento id=3 (Teatro, ACTIVO)
       Zona id=7 (Galería) con cupoDisponible=0
When:  POST /api/transaccion/comprar-boleto con zonaId=7
Then:  422 Unprocessable Entity
       mensaje: "No hay boletos disponibles para esta zona"
```

### CA-04 — Evento cancelado (RN-02)

```
Given: Cliente id=1 (ACTIVO)
       Evento id=4 (Concierto Cancelado) con estado CANCELADO
When:  POST /api/transaccion/comprar-boleto con eventoId=4
Then:  422 Unprocessable Entity
       mensaje: "Este evento no está disponible para la venta. Estado: CANCELADO"
```

### CA-05 — Eliminar cliente con boletos activos (RN-06)

```
Given: Cliente que ya tiene al menos un boleto en estado PAGADO
When:  DELETE /api/clientes/{id}/eliminar
Then:  422 Unprocessable Entity
       mensaje: "No se puede eliminar un cliente con boletos activos"
       El cliente sigue existiendo en la base de datos
```

### CA-06 — Email duplicado al crear (RN-07)

```
Given: Ya existe un cliente con email "ana.gomez@email.com"
When:  POST /api/clientes/crear con ese mismo email
Then:  422 Unprocessable Entity
       mensaje: "Ya existe un cliente con este email"
```

---

## 🛠️ Stack tecnológico

| Componente | Tecnología |
|------------|-----------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.2 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | H2 (en memoria, sin instalación) |
| Documentación | Springdoc OpenAPI 2 (Swagger UI) |
| Mapeo | Lombok + Records |
| Tests | JUnit 5 + Mockito + AssertJ + MockMvc |
| Build | Maven 3.8+ |

---