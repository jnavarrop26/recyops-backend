# Esquema de datos en Supabase — una "base de datos" por empresa

RecyOps usa **un solo proyecto de Supabase** y le da a cada empresa su **propio
esquema de Postgres** (aislamiento por esquema, *no* multitenancy por filas):

```
Proyecto Supabase (Postgres)
├── public                      ← solo el directorio de empresas (registro_empresas.sql)
│   └── empresas                (nombre, nit, schema_name, activo)
├── empresa_ecoverde            ← esquema COMPLETO de la empresa EcoVerde
│   ├── roles, usuarios
│   ├── bodegas
│   ├── materiales, opciones_catalogo
│   ├── proveedores, convenios
│   ├── entregas, ingresos_material
│   └── lineas_inventario, movimientos_inventario
├── empresa_recimax             ← copia idéntica para otra empresa
│   └── ... (mismas tablas)
└── auth (Supabase Auth)        ← credenciales de TODOS los usuarios
```

Cada empresa ve únicamente sus tablas: no hay columnas `empresa_id` ni filas
compartidas. Es el mismo efecto que "una BD por empresa" pero con un solo
proyecto (un solo costo, un solo pool de conexiones).

## Cómo enruta el API al esquema correcto

1. El usuario hace login (`POST /api/auth/login`); el API delega en Supabase
   Auth y devuelve el `access_token`.
2. Ese JWT trae en `app_metadata` dos claims que el admin fija al crear el
   usuario: `rol` (`ADMIN`/`OPERARIO`) y `empresa_schema` (ej. `empresa_ecoverde`).
3. En cada petición, `FiltroEsquemaEmpresa` lee `app_metadata.empresa_schema`
   y lo guarda en `ContextoEmpresa` (ThreadLocal).
4. Hibernate, vía `ProveedorConexionesPorEsquema`, ejecuta
   `SET search_path TO <esquema>` sobre la conexión del pool antes de cada
   operación. Todas las consultas del request caen en el esquema de esa empresa.
5. Si el JWT no trae el claim (desarrollo), se usa `supabase.esquema-por-defecto`
   (`empresa_demo` por defecto).

El nombre del esquema se valida contra `^[a-z][a-z0-9_]{0,62}$` antes de usarse
en `search_path`, así que un claim manipulado no puede inyectar SQL.

## Alta de una empresa nueva (paso a paso)

1. **Crear el esquema con sus tablas**: abre
   [esquema_empresa.sql](../src/main/resources/db/esquema_empresa.sql),
   reemplaza `{{esquema}}` por el nombre (ej. `empresa_ecoverde`) y ejecútalo
   en el SQL Editor de Supabase. El script crea las tablas, secuencias,
   los roles `ADMIN`/`OPERARIO` y las semillas del catálogo de materiales.
2. **Registrarla en el directorio**:
   ```sql
   insert into public.empresas (nombre, nit, schema_name)
   values ('EcoVerde SAS', '900123456-7', 'empresa_ecoverde');
   ```
   (La tabla se crea una única vez con
   [registro_empresas.sql](../src/main/resources/db/registro_empresas.sql).)
3. **Crear el usuario administrador en Supabase Auth**
   (Dashboard > Authentication > Users > *Add user*, o con la Admin API) y
   asignarle los claims. Con la Admin API:
   ```
   POST {SUPABASE_URL}/auth/v1/admin/users
   apikey / Authorization: <service_role_key>
   {
     "email": "admin@ecoverde.com",
     "password": "********",
     "email_confirm": true,
     "user_metadata": { "nombre_completo": "Ana Admin", "username": "ana" },
     "app_metadata":  { "rol": "ADMIN", "empresa_schema": "empresa_ecoverde" }
   }
   ```
4. Listo: cuando ese usuario inicie sesión, todas sus peticiones leen y
   escriben en `empresa_ecoverde`.

## Configuración del API (variables de entorno)

| Variable | Qué es |
|---|---|
| `SUPABASE_DB_URL` | JDBC del pooler (Project Settings > Database), modo *Session*, puerto 5432 |
| `SUPABASE_DB_USER` / `SUPABASE_DB_PASSWORD` | Credenciales de la base |
| `SUPABASE_URL` | `https://<ref>.supabase.co` |
| `SUPABASE_ANON_KEY` | Llave pública; la usa el login |
| `SUPABASE_SERVICE_ROLE_KEY` | Llave admin; solo para crear usuarios (nunca al cliente) |
| `RECYOPS_ESQUEMA_DEFECTO` | Esquema usado sin claim (desarrollo) |

**JWT**: el API valida los tokens contra
`{SUPABASE_URL}/auth/v1/.well-known/jwks.json` (claves asimétricas, opción
actual de Supabase). Si tu proyecto todavía usa el *JWT secret* legado (HS256),
reemplaza `jwk-set-uri` por un bean `JwtDecoder` con
`NimbusJwtDecoder.withSecretKey(...)` usando ese secret.

## Por qué esquema-por-empresa y no las alternativas

- **RLS con `empresa_id` (multitenancy por filas)**: descartado por requisito;
  todas las empresas compartirían tablas.
- **Un proyecto Supabase por empresa**: aislamiento máximo (incluso de Auth y
  Storage), pero cada proyecto se paga y administra por separado y el API
  tendría que manejar N pools de conexiones. Vale la pena solo si un cliente
  exige aislamiento físico contractual.
- **Esquema por empresa (elegido)**: separación real de datos, un solo
  proyecto, un solo pool, y agregar una empresa es ejecutar un script.
