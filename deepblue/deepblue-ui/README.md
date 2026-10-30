# DeepBlue Rescue — Frontend (React + TanStack Query)

## Requisitos
- Node 20+
- Backend Spring Boot corriendo en `http://localhost:8080` con datos (ver `../datos_prueba.sql`)

## Ejecutar
```bash
npm install
npm run dev
```
Abrir http://localhost:5173. El proxy de Vite reenvía `/api` hacia el backend.

## Estructura
- `src/api/` — llamadas HTTP (`request` lanza `ApiError`)
- `src/hooks/` — `useRescueCases`, `useAnimal`, `useRegisterTreatment`
- `src/components/` — `CaseList`, `CaseDetail`, `ChangeStatus`, `TreatmentList`, `TreatmentForm`, `ErrorMessage`
