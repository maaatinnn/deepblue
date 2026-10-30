Análisis — Capa de controlador 

Respuestas al checkpoint de la primera mitad del laboratorio (Partes I a XIV).

21. Checkpoint — Validación de entrada vs regla de negocio

| Regla | DTO/Controller o Service |
|---|---|
| `animalCode` vacío | **DTO/Controller** (`@NotBlank`) |
| `status == null` | **DTO/Controller** (`@NotNull`) |
| animal inexistente | **Service** (`ResourceNotFoundException`) |
| especialista inactivo | **Service** (`BusinessRuleException`) |
| `description` > 500 | **DTO/Controller** (`@Size(max = 500)`) |
| caso `RELEASED` | **Service** (`BusinessRuleException`) |
| transición `ADMITTED → RELEASED` | **Service** (`BusinessRuleException`) |

Criterio: si la regla se puede decidir mirando **solo el request** (formato, obligatoriedad, longitud, fecha futura), es validación de entrada y se resuelve en el DTO con Bean Validation antes de llegar al Service. Si hay que **consultar datos o estado del sistema**, es una regla de negocio y vive en el Service.

Mapa de excepciones a HTTP

| Situación | HTTP | Handler en `GlobalExceptionHandler` |
|---|---|---|
| DTO inválido | 400 | `MethodArgumentNotValidException` |
| JSON inválido / enum desconocido en el body | 400 | `HttpMessageNotReadableException` |
| Query parameter inválido | 400 | `MethodArgumentTypeMismatchException` |
| Recurso inexistente | 404 | `ResourceNotFoundException` |
| Regla de negocio violada | 409 | `BusinessRuleException` |
| Error inesperado | 500 | `Exception` |

Todos devuelven `ResponseEntity<ErrorResponse>` con la misma estructura (`timestamp`, `status`, `error`, `message`, `details`).

Cobertura Service → endpoint

| Método Service | Endpoint | Controller |
|---|---|---|
| `RescueCaseService.findByCode()` | `GET /api/rescue-cases/{caseCode}` | `RescueCaseController` |
| `RescueCaseService.findByStatus()` | `GET /api/rescue-cases?status=...` | `RescueCaseController` |
| `RescueCaseService.changeStatus()` | `PATCH /api/rescue-cases/{caseCode}/status` | `RescueCaseController` |
| `TreatmentService.register()` | `POST /api/treatments` (201) | `TreatmentController` |
| `TreatmentService.findByAnimalCode()` | `GET /api/animals/{animalCode}/treatments` | `AnimalController` |
| `AnimalService.findByCode()` | `GET /api/animals/{animalCode}` | `AnimalController` |
| `AnimalService.findAnimalsInRehabilitation()` | `GET /api/animals/in-rehabilitation` | `AnimalController` |
| `AnimalService.canReceiveTreatment()` | `GET /api/animals/{animalCode}/treatment-eligibility` | `AnimalController` |

8 métodos de Service → 8 operaciones HTTP, ninguno sin exponer.

---

Segunda mitad del laboratorio (Partes XV a XXXIV)

Lo que se agregó en esta entrega

| Elemento | Detalle |
|---|---|
| `RescueCaseControllerTest` | 15 tests (`@WebMvcTest` + `@MockitoBean RescueCaseService`) |
| `TreatmentControllerTest` | 9 tests (`@MockitoBean TreatmentService`) |
| `AnimalControllerTest` | 9 tests (`@MockitoBean AnimalService` y `TreatmentService`) |
| `pom.xml` | Se agregó `spring-boot-starter-webmvc-test` (ver nota 1) |
| `GlobalExceptionHandler` | 3 handlers extra (ver nota 2) |

Nota 1 — Spring Boot 4 y `@WebMvcTest`

En Spring Boot 4 los slices de test se modularizaron. `@WebMvcTest` vive ahora en
`org.springframework.boot.webmvc.test.autoconfigure` y requiere el starter
`spring-boot-starter-webmvc-test`; con solo `spring-boot-starter-test` no compila.
`@MockitoBean` viene de Spring Framework 7 (`org.springframework.test.context.bean.override.mockito`);
`@MockBean` ya no existe.

Nota 2 — Handlers que el laboratorio no pedía

Con el handler `Exception.class` como "red de seguridad", cualquier excepción estándar de Spring MVC que no
tuviera handler propio terminaba en **500**, que es incorrecto:

| Caso | Sin el handler extra | Ahora |
|---|---|---|
| `GET /api/rescue-cases` (falta `?status=`) | 500 | **400** (`MissingServletRequestParameterException`) |
| `PATCH /api/rescue-cases/RES-1` (método no soportado) | 500 | **405** (`HttpRequestMethodNotSupportedException`) |
| `GET /api/ruta-inexistente` | 500 | **404** (`NoResourceFoundException`) |

Todos siguen devolviendo `ResponseEntity<ErrorResponse>`. Quedan cubiertos por tests.

---

Parte XXII — Reto integrador (flujo de 7 operaciones)

| # | Operación | Resultado | Quién lo decide |
|---|---|---|---|
| 1 | `GET /api/rescue-cases/RES-2026-100` | 200 | Service encuentra el caso |
| 2 | `GET /api/animals/AN-2026-100` | 200 | Service encuentra el animal |
| 3 | `POST /api/treatments` (SPEC-001 activo, caso `IN_REHABILITATION`) | 201 | Controller fija el 201; Service valida reglas |
| 4 | `PATCH .../RES-2026-100/status` → `READY_FOR_RELEASE` | 200 | Service: transición válida |
| 5 | `PATCH` con `{"status": null}` | 400 | Bean Validation (`@NotNull`), el Service **ni se invoca** |
| 6 | `PATCH` con transición inválida (p. ej. `READY_FOR_RELEASE → ADMITTED`) | 409 | Service lanza `BusinessRuleException` |
| 7 | `GET /api/animals/AN-999` | 404 | Service lanza `ResourceNotFoundException` |

Observa que 5 y 6 son *ambos* "el PATCH está mal", pero fallan en capas distintas: el 5 se decide mirando solo el
body (DTO); el 6 exige conocer el estado actual del caso (Service).

> Con un caso nuevo, el paso 3 deja el animal en `IN_REHABILITATION`, y tras el paso 4 el caso queda en
> `READY_FOR_RELEASE`; un nuevo `POST /api/treatments` seguiría siendo válido (solo `RELEASED`/`CLOSED` bloquean).

Parte XXIII — Reto del estudiante: elegibilidad

| Requisito | Dónde está |
|---|---|
| 1. DTO | `TreatmentEligibilityResponse(String animalCode, boolean eligible)` |
| 2. Endpoint | `AnimalController.canReceiveTreatment()` → `GET /api/animals/{animalCode}/treatment-eligibility` |
| 3. Llamada a `AnimalService` | `animalService.canReceiveTreatment(animalCode)` |
| 4. Test 200 | `shouldReturnTreatmentEligibility` (y `shouldReturnNotEligibleWhenAnimalCannotReceiveTreatment` para `false`) |
| 5. Test 404 | `shouldReturn404WhenCheckingEligibilityOfUnknownAnimal` |
| 6. `verify(...)` | `verify(animalService).canReceiveTreatment("AN-001")` |

Detalle de diseño: el Controller **no calcula** la elegibilidad; solo arma el DTO con lo que responde el Service.
Que "elegible" signifique `UNDER_EVALUATION` o `IN_REHABILITATION` es regla de negocio y vive en `AnimalServiceImpl`.

Parte XXIV — Diseño REST

**92. `GET /api/getAnimal?id=1` vs `GET /api/animals/AN-001`** → la segunda.
El recurso es un sustantivo (`animals`) y se identifica en el path; el verbo ya lo da el método HTTP, así que
`getAnimal` es redundante (RPC disfrazado de REST). Además `AN-001` es el identificador de negocio y la URL
es estable, legible y cacheable. El `id` como query parameter sugiere un filtro, no la identidad del recurso.

**93. `POST /changeStatus` vs `PATCH /api/rescue-cases/RES-001/status`** → el PATCH.
`PATCH` declara semántica de modificación **parcial** (solo cambia `status`, el resto del caso queda intacto);
la URL dice *qué recurso* y *qué parte*. `POST /changeStatus` no dice sobre qué caso opera (iría en el body),
mete un verbo en la URL y `POST` implica "crear". Nota: `PATCH` no es idempotente por definición, pero aquí
repetirlo da 409 (la transición ya no es válida), lo cual es coherente con la regla de negocio.

**94. `GET /api/animals/AN-001/treatments` vs `GET /api/treatments?animal=AN-001`**

| | Anidado | Filtro |
|---|---|---|
| Relación que comunica | **Pertenencia**: los tratamientos *de* este animal; el tratamiento no se concibe sin su animal | Los tratamientos son un recurso **independiente** al que se le aplica un filtro |
| Cuándo conviene | Navegación jerárquica clara | Consultas transversales (por especialista, fecha, tipo, combinadas) |
| Costo | Se acopla a la jerarquía; con más niveles se vuelve profundo | Menos expresivo sobre la relación |

En este proyecto el tratamiento **se crea** en `POST /api/treatments` (necesita `animalCode` *y* `specialistCode`,
así que no pertenece solo a un animal), y la **lectura por animal** es anidada. Ambos coexisten sin conflicto.

## Parte XXV — Anti-patrones (cómo se evitaron en el proyecto)

| Anti-patrón | Estado en el código |
|---|---|
| Controller → Repository | Los 3 controllers solo inyectan `*Service` |
| Reglas de negocio en Controller | Ningún `if` de dominio en controllers; `canReceiveTreatment` solo envuelve el `boolean` |
| Retornar Entity | Todos los endpoints retornan DTOs `record` |
| `try/catch` en cada endpoint | Ninguno; todo en `GlobalExceptionHandler` |
| 200 para todo | 200 / 201 / 400 / 404 / 409 / 500 (+405 y 400 por parámetro faltante) |

## Parte XXVI — Clasificar responsabilidades (100)

| Necesidad | Capa |
|---|---|
| Recibir JSON | **Controller** (`@RequestBody`; la deserialización la hace Spring MVC) |
| Verificar `@NotBlank` | **DTO Validation** (disparado por `@Valid` en el Controller) |
| Buscar Animal | **Service** (orquesta) → Repository (ejecuta el acceso) |
| Verificar especialista activo | **Service** |
| Ejecutar query | **Repository** |
| Convertir excepción en 409 | **ControllerAdvice** |
| Abrir transacción | **Service** (`@Transactional`) |
| Retornar 201 | **Controller** |

## Parte XXVII — Testing por capa

| Test | Qué se prueba | Qué se reemplaza | Herramienta |
|---|---|---|---|
| Repository | Persistencia, queries, constraints | Nada (PostgreSQL real) | Testcontainers |
| Service | Reglas de negocio | Repository y Mapper (mocks) | JUnit 5 + Mockito |
| Controller | Contrato HTTP | Service (mock) | `@WebMvcTest` + `@MockitoBean` + MockMvc |

El test de Controller no necesita PostgreSQL ni Docker porque `@WebMvcTest` solo levanta la capa web
(controllers, `@RestControllerAdvice`, conversores JSON, validación) y el Service es un mock.

## Parte XXVIII — Matriz de trazabilidad Service → Controller → Test

| Método Service | Controller | Test principal | Otros tests |
|---|---|---|---|
| `RescueCaseService.findByCode()` | `RescueCaseController` | `shouldReturnRescueCaseByCode` | `shouldReturn404WhenCaseDoesNotExist` |
| `RescueCaseService.findByStatus()` | `RescueCaseController` | `shouldReturnCasesByStatus` | `shouldReturnEmptyListWhenNoCaseMatchesStatus`, `shouldReturn400WhenStatusQueryParamIsInvalid`, `shouldReturn400WhenStatusQueryParamIsMissing` |
| `RescueCaseService.changeStatus()` | `RescueCaseController` | `shouldChangeStatus` | `shouldReturn400WhenPatch*` (3), `shouldReturn404WhenChangingStatusOfUnknownCase`, `shouldReturn409WhenStatusTransitionIsInvalid` |
| `TreatmentService.register()` | `TreatmentController` | `shouldCreateTreatment` | `shouldReturn400*` (5), `shouldReturn404WhenAnimalDoesNotExist`, `shouldReturn409WhenBusinessRuleIsViolated`, `shouldReturn500OnUnexpectedError` |
| `TreatmentService.findByAnimalCode()` | `AnimalController` | `shouldReturnAnimalTreatments` | `shouldReturnEmptyListWhenAnimalHasNoTreatments` |
| `AnimalService.findByCode()` | `AnimalController` | `shouldReturnAnimalByCode` | `shouldReturn404WhenAnimalDoesNotExist` |
| `AnimalService.findAnimalsInRehabilitation()` | `AnimalController` | `shouldReturnAnimalsInRehabilitation` | `shouldNotTreatInRehabilitationAsAnAnimalCode` |
| `AnimalService.canReceiveTreatment()` | `AnimalController` | `shouldReturnTreatmentEligibility` | `shouldReturnNotEligible...`, `shouldReturn404WhenCheckingEligibilityOfUnknownAnimal` |

## Parte XXIX — Casos mínimos de error

| Situación | HTTP | Handler | Cubierto por |
|---|---|---|---|
| DTO inválido | 400 | `MethodArgumentNotValidException` | `shouldReturn400WhenPatchBodyIsEmptyObject`, `shouldReturn400WhenRequiredFieldsAreInvalid` |
| JSON inválido / enum en body | 400 | `HttpMessageNotReadableException` | `shouldReturn400WhenPatchStatusIsNotAnEnumValue`, `...BodyIsMalformedJson`, `shouldReturn400WhenTreatmentTypeIsNotAnEnumValue` |
| Query parameter inválido | 400 | `MethodArgumentTypeMismatchException` | `shouldReturn400WhenStatusQueryParamIsInvalid` |
| Query parameter faltante *(extra)* | 400 | `MissingServletRequestParameterException` | `shouldReturn400WhenStatusQueryParamIsMissing` |
| Recurso inexistente | 404 | `ResourceNotFoundException` | varios `shouldReturn404...` |
| Regla de negocio | 409 | `BusinessRuleException` | `shouldReturn409...` (2) |
| Error inesperado | 500 | `Exception` | `shouldReturn500...` (2) |
| Método no soportado *(extra)* | 405 | `HttpRequestMethodNotSupportedException` | `shouldReturn405WhenHttpMethodIsNotSupported` |

## Parte XXX — Los 18 tests obligatorios

| # | Caso | Test |
|---|---|---|
| 1 | GET RescueCase existente → 200 | `RescueCaseControllerTest.shouldReturnRescueCaseByCode` |
| 2 | GET RescueCase inexistente → 404 + ErrorResponse | `RescueCaseControllerTest.shouldReturn404WhenCaseDoesNotExist` |
| 3 | GET por status → 200 | `RescueCaseControllerTest.shouldReturnCasesByStatus` |
| 4 | GET status inválido → 400 | `RescueCaseControllerTest.shouldReturn400WhenStatusQueryParamIsInvalid` |
| 5 | PATCH válido → 200 | `RescueCaseControllerTest.shouldChangeStatus` |
| 6 | PATCH inválido → 400 + details | `RescueCaseControllerTest.shouldReturn400WhenPatchBodyIsEmptyObject` |
| 7 | PATCH transición inválida → 409 | `RescueCaseControllerTest.shouldReturn409WhenStatusTransitionIsInvalid` |
| 8 | POST Treatment válido → 201 | `TreatmentControllerTest.shouldCreateTreatment` |
| 9 | POST inválido → 400 + details | `TreatmentControllerTest.shouldReturn400WhenRequiredFieldsAreInvalid` |
| 10 | POST animal inexistente → 404 | `TreatmentControllerTest.shouldReturn404WhenAnimalDoesNotExist` |
| 11 | POST regla de negocio → 409 | `TreatmentControllerTest.shouldReturn409WhenBusinessRuleIsViolated` |
| 12 | GET Animal → 200 | `AnimalControllerTest.shouldReturnAnimalByCode` |
| 13 | GET en rehabilitación → 200 | `AnimalControllerTest.shouldReturnAnimalsInRehabilitation` |
| 14 | GET tratamientos del animal → 200 | `AnimalControllerTest.shouldReturnAnimalTreatments` |
| 15 | GET eligibility → 200 | `AnimalControllerTest.shouldReturnTreatmentEligibility` |
| 16 | GET Animal inexistente → 404 + ErrorResponse | `AnimalControllerTest.shouldReturn404WhenAnimalDoesNotExist` |
| 17 | JSON enum inválido → 400 | `RescueCaseControllerTest.shouldReturn400WhenPatchStatusIsNotAnEnumValue`, `TreatmentControllerTest.shouldReturn400WhenTreatmentTypeIsNotAnEnumValue` |
| 18 | Error inesperado → 500 | `RescueCaseControllerTest.shouldReturn500AndHideInternalDetailsOnUnexpectedError`, `TreatmentControllerTest.shouldReturn500OnUnexpectedError` |

Total: 33 tests de controller (15 + 9 + 9).

## Parte XXXII — Checklist

Todo marcado salvo la ejecución de `mvn clean test`, que debe correrse en el entorno del curso (necesita
descargar dependencias de Maven Central):

- [x] Dependencias (`webmvc`, `validation`, y `webmvc-test` para tests)
- [x] 3 Controllers con `@RestController`, `@RequestMapping`, `@Get/Post/PatchMapping`, `@PathVariable`, `@RequestParam`, `@RequestBody`, `@Valid`, `ResponseEntity`
- [x] `ErrorResponse` con `timestamp`, `status`, `error`, `message`, `details`
- [x] `@RestControllerAdvice` con 400 / 404 / 409 / 500 (+405, +400 parámetro faltante)
- [x] 8/8 métodos de Service expuestos y cubiertos
- [x] `@WebMvcTest`, `@MockitoBean`, `MockMvc`, `jsonPath`, `verify`, `verify(..., never())`
- [x] `ErrorResponse` y `details` verificados en los tests de error
- [ ] `mvn clean test` ejecutado con `BUILD SUCCESS` (pendiente de correr)

## Parte XXXIII — Preguntas de sustentación

1. **Responsabilidad del Controller:** traducir HTTP hacia la aplicación y de vuelta: URL, método, path/query/body, validación de formato, delegar al Service y elegir el status code.
2. **Controller vs Service:** el Controller responde "¿cómo llega la petición?"; el Service, "¿está permitida la operación?". El Service no conoce HTTP; el Controller no conoce reglas de negocio.
3. **Por qué no usar Repository directo:** se saltaría las reglas de negocio y la transacción, acoplaría la capa web a la persistencia, duplicaría lógica entre endpoints y volvería difícil probar el Controller.
4. **`@RestController`:** `@Controller` + `@ResponseBody`; el valor de retorno se serializa (JSON) en el body en lugar de resolver una vista.
5. **`@RequestMapping`:** asocia una URL base (y opcionalmente método HTTP) a un controller o handler. En clase define el prefijo, p. ej. `/api/animals`.
6. **`@PathVariable` vs `@RequestParam`:** `@PathVariable` toma un segmento del path e **identifica** el recurso (`/animals/AN-001`); `@RequestParam` toma un parámetro de query y **filtra o modifica** la consulta (`?status=...`).
7. **`@RequestBody`:** deserializa el body JSON al objeto Java (DTO).
8. **`@Valid`:** ordena ejecutar Bean Validation sobre el objeto; si falla, lanza `MethodArgumentNotValidException` antes de entrar al método.
9. **Validación de entrada vs regla de negocio:** la primera se decide mirando solo el request (formato, obligatoriedad, longitud); la segunda requiere consultar estado o datos del sistema.
10. **GET:** consultar recursos sin modificar estado.
11. **POST:** crear un recurso nuevo (o ejecutar una acción que no encaja en los otros verbos).
12. **PATCH:** modificación parcial de un recurso existente.
13. **200:** la operación (consulta o actualización) fue exitosa.
14. **201:** se creó un recurso nuevo.
15. **400:** el request es inválido (validación, JSON mal formado, parámetro inválido).
16. **404:** el recurso solicitado no existe.
17. **409:** el request es válido pero entra en conflicto con el estado actual o una regla de negocio.
18. **500:** error inesperado del servidor; no se exponen detalles internos.
19. **`ResponseEntity`:** controla status, headers y body de la respuesta HTTP de forma explícita.
20. **`@RestControllerAdvice`:** centraliza el manejo de excepciones para todos los controllers, evitando `try/catch` repetido.
21. **`ErrorResponse` común:** el cliente procesa todos los errores con un solo formato, y el contrato queda documentado y estable.
22. **`details`:** información adicional estructurada (p. ej. qué campo falló y por qué) sin cambiar el esquema.
23. **`MethodArgumentNotValidException` vs `BusinessRuleException`:** la primera la lanza Spring antes del Service por datos mal formados (400); la segunda la lanza el Service por violar una regla con datos bien formados (409).
24. **`@WebMvcTest`:** levanta solo la capa web (controllers, advice, conversores, validación), no el contexto completo.
25. **Service mock:** aísla al Controller; así el test verifica el contrato HTTP y no la lógica del Service ni la BD.
26. **MockMvc:** simula peticiones HTTP contra el DispatcherServlet sin levantar un servidor real y permite afirmar status, headers y body.
27. **Sin PostgreSQL:** porque el Service es un mock y `@WebMvcTest` no carga JPA ni DataSource.
28. **`verify(..., never())`:** demuestra que, ante un request inválido, la validación cortó el flujo y el Service no fue invocado.
29. **Transacciones:** el Service.
30. **Transición de estado:** el Service.
