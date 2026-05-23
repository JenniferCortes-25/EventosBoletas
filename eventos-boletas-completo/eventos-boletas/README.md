# 🎟️ Eventos y Boletas

**Proyecto Ingeniería de Software II — Mayo 2026-1**  
**Universidad del Quindío — Facultad de Ingeniería**  
**Escenario 7A:** Comprar Boleto (Cliente, Evento, Zona, Boleto, Pago)

**Integrantes:**
- Sara María Otero Echeverri — 1091203101
- Sara Valentina Acosta Burbano — 1080044648
- Jennifer Andrea Cortés Chavarro — 1090273061

---

## 🚀 Cómo correr el proyecto

### Prerrequisitos

| Herramienta | Versión mínima | Verificar con |
|-------------|----------------|---------------|
| Java (JDK)  | 17             | `java -version` |
| Maven       | 3.8            | `mvn -version` |

> No se requiere instalar ninguna base de datos. El proyecto usa **H2 en memoria**, incluida automáticamente.

### Pasos

**1. Entrar a la carpeta del proyecto**
```bash
cd eventos-boletas
```

**2. Levantar el backend**
```bash
mvn spring-boot:run
```
La primera vez Maven descarga dependencias (requiere internet). El servidor está listo cuando aparezca:
```
INFO  ClienteSeeder   : 12 clientes cargados.
INFO  EventoSeeder    : 4 eventos cargados.
INFO  TomcatWebServer : Tomcat started on port(s): 8080
```

**3. Abrir el frontend**

Abrir el archivo `frontend-eventos-boletas-v6.html` directamente en el navegador.

**4. Recursos disponibles**

| Recurso | URL |
|---------|-----|
| Swagger UI (probar API) | http://localhost:8080/swagger-ui.html |
| Consola H2 (base de datos) | http://localhost:8080/h2-console |
| API Docs JSON | http://localhost:8080/api-docs |

**Datos para la consola H2:**

| Campo | Valor |
|-------|-------|
| JDBC URL | `jdbc:h2:mem:eventosdb` |
| User Name | `sa` |
| Password | *(dejar vacío)* |

### Correr los tests
```bash
mvn test
```

---

## 👤 Usuarios demo

> Cargados automáticamente al arrancar. Los datos se pierden al detener el servidor.

### Clientes — estado ACTIVO (flujo feliz)

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

### Clientes — flujos de error (RN-01)

| ID | Nombre | Estado | Comportamiento esperado |
|----|--------|--------|------------------------|
| 9 | Sofía Vargas | INACTIVO | 422 — cliente no habilitado |
| 10 | Miguel Jiménez | BLOQUEADO | 422 — cliente no habilitado |

### Eventos y zonas

| ID Evento | Nombre | Estado | ID Zona | Zona | Precio base | Recargo zona |
|-----------|--------|--------|---------|------|-------------|--------------|
| 1 | Festival Latinoamericano de Música | ACTIVO | 1 | VIP | $350.000 | 10% |
| 1 | Festival Latinoamericano de Música | ACTIVO | 2 | General | $120.000 | 5% |
| 1 | Festival Latinoamericano de Música | ACTIVO | 3 | Palco | $500.000 | 15% |
| 2 | UniQuindío Tech Conference 2026 | ACTIVO | 4 | Premium | $80.000 | 8% |
| 2 | UniQuindío Tech Conference 2026 | ACTIVO | 5 | Libre | $30.000 | 0% |
| 3 | Hamlet — Compañía Nacional de Teatro | ACTIVO | 6 | Butaca Preferencial | $95.000 | 5% |
| 3 | Hamlet — Compañía Nacional de Teatro | ACTIVO | 7 | Galería *(cupo=0)* | $40.000 | 0% |
| 4 | Concierto Cancelado | CANCELADO | — | — | — | — |

### Recargos por método de pago

| Método | Recargo | Probabilidad de aprobación | Ejemplo sobre $100.000 |
|--------|---------|---------------------------|------------------------|
| EFECTIVO | 0% | 100% (siempre aprobado) | $100.000 |
| PSE | 1% | 80% | $101.000 |
| TARJETA_DEBITO | 2% | 85% | $102.000 |
| TRANSFERENCIA | 1.5% | 75% | $101.500 |
| TARJETA_CREDITO | 5% | 90% | $105.000 |

> El procesamiento del pago es una simulación académica. No se consultan saldos reales ni se realizan transacciones financieras.

---

## 🏗️ Arquitectura

El proyecto sigue **Arquitectura Hexagonal (Ports & Adapters)** con separación estricta en tres capas: dominio, aplicación e infraestructura. El dominio no depende de ningún framework.

```
src/
├── main/java/co/uniquindio/eventoboletas/
│   │
│   ├── domain/                          ← Núcleo puro (sin Spring, sin JPA)
│   │   ├── entities/
│   │   │   ├── Cliente.java             ← RN-01, RN-06
│   │   │   ├── Evento.java              ← RN-02
│   │   │   ├── Zona.java                ← RN-03, RN-04 · calcularPrecioFinal(MetodoPago)
│   │   │   ├── Boleto.java              ← RN-05 · emitir() solo con pago APROBADO
│   │   │   └── Pago.java
│   │   ├── enums/
│   │   │   ├── EstadoCliente.java       ← ACTIVO · INACTIVO · BLOQUEADO
│   │   │   ├── EstadoEvento.java        ← ACTIVO · CANCELADO · AGOTADO
│   │   │   ├── EstadoBoleto.java        ← PAGADO · ANULADO
│   │   │   ├── EstadoPago.java          ← APROBADO · PENDIENTE · RECHAZADO
│   │   │   └── MetodoPago.java          ← lleva su propio recargoPorcentaje
│   │   ├── repositories/                ← Puertos de salida (interfaces — DIP)
│   │   │   ├── ClienteRepository.java
│   │   │   ├── EventoRepository.java
│   │   │   ├── ZonaRepository.java
│   │   │   └── BoletoRepository.java
│   │   └── exceptions/
│   │       ├── ReglaDeNegocioException.java       ← HTTP 422
│   │       └── EntidadNoEncontradaException.java  ← HTTP 404
│   │
│   ├── application/                     ← Casos de uso (orquestación)
│   │   ├── usecases/
│   │   │   ├── boleto/
│   │   │   │   ├── AbstractCompraUseCase.java      ← Template Method · algoritmo P1→P10 (final)
│   │   │   │   ├── ComprarBoletoUseCase.java       ← CU-01 · implementa los pasos abstractos
│   │   │   │   └── BuscarParaTransaccionUseCase.java
│   │   │   └── cliente/
│   │   │       └── GestionarClienteUseCase.java    ← CU-02 · CRUD completo
│   │   └── dtos/
│   │       ├── request/
│   │       │   ├── ComprarBoletoRequest.java       ← clienteId, eventoId, zonaId, metodoPago, cantidad
│   │       │   └── ClienteRequest.java
│   │       └── response/
│   │           ├── BoletoResponse.java
│   │           ├── ClienteResponse.java
│   │           ├── EventoResponse.java
│   │           ├── ZonaResponse.java
│   │           ├── PagoResponse.java
│   │           └── PagedResponse.java
│   │
│   └── infrastructure/                  ← Adaptadores e implementaciones
│       ├── adapters/                    ← Implementan los puertos de dominio (patrón Adapter)
│       │   ├── ClienteRepositoryAdapter.java
│       │   ├── EventoRepositoryAdapter.java
│       │   ├── ZonaRepositoryAdapter.java
│       │   └── BoletoRepositoryAdapter.java
│       ├── persistence/
│       │   ├── entities/                ← Entidades JPA (@Entity), separadas del dominio
│       │   │   ├── ClienteJpa.java
│       │   │   ├── EventoJpa.java
│       │   │   ├── ZonaJpa.java
│       │   │   ├── BoletoJpa.java
│       │   │   └── PagoJpa.java
│       │   └── repositories/            ← Spring Data JPA
│       │       ├── ClienteJpaRepository.java
│       │       ├── EventoJpaRepository.java
│       │       ├── ZonaJpaRepository.java
│       │       └── BoletoJpaRepository.java
│       ├── config/
│       │   ├── AplicacionIniciadaEvent.java  ← Evento de dominio (Observer · sujeto)
│       │   ├── DataSeeder.java               ← Publica AplicacionIniciadaEvent al arranque
│       │   ├── ClienteSeeder.java            ← Observador 1 · carga clientes
│       │   ├── EventoSeeder.java             ← Observador 2 · carga eventos y zonas
│       │   └── OpenApiConfig.java            ← Configuración Swagger
│       └── web/
│           ├── controllers/
│           │   ├── BoletoController.java     ← /api/transaccion/**
│           │   └── ClienteController.java    ← /api/clientes/**
│           └── handlers/
│               ├── GlobalExceptionHandler.java        ← Ensambla la cadena de handlers
│               └── chain/
│                   ├── ExceptionHandlerLink.java      ← Eslabón base abstracto (CoR)
│                   ├── ValidacionHandler.java         ← Eslabón 1 · 400 Bean Validation
│                   ├── ReglaDeNegocioHandler.java     ← Eslabón 2 · 422 reglas de negocio
│                   ├── EntidadNoEncontradaHandler.java← Eslabón 3 · 404 no encontrado
│                   └── FallbackHandler.java           ← Eslabón final · 500 error general
│
└── test/java/co/uniquindio/eventoboletas/
    │
    ├── domain/
    │   └── DomainRulesTest.java
    │       ← Pruebas puras de dominio: RN-01 a RN-06
    │         Sin Spring ni mocks — instancia entidades directamente
    │         Cubre los 3 estados de cliente, 3 estados de evento,
    │         cupo suficiente/insuficiente, cálculo de precio con
    │         cada método de pago, pago aprobado/rechazado/pendiente
    │
    ├── usecases/
    │   ├── boleto/
    │   │   └── ComprarBoletoUseCaseTest.java
    │   │       ← Mockito · RN-01 a RN-05
    │   │         Verifica abort de transacción en cada falla,
    │   │         precio unitario por método de pago, compra de N boletos
    │   └── cliente/
    │       └── GestionarClienteUseCaseTest.java
    │           ← Mockito · RN-06 y RN-07
    │             CRUD completo, email único en crear y editar,
    │             eliminación con y sin boletos pagados
    │
    └── integration/
        ├── ComprarBoletoIntegrationTest.java
        │   ← MockMvc + H2 real · RN-01 a RN-05
        │     HTTP end-to-end: verifica códigos de estado,
        │     precio final en body, mensaje de error en 422
        └── GestionarClienteIntegrationTest.java
            ← MockMvc + H2 real · RN-06 y RN-07
              Crea boleto real antes de intentar eliminar,
              prueba edición con email propio vs email ajeno
```

---

## 🎨 Patrones de diseño aplicados

| Patrón | Categoría | Dónde y cómo |
|--------|-----------|--------------|
| **Factory Method** | Creacional | `Cliente.crear()`, `Zona.crear()`, `Boleto.emitir()`, `Pago.crear()` — constructor privado + método estático que garantiza construcción válida y aplica invariantes (ej. `Boleto.emitir()` exige pago APROBADO). |
| **Template Method** | Comportamiento | `AbstractCompraUseCase` define el algoritmo P1→P10 como método `final`. `ComprarBoletoUseCase` extiende esa clase e implementa cada paso abstracto (`obtenerCliente`, `validarEvento`, `calcularPrecio`, etc.) sin alterar el flujo general. |
| **Chain of Responsibility** | Comportamiento | `ExceptionHandlerLink` es el eslabón base con `puedeAtender()` y `setSiguiente()`. La cadena es: `ValidacionHandler` → `ReglaDeNegocioHandler` → `EntidadNoEncontradaHandler` → `FallbackHandler`. `GlobalExceptionHandler` la ensambla con `@PostConstruct` y delega a ella con un único `@ExceptionHandler(Exception.class)`. |
| **Observer** | Comportamiento | `DataSeeder` (sujeto) publica `AplicacionIniciadaEvent` al arrancar. `ClienteSeeder` y `EventoSeeder` (observadores) escuchan ese evento con `@EventListener` y cargan sus datos de forma independiente y desacoplada. |
| **Adapter** | Estructural | `ClienteRepositoryAdapter`, `EventoRepositoryAdapter`, `ZonaRepositoryAdapter` y `BoletoRepositoryAdapter` implementan los puertos de dominio y traducen entre el modelo de dominio y las entidades JPA mediante mappers `toDomain()` / `toJpa()`. |
| **Facade** | Estructural | `ComprarBoletoUseCase.ejecutar()` es el único punto de entrada para el CU-01. Desde el controlador se hace una sola llamada; la orquestación de 4 repositorios y múltiples entidades queda completamente oculta. |
| **Repository** | Arquitectural | Las interfaces en `domain/repositories/` son los puertos de salida. Los adaptadores en `infrastructure/adapters/` son las implementaciones concretas. El dominio nunca importa JPA ni Spring. |

---

## 🧱 Principios SOLID

| Principio | Aplicación en el proyecto |
|-----------|--------------------------|
| **SRP** | Cada use case tiene una única responsabilidad. Las entidades de dominio protegen sus propias invariantes. `ClienteSeeder` y `EventoSeeder` tienen cada uno una única razón de cambio. |
| **OCP** | Para agregar un nuevo tipo de excepción basta con crear un nuevo `ExceptionHandlerLink` e insertarlo en la cadena, sin modificar los handlers existentes. Para agregar un observador de arranque basta con crear un nuevo `@EventListener`. |
| **LSP** | Cualquier implementación de `ClienteRepository` (JPA, en memoria, etc.) es intercambiable sin afectar los casos de uso. |
| **ISP** | Cada repositorio de dominio expone únicamente los métodos que su caso de uso necesita. `BoletoRepository` no expone operaciones de búsqueda que solo necesita `ClienteRepository`. |
| **DIP** | Domain y Application dependen de interfaces (puertos). Nunca importan clases de Spring ni de JPA. La inyección de dependencias la gestiona Spring en la capa de infraestructura. |

---

## 🔗 Endpoints REST

### CU-01 — Comprar Boleto

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/transaccion/clientes/buscar?q=ana` | Buscar cliente por nombre o documento |
| GET | `/api/transaccion/eventos/listar-activos` | Eventos activos con sus zonas |
| POST | `/api/transaccion/comprar-boleto` | Ejecutar compra (retorna lista de boletos) |

Body del POST:
```json
{
  "clienteId": 1,
  "eventoId": 1,
  "zonaId": 2,
  "metodoPago": "EFECTIVO",
  "cantidad": 2
}
```

Valores válidos para `metodoPago`: `EFECTIVO`, `TARJETA_DEBITO`, `TARJETA_CREDITO`, `PSE`, `TRANSFERENCIA`

La respuesta es un array con un objeto por cada boleto emitido:
```json
[
  {
    "id": 1,
    "codigoQR": "A3F9...",
    "precioFinal": 126000.00,
    "estadoBoleto": "PAGADO",
    "pago": { "estado": "APROBADO", "metodoPago": "EFECTIVO", ... }
  }
]
```

### CU-02 — Clientes

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/clientes/listar?pagina=0&tamano=10` | Listar paginado |
| POST | `/api/clientes/crear` | Crear cliente |
| PUT | `/api/clientes/{id}/editar` | Editar cliente |
| DELETE | `/api/clientes/{id}/eliminar` | Eliminar cliente |

### Códigos de respuesta

| Código | Cuándo ocurre |
|--------|---------------|
| 200 OK | Operación exitosa |
| 201 Created | Cliente creado |
| 400 Bad Request | Campos inválidos (Bean Validation) |
| 404 Not Found | Cliente, evento o zona inexistente |
| 422 Unprocessable Entity | Violación de regla de negocio (RN-01 a RN-07) |
| 500 Internal Server Error | Error inesperado del servidor |

---

## 🧪 Cobertura de tests por regla de negocio

| Regla | Descripción | Domain | UseCase | Integration |
|-------|-------------|--------|---------|-------------|
| **RN-01** | Cliente debe estar ACTIVO | 3 tests | 3 tests | 3 tests |
| **RN-02** | Evento debe estar ACTIVO | 3 tests | 2 tests | 1 test |
| **RN-03** | Cupo disponible >= cantidad | 5 tests | 2 tests | 1 test |
| **RN-04** | Precio calculado en servidor | 6 tests | 6 tests | 3 tests |
| **RN-05** | Boleto solo con pago APROBADO | 4 tests | 1 test | 1 test |
| **RN-06** | No eliminar cliente con boletos | 2 tests | 3 tests | 2 tests |
| **RN-07** | Email único en el sistema | — | 4 tests | 4 tests |

> RN-07 no tiene test de dominio puro porque la unicidad de email requiere consultar el repositorio; no es una invariante de la entidad sola.

Cada regla se prueba en tres capas independientes:
- **Domain** — sin Spring ni mocks, instancia entidades directamente.
- **UseCase** — con Mockito, aísla el caso de uso de la infraestructura.
- **Integration** — con MockMvc + H2 real, verifica el comportamiento HTTP de extremo a extremo.

---

## 🛠️ Stack tecnológico

| Componente | Tecnología |
|------------|-----------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.2 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | H2 en memoria |
| Documentación API | Springdoc OpenAPI 2 (Swagger UI) |
| Tests | JUnit 5 + Mockito + AssertJ + MockMvc |
| Build | Maven 3.8+ |