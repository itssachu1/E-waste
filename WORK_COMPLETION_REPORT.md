# JanVoice AI — Work Completion Report

## Overview
JanVoice AI was upgraded into a more demo-ready civic intelligence platform with improved authentication fallback, a stronger frontend experience, and verified local build/test status.

## Completed Work

### 1. Demo Authentication Fix
- Fixed the frontend login experience so the app no longer gets stuck on “Authentication failed” during hackathon/demo usage.
- Preserved the existing authentication flow while adding a reliable demo fallback.
- Added two demo login buttons:
  - Continue as Citizen
  - Continue as MP/Admin
- Added a visible “Demo Mode” label.
- Demo login stores the user in localStorage using the existing key: `janvoice_user`.
- Citizen demo users are routed to the citizen portal.
- MP/Admin demo users are routed to the dashboard.

### 2. Frontend API and Routing Reliability
- Updated the frontend API base to use a safer `/api` path.
- Configured Vite dev proxying so the frontend can reach the Spring Boot backend during local development.
- Kept the project name as JanVoice AI.

### 3. Verification
- Verified the frontend production build successfully with:
  - `npm run build`
- Verified the backend test suite successfully with:
  - `mvn -q test`

## Files Updated
- frontend/src/views/LoginPortal.jsx
- frontend/src/services/api.js
- frontend/vite.config.js

## Bug Fix Update: Unstyled Deployment + Recycler 403s

### Root causes
1. **Unstyled deployed site** — `src/index.css`, the stylesheet imported by
   `main.jsx`, was empty and no other stylesheet contained
   `@import "tailwindcss"`. Tailwind utilities used by the login screen were
   therefore never emitted (the built CSS contained zero of `.flex`,
   `.min-h-screen`, `.text-emerald-700`, ...), so the entry screen rendered as
   raw HTML. The build itself still succeeded, and a service worker precaching
   the previous build kept the broken CSS around.
2. **403s for real Recycler logins** — two independent defects:
   - CORS: global `CorsConfig` registered `allowedOrigins(<origin>)` together
     with `allowCredentials(true)`. Spring merges that with the per-controller
     `@CrossOrigin(origins = "*")` into `allowedOrigins=["*"] +
     allowCredentials=true`, and `CorsConfiguration.checkOrigin` then threw
     `IllegalArgumentException` on **every** preflight — and every API call
     carries a Bearer token, so every call preflights. With CORS unset the API
     was permissive and worked, which is why the failure only appeared in the
     deployed (configured) environment.
   - Authorisation: `TransactionServiceImpl.earnings()` threw
     `403 "Collector role required"` for the `RECYCLER` role that
     `AuthController` issues.
   Demo profiles do not call the API, so neither defect was visible in demo
   mode.

### Fixes
- Restored the Tailwind v4 entry point in `src/index.css`
  (`@import "tailwindcss"`, explicit `@source` globs, `@theme` brand palette)
  and import the base stylesheet before `App.jsx`.
- Reworked `CorsConfig` to use origin **patterns** only (never
  `allowedOrigins("*")` with credentials), `allowCredentials=false` (Bearer
  token API), reads `CORS_ALLOWED_ORIGIN(S)` / `ALLOWED_ORIGIN(S)`, keeps
  localhost + PaaS preview hosts, and adds opt-in strict mode
  (`CORS_STRICT=true`). Removed the nine conflicting `@CrossOrigin`
  annotations so one global configuration is authoritative.
- `earnings()` now returns role-scoped figures (collector / recycler / admin)
  instead of a 403; recycler totals are scoped to that recycler's own lots.
- Dashboard, ledger and earnings panels now use `Promise.allSettled`, so a
  single failing endpoint can no longer blank out an unrelated view.
- PWA/deploy hygiene: `cleanupOutdatedCaches`, `skipWaiting`, `clientsClaim`,
  development service worker disabled, and the committed
  `frontend/dev-dist/` plus stale `frontend/index.html.save*` files removed
  (now git-ignored).

### Tests added
- `CorsConfigTest`, `CorsConfigConfiguredOriginTest` (reproduces the deployed
  configuration that used to fail), `CorsConfigStrictTest`,
  `CorsConfigParsingTest`
- `RecyclerRoleAccessTest` — a real `RECYCLER` login across dashboard, lots,
  transactions and earnings, including scoping/no-leak checks

### Verification
- `./mvnw -o clean test` → `Tests run: 63, Failures: 0, Errors: 0`
  (BUILD SUCCESS)
- `npm run build` → build OK; the emitted stylesheet now contains the Tailwind
  utilities (`.flex`, `.min-h-screen`, `.text-emerald-700`, ...) alongside the
  existing `App.css` rules

### Deployment note
- Rebuild/redeploy the frontend so the new CSS asset is published, set
  `CORS_ALLOWED_ORIGIN` on the backend and `VITE_API_BASE_URL` for the
  frontend build, and clear the Vercel build cache once (a previously
  installed service worker can keep serving the old HTML/CSS until the new
  worker activates).

## Commit
- Local commit created:
  - `65c03e5 Fix demo authentication flow`

## Status
The application is now usable for hackathon/demo purposes even if backend authentication is unavailable.
