# OceanBase Support Plan

This document tracks production-grade support for OceanBase as both the Crest metadata database and a business datasource.

## Scope

- MySQL remains the default metadata database and must keep its current behavior.
- `ob-mysql` is a reserved metadata database mode that initially uses MySQL-compatible runtime behavior.
- `ob-oracle` is the primary metadata database adaptation target and uses a separate Flyway migration path.
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
obclient -h127.0.0.1 -P2881 -uob_user@ob_mysql#obcluster -pob_password
```

```bash
obclient -h127.0.0.1 -P2881 -uob_user@ob_oracle#obcluster -pob_password
```

## Metadata Database Configuration

Common variables:

| Variable | Description | Default |
| --- | --- | --- |
| `CREST_DB_TYPE` | Metadata database type: `mysql`, `ob-mysql`, or `ob-oracle` | `mysql` |
| `CREST_DB_DRIVER_CLASS_NAME` | Optional JDBC driver override | Type-specific |
| `CREST_DB_URL` | Optional full JDBC URL. If set, it has priority. | Empty |
| `CREST_DB_HOST` | Database host | `127.0.0.1` |
| `CREST_DB_PORT` | Database port | MySQL `3306`, OceanBase usually `2883` |
| `CREST_DB_NAME` | Metadata database name for MySQL-compatible metadata databases | `crest` |
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

When running with Docker Compose files that set `CREST_DB_URL`, provide an explicit OceanBase URL:

```bash
export CREST_DB_URL='jdbc:oceanbase://127.0.0.1:2883/crest'   # ob-mysql
export CREST_DB_URL='jdbc:oceanbase://127.0.0.1:2883'         # ob-oracle
```

## Implementation Rules

- Do not bulk-replace SQL. Each SQL branch must be reviewed and tested.
- Keep MySQL migrations unchanged.
- Keep OB Oracle migrations under `classpath:db/migration-ob-oracle`.
- Use metadata database dialect helpers for new Java SQL.
- Use MyBatis `databaseId` for mapper SQL that needs database-specific syntax.
- Keep long demo payloads out of fragile single-statement Oracle SQL where CLOB handling is required.
