# OceanBase Support Plan

This document tracks production-grade support for OceanBase as both the Crest metadata database and a business datasource.

## Scope

- MySQL remains the default metadata database and must keep its current behavior.
- `ob-mysql` uses MySQL-compatible runtime behavior with a dedicated initialization path.
- `ob-oracle` is the primary metadata database adaptation target and uses a separate initialization path.
- OceanBase metadata databases are treated as enterprise external databases in production: initialize them with SQL first, then start Crest with Flyway disabled.
- Business datasources must expose explicit `obMysql` and `obOracle` types instead of hiding OceanBase behind `mysql`.
- OceanBase datasource connections must support both direct observer access and OBProxy access.

## Local OceanBase Environment

Local environment documentation lives in `/Users/carson/Downloads/code/oceanbase/OB_LOCAL_USAGE.md`.

| Item | Value |
| --- | --- |
| Container | `ob-deploy` |
| OceanBase version | `5.7.25-OceanBase-v4.2.5.7` |
| Cluster | `obcluster` |
| Observer SQL | `127.0.0.1:2881` |
| OBProxy SQL | `127.0.0.1:2883` |
| MySQL tenant | `ob_mysql` |
| Oracle tenant | `ob_oracle` |
| Business user | `ob_user` |
| Business password | `ob_password` |

## Connection Checks

OBProxy is the preferred path for routine validation:

```bash
obclient -h127.0.0.1 -P2883 -uob_user@ob_mysql#obcluster -pob_password
```

```bash
obclient -h127.0.0.1 -P2883 -uob_user@ob_oracle#obcluster -pob_password
```

Observer direct connections must also be covered:

```bash
obclient -h127.0.0.1 -P2881 -uob_user@ob_mysql -pob_password
```

```bash
obclient -h127.0.0.1 -P2881 -uob_user@ob_oracle -pob_password
```

For JDBC datasource checks, use `jdbc:oceanbase://127.0.0.1:2883` through OBProxy and `jdbc:oceanbase://127.0.0.1:2881` for observer direct connections. OBProxy usernames include the cluster suffix, for example `ob_user@ob_oracle#obcluster`; observer direct usernames use the tenant suffix without the cluster, for example `ob_user@ob_oracle`.

## Metadata Database Configuration

Common variables:

| Variable | Description | Default |
| --- | --- | --- |
| `CREST_DB_TYPE` | Metadata database type: `mysql`, `ob-mysql`, or `ob-oracle` | `mysql` |
| `CREST_DB_DRIVER_CLASS_NAME` | Optional JDBC driver override | Type-specific |
| `CREST_DB_URL` | Optional full JDBC URL. If set, it has priority. Docker Compose deployments should set it explicitly for OceanBase. | Empty |
| `CREST_DB_HOST` | Database host. Use an OBProxy/ODP host or an observer host for OceanBase. | `127.0.0.1` |
| `CREST_DB_PORT` | Database port. OBProxy/ODP usually uses `2883`; observer direct connections usually use `2881`. | MySQL `3306`, OceanBase `2883` |
| `CREST_DB_NAME` | Metadata database name for MySQL-compatible metadata databases | `crest` |
| `CREST_FLYWAY_ENABLED` | Whether Crest runs Flyway on startup. Leave unset for installer auto mode: MySQL `true`, OceanBase `false`. | Type-specific |
| `CREST_FLYWAY_LOCATIONS` | Optional Flyway location override | Type-specific |

Example OB MySQL metadata database:

```bash
export CREST_DB_TYPE=ob-mysql
export CREST_DB_HOST=127.0.0.1
export CREST_DB_PORT=2883
export CREST_DB_NAME=crest
export CREST_DB_USERNAME='ob_user@ob_mysql#obcluster'
export CREST_DB_PASSWORD='ob_password'
```

Example OB Oracle metadata database:

```bash
export CREST_DB_TYPE=ob-oracle
export CREST_DB_HOST=127.0.0.1
export CREST_DB_PORT=2883
export CREST_DB_USERNAME='ob_user@ob_oracle#obcluster'
export CREST_DB_PASSWORD='ob_password'
```

When running with Docker Compose files, provide an explicit OceanBase URL so the MySQL default URL is not used:

```bash
export CREST_DB_URL='jdbc:oceanbase://127.0.0.1:2883/crest'   # ob-mysql
export CREST_DB_URL='jdbc:oceanbase://127.0.0.1:2883'         # ob-oracle
```

The single-node installer generates `CREST_DB_URL`, driver, and Flyway locations from `CREST_DB_TYPE`.
For OceanBase metadata databases, set `CREST_DB_TYPE`, `CREST_DB_HOST`, `CREST_DB_PORT`, `CREST_DB_USERNAME`, and `CREST_DB_PASSWORD` in `installer/install.conf`; leave `CREST_DB_URL` empty unless a custom JDBC URL is required. OceanBase metadata databases automatically use external database mode and do not start the bundled MySQL container.

For production OceanBase deployments, have the DBA create the tenant, database or schema, user, and permissions first. Then execute the SQL files shipped in the release package:

- `init-sql/ob-mysql/`: execute every file in filename order in the target OceanBase MySQL database.
- `init-sql/ob-oracle/`: execute every file in filename order as the target OceanBase Oracle schema user.

Keep `CREST_FLYWAY_ENABLED=false` for production OceanBase startup so Crest only connects to an already initialized external metadata database. Set `CREST_FLYWAY_ENABLED=true` only for development or controlled validation runs where the application is allowed to initialize an empty OceanBase database.
`ob-mysql` uses `classpath:db/migration-ob-mysql`; these scripts keep the MySQL-compatible schema while removing MySQL-only table engine options that OceanBase reports as warnings. MySQL demo migrations are not loaded for OceanBase metadata databases.

For Kubernetes, keep the default manifests on MySQL unless an overlay explicitly sets:

```yaml
CREST_DB_TYPE=ob-oracle
CREST_DB_DRIVER_CLASS_NAME=com.oceanbase.jdbc.Driver
CREST_DB_HOST=<obproxy-or-observer-host>
CREST_DB_PORT=2883
CREST_FLYWAY_ENABLED=false
CREST_FLYWAY_LOCATIONS=classpath:db/migration-ob-oracle
```

Use `ob_user@tenant#cluster` through OBProxy and `ob_user@tenant` for observer direct connections.

## Implementation Rules

- Do not bulk-replace SQL. Each SQL branch must be reviewed and tested.
- Keep MySQL migrations unchanged.
- Keep OB MySQL migrations under `classpath:db/migration-ob-mysql`; do not copy large MySQL demo migrations into the OB path.
- Keep OB Oracle migrations under `classpath:db/migration-ob-oracle`.
- Keep release-package initialization SQL in sync with metadata migrations so offline enterprise deployments can initialize OceanBase before service startup.
- Use metadata database dialect helpers for new Java SQL.
- Use MyBatis `databaseId` for mapper SQL that needs database-specific syntax.
- Keep long demo payloads out of fragile single-statement Oracle SQL where CLOB handling is required.
