#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { readdirSync, statSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const overlay = process.argv[2] || "deploy/kubernetes";

function fail(message) {
  console.error(`k8s-check: ${message}`);
  process.exit(1);
}

function info(message) {
  console.log(`k8s-check: ${message}`);
}

function assert(condition, message) {
  if (!condition) {
    fail(message);
  }
}

function renderOverlay() {
  const overlayPath = path.resolve(repoRoot, overlay);
  const files = manifestFiles(overlayPath);
  assert(files.length > 0, `${overlay} must contain Kubernetes YAML files`);
  if (files.length > 1) {
    execFileSync("ruby", [
      "-ryaml",
      "-e",
      "ARGV.each { |f| docs = YAML.load_stream(File.read(f)).compact; abort(\"#{f} must contain exactly one Kubernetes resource\") unless docs.length == 1 }",
      ...files,
    ], {
      cwd: repoRoot,
      stdio: ["ignore", "ignore", "pipe"],
    });
  }
  const json = execFileSync("ruby", [
    "-ryaml",
    "-rjson",
    "-e",
    "items = ARGV.flat_map { |f| YAML.load_stream(File.read(f)).compact }; puts({kind: 'List', items: items}.to_json)",
    ...files,
  ], {
    cwd: repoRoot,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
  return JSON.parse(json);
}

function manifestFiles(targetPath) {
  const stat = statSync(targetPath);
  if (stat.isFile()) {
    return yamlFile(targetPath) ? [targetPath] : [];
  }
  return readdirSync(targetPath)
    .flatMap((name) => manifestFiles(path.join(targetPath, name)))
    .sort();
}

function yamlFile(filePath) {
  return filePath.endsWith(".yaml") || filePath.endsWith(".yml");
}

function byKindAndName(items, kind, name) {
  return items.find((item) => item.kind === kind && item.metadata?.name === name);
}

function allByKind(items, kind) {
  return items.filter((item) => item.kind === kind);
}

function checkResourceShapes(items) {
  const allowedResources = new Set([
    "apps/v1/Deployment",
    "v1/ConfigMap",
    "v1/PersistentVolumeClaim",
    "v1/Secret",
    "v1/Service",
  ]);
  for (const item of items) {
    const resourceId = `${item.apiVersion}/${item.kind}`;
    assert(allowedResources.has(resourceId), `unexpected Kubernetes resource ${resourceId}`);
    assert(typeof item.metadata?.name === "string" && item.metadata.name.length > 0,
      `${resourceId} must set metadata.name`);
  }
}

function container(deployment) {
  return deployment?.spec?.template?.spec?.containers?.[0];
}

function envMap(containerSpec) {
  return Object.fromEntries((containerSpec?.env || []).map((item) => [item.name, item]));
}

function envValue(containerSpec, name) {
  return envMap(containerSpec)[name]?.value;
}

function hasEnvFrom(containerSpec, type, name) {
  return (containerSpec?.envFrom || []).some((item) => item[type]?.name === name);
}

function envFromRef(containerSpec, type, name) {
  return (containerSpec?.envFrom || []).find((item) => item[type]?.name === name);
}

function hasVolume(deployment, name, type) {
  const volume = (deployment?.spec?.template?.spec?.volumes || []).find((item) => item.name === name);
  return Boolean(volume?.[type]);
}

function hasVolumeMount(containerSpec, name, mountPath) {
  return (containerSpec?.volumeMounts || []).some((item) => item.name === name && item.mountPath === mountPath);
}

function assertPodSecurityContext(deployment, name, userId, groupId = userId, fsGroup = userId) {
  const securityContext = deployment?.spec?.template?.spec?.securityContext || {};
  assert(securityContext.runAsNonRoot === true, `${name} must set runAsNonRoot=true`);
  assert(securityContext.runAsUser === userId, `${name} must set runAsUser=${userId}`);
  assert(securityContext.runAsGroup === groupId, `${name} must set runAsGroup=${groupId}`);
  if (fsGroup !== null && fsGroup !== undefined) {
    assert(securityContext.fsGroup === fsGroup, `${name} must set fsGroup=${fsGroup}`);
    assert(securityContext.fsGroupChangePolicy === "OnRootMismatch",
      `${name} must set fsGroupChangePolicy=OnRootMismatch`);
  }
  assert(securityContext.seccompProfile?.type === "RuntimeDefault",
    `${name} must set seccompProfile RuntimeDefault`);
}

function assertContainerSecurityContext(containerSpec, name) {
  const securityContext = containerSpec?.securityContext || {};
  assert(securityContext.allowPrivilegeEscalation === false,
    `${name} must disable privilege escalation`);
  assert(securityContext.readOnlyRootFilesystem === true,
    `${name} must use a read-only root filesystem`);
  assert((securityContext.capabilities?.drop || []).includes("ALL"),
    `${name} must drop all Linux capabilities`);
}

function checkConfig(items) {
  const configMap = byKindAndName(items, "ConfigMap", "crest-env");
  assert(configMap, "missing ConfigMap crest-env");
  const data = configMap.data || {};
  const expected = {
    CREST_DB_TYPE: "ob-oracle",
    CREST_PRODUCTION_MODE: "true",
    CREST_LOAD_DEMO: "false",
    CREST_FLYWAY_ENABLED: "false",
    SPRING_PROFILES_ACTIVE: "standalone",
    CREST_REDIS_HOST: "crest-redis",
    CREST_REDIS_PORT: "6379",
    CREST_REDIS_DATABASE: "0",
  };
  for (const [key, value] of Object.entries(expected)) {
    assert(data[key] === value, `crest-env ${key} must be ${value}`);
  }
  for (const key of ["CREST_DB_NAME", "CREST_DB_PARAMS", "CREST_REDIS_CLUSTER_NODES", "CREST_REDIS_USERNAME"]) {
    assert(!(key in data), `crest-env must not require optional key ${key}`);
  }
}

function checkBackendDeployment(items) {
  const deployment = byKindAndName(items, "Deployment", "crest-service");
  assert(deployment, "missing backend Deployment crest-service");
  assert(deployment.spec?.replicas === 1, "crest-service must run 1 replica in minimal deployment");
  assert((deployment.spec?.template?.spec?.initContainers || []).length === 0,
    "crest-service must not use initContainers in cloud-platform manifest");
  assertPodSecurityContext(deployment, "crest-service", 10001);

  const appContainer = container(deployment);
  assertContainerSecurityContext(appContainer, "crest-service");
  assert(!appContainer.command && !appContainer.args, "crest-service must not override command or args");
  assert(envValue(appContainer, "CREST_RUNTIME_ROLE") === "all",
    "crest-service must set CREST_RUNTIME_ROLE=all");
  assert(envMap(appContainer).CREST_WORKER_ID?.valueFrom?.fieldRef?.fieldPath === "metadata.name",
    "crest-service must derive CREST_WORKER_ID from pod name");
  assert(hasEnvFrom(appContainer, "configMapRef", "crest-env"), "crest-service must load crest-env");
  assert(hasEnvFrom(appContainer, "secretRef", "crest-db-secret"), "crest-service must load crest-db-secret");
  assert(hasEnvFrom(appContainer, "secretRef", "crest-redis-secret"),
    "crest-service must load crest-redis-secret");
  assert(envFromRef(appContainer, "secretRef", "crest-redis-secret").secretRef.optional !== true,
    "crest-service must require crest-redis-secret");
  assert(appContainer?.readinessProbe && appContainer?.livenessProbe,
    "crest-service must configure readiness/liveness probes");
  assert(appContainer.readinessProbe?.httpGet && appContainer.livenessProbe?.httpGet,
    "crest-service probes must use HTTP GET");
  assert(hasVolume(deployment, "crest-data", "persistentVolumeClaim"), "crest-service must mount crest-data PVC");
  assert(hasVolume(deployment, "crest-logs", "emptyDir"), "crest-service must mount writable logs emptyDir");
  assert(hasVolume(deployment, "crest-cache", "emptyDir"), "crest-service must mount writable cache emptyDir");
  assert(hasVolume(deployment, "crest-tmp", "emptyDir"), "crest-service must mount writable tmp emptyDir");
  assert(hasVolumeMount(appContainer, "crest-data", "/opt/crest/data"),
    "crest-service must mount persistent data path");
  assert(hasVolumeMount(appContainer, "crest-logs", "/opt/crest/logs"),
    "crest-service must mount writable log path");
  assert(hasVolumeMount(appContainer, "crest-cache", "/opt/crest/cache"),
    "crest-service must mount writable cache path");
  assert(hasVolumeMount(appContainer, "crest-tmp", "/tmp"),
    "crest-service must mount writable /tmp path");
}

function checkRedisDeployment(items) {
  const deployment = byKindAndName(items, "Deployment", "crest-redis");
  assert(deployment, "missing Redis Deployment crest-redis");
  assert(deployment.spec?.replicas === 1, "crest-redis must run 1 replica");
  assert((deployment.spec?.template?.spec?.initContainers || []).length === 0,
    "crest-redis must not use initContainers");
  assertPodSecurityContext(deployment, "crest-redis", 999);

  const appContainer = container(deployment);
  assertContainerSecurityContext(appContainer, "crest-redis");
  assert(!appContainer.command, "crest-redis must not override command");
  const args = appContainer.args || [];
  assert(args.includes("redis-server") && args.includes("/etc/redis/redis.conf"),
    "crest-redis must start with redis-server /etc/redis/redis.conf");
  assert(args.includes("--requirepass") && args.includes("$(CREST_REDIS_PASSWORD)"),
    "crest-redis must use CREST_REDIS_PASSWORD");
  assert(hasEnvFrom(appContainer, "secretRef", "crest-redis-secret"),
    "crest-redis must load crest-redis-secret");
  assert(appContainer?.readinessProbe?.tcpSocket && appContainer?.livenessProbe?.tcpSocket,
    "crest-redis probes must use TCP socket checks");
  assert(hasVolume(deployment, "redis-config", "configMap"), "crest-redis must mount Redis ConfigMap");
  assert(hasVolume(deployment, "redis-data", "persistentVolumeClaim"), "crest-redis must mount redis data PVC");
  assert(hasVolume(deployment, "redis-tmp", "emptyDir"), "crest-redis must mount writable tmp emptyDir");
  assert(hasVolumeMount(appContainer, "redis-config", "/etc/redis/redis.conf"),
    "crest-redis must mount redis.conf");
  assert(hasVolumeMount(appContainer, "redis-data", "/data"),
    "crest-redis must mount /data");
  assert(hasVolumeMount(appContainer, "redis-tmp", "/tmp"),
    "crest-redis must mount writable /tmp path");
}

function checkFrontendDeployment(items) {
  const deployment = byKindAndName(items, "Deployment", "crest");
  assert(deployment, "missing frontend Deployment crest");
  assert(deployment.spec?.replicas === 1, "frontend must run 1 replica in minimal deployment");
  assert((deployment.spec?.template?.spec?.initContainers || []).length === 0,
    "frontend must not use initContainers in cloud-platform manifest");
  assertPodSecurityContext(deployment, "frontend", 10001, 10001, null);
  const appContainer = container(deployment);
  assertContainerSecurityContext(appContainer, "frontend");
  assert(!appContainer.command && !appContainer.args, "frontend must not override command or args");
  assert(appContainer?.readinessProbe?.httpGet && appContainer?.livenessProbe?.httpGet,
    "frontend probes must use HTTP GET");
  for (const [volumeName, mountPath] of [
    ["nginx-run", "/var/run/nginx"],
    ["nginx-client-temp", "/var/cache/nginx/client_temp"],
    ["nginx-proxy-temp", "/var/cache/nginx/proxy_temp"],
    ["nginx-fastcgi-temp", "/var/cache/nginx/fastcgi_temp"],
    ["nginx-uwsgi-temp", "/var/cache/nginx/uwsgi_temp"],
    ["nginx-scgi-temp", "/var/cache/nginx/scgi_temp"],
  ]) {
    assert(hasVolume(deployment, volumeName, "emptyDir"), `frontend must define ${volumeName} emptyDir`);
    assert(hasVolumeMount(appContainer, volumeName, mountPath), `frontend must mount ${mountPath}`);
  }
}

function checkSupportResources(items) {
  const deployments = allByKind(items, "Deployment").map((item) => item.metadata?.name).sort();
  assert(JSON.stringify(deployments) === JSON.stringify(["crest", "crest-redis", "crest-service"]),
    `minimal deployment must only contain crest, crest-redis and crest-service Deployments, got ${deployments.join(", ")}`);

  assert(allByKind(items, "HorizontalPodAutoscaler").length === 0, "minimal deployment must not include HPA");
  assert(allByKind(items, "PodDisruptionBudget").length === 0, "minimal deployment must not include PDB");
  assert(!byKindAndName(items, "ConfigMap", "crest-app-config"),
    "minimal deployment must not mount application.yml ConfigMap");
  assert(allByKind(items, "Namespace").length === 0, "minimal deployment must not create Namespace");
  assert(allByKind(items, "ServiceAccount").length === 0, "minimal deployment must not require ServiceAccount");

  const pvc = byKindAndName(items, "PersistentVolumeClaim", "crest-data");
  assert(pvc, "missing PVC crest-data");
  assert((pvc.spec?.accessModes || []).includes("ReadWriteOnce"), "crest-data PVC must use ReadWriteOnce");

  const redisPvc = byKindAndName(items, "PersistentVolumeClaim", "crest-redis-data");
  assert(redisPvc, "missing PVC crest-redis-data");
  assert((redisPvc.spec?.accessModes || []).includes("ReadWriteOnce"), "crest-redis-data PVC must use ReadWriteOnce");

  assert(byKindAndName(items, "ConfigMap", "crest-redis-config"), "missing Redis ConfigMap crest-redis-config");
  const dbSecret = byKindAndName(items, "Secret", "crest-db-secret");
  const redisSecret = byKindAndName(items, "Secret", "crest-redis-secret");
  assert(dbSecret, "missing DB Secret crest-db-secret");
  assert(redisSecret, "missing Redis Secret crest-redis-secret");
  for (const key of ["CREST_PROMETHEUS_TOKEN", "CREST_SM4_KEY", "CREST_CRYPTO_MODE"]) {
    assert(!(key in (dbSecret.stringData || {})), `crest-db-secret must not require optional key ${key}`);
  }

  const backendService = byKindAndName(items, "Service", "crest-service");
  assert(backendService?.spec?.selector?.["app.kubernetes.io/name"] === "crest-service",
    "crest-service must route to backend pod");
  assert(backendService?.spec?.selector?.["app.kubernetes.io/component"] === "backend",
    "crest-service must select backend component");

  const frontendService = byKindAndName(items, "Service", "crest");
  assert(frontendService?.spec?.selector?.["app.kubernetes.io/name"] === "crest",
    "crest Service must route to frontend pod");

  const redisService = byKindAndName(items, "Service", "crest-redis");
  assert(redisService?.spec?.selector?.["app.kubernetes.io/name"] === "crest-redis",
    "crest-redis Service must route to Redis pod");
}

const rendered = renderOverlay();
const items = rendered.items || [];

checkResourceShapes(items);
checkConfig(items);
checkBackendDeployment(items);
checkRedisDeployment(items);
checkFrontendDeployment(items);
checkSupportResources(items);

info(`${overlay} passed minimal Kubernetes checks`);
