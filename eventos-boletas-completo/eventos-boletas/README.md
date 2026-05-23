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
INFO  DataSeeder      : Datos demo cargados correctamente
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

### Clientes — flujos de error (RN-01)

| ID | Nombre | Estado | Comportamiento esperado |
|----|--------|--------|------------------------|
| 9 | Sofía Vargas | INACTIVO | 422 — cliente no habilitado |
| 10 | Miguel Jiménez | BLOQUEADO | 422 — cliente no habilitado |

### Eventos y zonas

| ID | Nombre | Estado | Zonas |
|----|--------|--------|-------|
| 1 | Festival Latinoamericano de Música | ACTIVO | VIP · General · Palco |
| 2 | UniQuindío Tech Conference 2026 | ACTIVO | Premium · Libre |
| 3 | Hamlet — Compañía Nacional de Teatro | ACTIVO | Butaca · Galería (agotada, cupo=0) |
| 4 | Concierto Cancelado | CANCELADO | — sirve para probar RN-02 |

### Recargos por método de pago

| Método | Recargo | Ejemplo sobre $100.000 |
|--------|---------|------------------------|
| EFECTIVO | 0% | $100.000 |
| PSE | 1% | $101.000 |
| TARJETA_DEBITO | 2% | $102.000 |
| TRANSFERENCIA | 1.5% | $101.500 |
| TARJETA_CREDITO | 5% | $105.000 |

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
│   │   │   │   ├── ComprarBoletoUseCase.java       ← CU-01 · flujo P1→P10
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
│       ├── adapters/                    ← Implementan los puertos de dominio
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
│       │   ├── DataSeeder.java          ← Carga datos demo al arranque
│       │   └── OpenApiConfig.java       ← Configuración Swagger
│       └── web/
│           ├── controllers/
│           │   ├── BoletoController.java     ← /api/transaccion/**
│           │   └── ClienteController.java    ← /api/clientes/**
│           └── handlers/
│               └── GlobalExceptionHandler.java  ← Manejo centralizado de errores
│
└── test/java/co/uniquindio/eventoboletas/
    │
    ├── domain/
    │   └── DomainRulesTest.java
    │       ← Pruebas de reglas de negocio RN-01 a RN-06
    │         Sin Spring ni mocks — solo instancia entidades de dominio
    │
    ├── usecases/
    │   ├── boleto/
    │   │   └── ComprarBoletoUseCaseTest.java
    │   │       ← Flujo feliz + alternos bloqueantes (Mockito)
    │   │         Verifica: precio con recargo, múltiples boletos, RN-01..RN-05
    │   └── cliente/
    │       └── GestionarClienteUseCaseTest.java
    │           ← CRUD + RN-06 + RN-07 (Mockito)
    │
    └── integration/
        └── ComprarBoletoIntegrationTest.java
            ← HTTP real de extremo a extremo (MockMvc + H2)
              Levanta Spring Boot completo, verifica request → response → BD
```

### Patrones de diseño aplicados

| Patrón | Dónde |
|--------|-------|
| **Factory Method** | `Cliente.crear()`, `Zona.crear()`, `Boleto.emitir()`, `Pago.crear()` |
| **Adapter** | `ClienteRepositoryAdapter`, `EventoRepositoryAdapter`, `ZonaRepositoryAdapter`, `BoletoRepositoryAdapter` |
| **Facade** | `ComprarBoletoUseCase` — orquesta repos y entidades en una sola operación |
| **Repository** | Puertos de dominio (interfaces) + adaptadores JPA |
| **Observer** | `DataSeeder implements ApplicationRunner` |
| **Chain of Responsibility** | `GlobalExceptionHandler` (`@RestControllerAdvice`) |
| **Template Method** | Flujo P1→P10 del CU-01 en `ComprarBoletoUseCase` |

### Principios SOLID

| Principio | Aplicación |
|-----------|-----------|
| **SRP** | Un use case = una responsabilidad. Las entidades protegen sus propias invariantes. |
| **OCP** | Nuevas reglas se agregan sin modificar los casos de uso existentes. |
| **LSP** | Cualquier implementación de un repositorio es intercambiable. |
| **ISP** | Cada repositorio de dominio expone solo los métodos que su use case necesita. |
| **DIP** | Domain y Application dependen de interfaces; nunca de JPA ni de Spring. |

---

## 🔗 Endpoints REST

### CU-01 — Comprar Boleto

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/transaccion/clientes/buscar?q=ana` | Buscar cliente activo |
| GET | `/api/transaccion/eventos/listar-activos` | Eventos activos con zonas |
| POST | `/api/transaccion/comprar-boleto` | Ejecutar compra |

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

### CU-02 — Clientes

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/clientes/listar?pagina=0&tamano=10` | Listar paginado |
| POST | `/api/clientes/crear` | Crear cliente |
| PUT | `/api/clientes/{id}/editar` | Editar cliente |
| DELETE | `/api/clientes/{id}/eliminar` | Eliminar cliente |

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