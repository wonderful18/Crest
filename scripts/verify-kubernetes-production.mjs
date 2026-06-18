#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import path from "node:path";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const overlay = process.argv[2] || "deploy/kubernetes/production";

function fail(message) {
  console.error(`production-k8s: ${message}`);
  process.exit(1);
}

function info(message) {
  console.log(`production-k8s: ${message}`);
}

function assert(condition, message) {
  if (!condition) {
    fail(message);
  }
}

function renderOverlay() {
  // 先由 kustomize 渲染，再用 Ruby 标准库解析 YAML，避免 kubectl apply 合并本地状态。
  const yaml = execFileSync("kubectl", ["kustomize", overlay], {
    cwd: repoRoot,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
  const json = execFileSync("ruby", [
    "-ryaml",
    "-rjson",
    "-e",
    "items = YAML.load_stream(ARGF.read).compact; puts({kind: 'List', items: items}.to_json)",
  ], {
    cwd: repoRoot,
    input: yaml,
    encoding: "utf8",
    stdio: ["pipe", "pipe", "pipe"],
  });
  return JSON.parse(json);
}

function byKindAndName(items, kind, name) {
  return items.find((item) => item.kind === kind && item.metadata?.name === name);
}

function container(deployment) {
  return deployment?.spec?.template?.spec?.containers?.[0];
}

function initContainer(deployment, name) {
  return (deployment?.spec?.template?.spec?.initContainers || []).find((item) => item.name === name);
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

function hasVolume(deployment, name, type) {
  const volume = (deployment?.spec?.template?.spec?.volumes || []).find((item) => item.name === name);
  return Boolean(volume?.[type]);
}

function hasVolumeMount(containerSpec, name, mountPath) {
  return (containerSpec?.volumeMounts || []).some((item) => item.name === name && item.mountPath === mountPath);
}

function checkBackendDeployment(items, name, role) {
  const deployment = byKindAndName(items, "Deployment", name);
  assert(deployment, `missing Deployment ${name}`);
  assert(deployment.spec?.replicas >= 2, `${name} must run at least 2 replicas`);
  assert(deployment.spec?.template?.spec?.affinity?.podAntiAffinity, `${name} must configure podAntiAffinity`);
  assert((deployment.spec?.template?.spec?.topologySpreadConstraints || []).length > 0,
    `${name} must configure topologySpreadConstraints`);

  const appContainer = container(deployment);
  assert(envValue(appContainer, "CREST_RUNTIME_ROLE") === role, `${name} must set CREST_RUNTIME_ROLE=${role}`);
  assert(envMap(appContainer).CREST_WORKER_ID?.valueFrom?.fieldRef?.fieldPath === "metadata.name",
    `${name} must derive CREST_WORKER_ID from pod name`);
  assert(hasEnvFrom(appContainer, "configMapRef", "crest-env"), `${name} must load crest-env`);
  assert(hasEnvFrom(appContainer, "secretRef", "crest-db-secret"), `${name} must load crest-db-secret`);
  assert(hasEnvFrom(appContainer, "secretRef", "crest-redis-secret"), `${name} must load crest-redis-secret`);
  assert(appContainer?.startupProbe && appContainer?.readinessProbe && appContainer?.livenessProbe,
    `${name} must configure startup/readiness/liveness probes`);
  const redisInit = initContainer(deployment, "wait-for-redis");
  const redisInitCommand = (redisInit?.command || []).join("\n");
  assert(redisInitCommand.includes("CREST_REDIS_CLUSTER_NODES"),
    `${name} wait-for-redis must read Redis Cluster nodes`);
  assert(redisInitCommand.includes("for REDIS_ENDPOINT in \"$@\"")
      && redisInitCommand.includes("REDIS_AVAILABLE")
      && redisInitCommand.includes("waiting for redis cluster"),
    `${name} wait-for-redis must accept any reachable Redis Cluster seed`);

  // cache、log、tmp 只能使用 Pod 本地临时目录，业务文件统一走 RWX PVC。
  assert(hasVolume(deployment, "crest-data", "persistentVolumeClaim"), `${name} must mount crest-data PVC`);
  assert(hasVolume(deployment, "crest-cache", "emptyDir"), `${name} cache must use emptyDir`);
  assert(hasVolume(deployment, "crest-logs", "emptyDir"), `${name} logs must use emptyDir`);
  assert(hasVolume(deployment, "crest-tmp", "emptyDir"), `${name} tmp must use emptyDir`);
  assert(hasVolumeMount(appContainer, "crest-data", "/opt/crest/data/exportData"),
    `${name} must mount shared exportData path`);
}

function checkFrontendDeployment(items) {
  const deployment = byKindAndName(items, "Deployment", "crest");
  assert(deployment, "missing frontend Deployment crest");
  assert(deployment.spec?.replicas >= 2, "frontend must run at least 2 replicas");
  assert(deployment.spec?.template?.spec?.affinity?.podAntiAffinity, "frontend must configure podAntiAffinity");
  assert((deployment.spec?.template?.spec?.topologySpreadConstraints || []).length > 0,
    "frontend must configure topologySpreadConstraints");
}

function checkConfig(items) {
  const configMap = byKindAndName(items, "ConfigMap", "crest-env");
  assert(configMap, "missing ConfigMap crest-env");
  const data = configMap.data || {};
  const expected = {
    CREST_PRODUCTION_MODE: "true",
    CREST_LOAD_DEMO: "false",
    CREST_FLYWAY_ENABLED: "false",
    SPRING_FLYWAY_ENABLED: "false",
    SPRING_CACHE_TYPE: "redis",
    MANAGEMENT_HEALTH_REDIS_ENABLED: "true",
    CREST_TASK_QUEUE_ENABLED: "true",
    CREST_WEBSOCKET_BROADCAST_ENABLED: "true",
    CREST_REDIS_CLUSTER_MAX_REDIRECTS: "3",
    CREST_REDIS_SSL_ENABLED: "false",
    CREST_REDIS_CONNECT_TIMEOUT: "2s",
    CREST_REDIS_TIMEOUT: "5s",
    CREST_REDIS_CLUSTER_REFRESH_ADAPTIVE: "true",
    CREST_REDIS_CLUSTER_REFRESH_PERIOD: "30s",
    CREST_REDIS_CLUSTER_DYNAMIC_REFRESH_SOURCES: "true",
    CREST_SCHEDULED_TASK_STREAM: "crest:stream:scheduled-task",
    CREST_SCHEDULED_TASK_CONSUMER_GROUP: "crest-scheduled-workers",
  };
  for (const [key, value] of Object.entries(expected)) {
    assert(data[key] === value, `crest-env ${key} must be ${value}`);
  }
}

function checkSupportResources(items) {
  const pvc = byKindAndName(items, "PersistentVolumeClaim", "crest-data");
  assert(pvc, "missing PVC crest-data");
  assert((pvc.spec?.accessModes || []).includes("ReadWriteMany"), "crest-data PVC must use ReadWriteMany");

  const service = byKindAndName(items, "Service", "crest-service");
  assert(service?.spec?.selector?.["app.kubernetes.io/name"] === "crest-service-api",
    "crest-service must route to api pods");

  for (const name of ["crest", "crest-service-api", "crest-service-scheduler", "crest-service-worker"]) {
    assert(byKindAndName(items, "PodDisruptionBudget", name), `missing PDB ${name}`);
    const hpa = byKindAndName(items, "HorizontalPodAutoscaler", name);
    assert(hpa, `missing HPA ${name}`);
    assert(hpa.spec?.minReplicas >= 2, `${name} HPA must keep at least 2 replicas`);
  }
}

const rendered = renderOverlay();
const items = rendered.items || [];

checkConfig(items);
checkFrontendDeployment(items);
checkBackendDeployment(items, "crest-service-api", "api");
checkBackendDeployment(items, "crest-service-scheduler", "scheduler");
checkBackendDeployment(items, "crest-service-worker", "worker");
checkSupportResources(items);

info(`${overlay} passed production topology checks`);
