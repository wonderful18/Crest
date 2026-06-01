#!/bin/bash

INSTALL_TYPE='install'
title_count=1

CURRENT_DIR=$(
   cd "$(dirname "$0")"
   pwd
)

function log() {
   echo -e "${1}" 2>&1 | tee -a ${CURRENT_DIR}/install.log
}

function log_title () {
   log "${title_count}. ${1}"
   let title_count++
}

function log_content () {
   log "\t${1}"
}

function prop {
   [ -f "$1" ] | grep -P "^\s*[^#]?${2}=.*$" $1 | cut -d'=' -f2
}

function load_env_file() {
   local env_file=$1
   local key value
   while IFS='=' read -r key value || [[ -n "$key" ]]; do
      [[ "$key" =~ ^CREST_[A-Z0-9_]+$ ]] || continue
      export "${key}=${value}"
   done < "$env_file"
}

function check_and_prepare_env_params() {
   log "当前时间 : $(date)"
   log_title "检查安装环境并初始化环境变量"

   cd ${CURRENT_DIR}
   if [ -f /usr/bin/crestctl ]; then
      ctl_cmd=/usr/bin/crestctl
      existing_version=$($ctl_cmd version 2>/dev/null | awk '/current version is/{print $4; exit}')
      if [[ ! $existing_version =~ ^v?1\.5\. ]];then
         echo "检测到已安装的 Crest 版本：${existing_version:-unknown}"
         echo "v1.5 系列面向全新环境安装，不支持从早期开发版本直接原地升级。"
         echo "请先备份并导出业务资源，再卸载旧环境后执行全新安装。"
         exit 1;
      fi
      # 获取已安装的 Crest 的运行目录
      CREST_BASE=$(grep "^CREST_BASE=" $ctl_cmd | cut -d'=' -f2)
      CREST_BASE_OLD=$CREST_BASE
      sed -i -e "s#CREST_BASE=.*#CREST_BASE=${CREST_BASE}#g" crestctl
      \cp crestctl /usr/local/bin && chmod +x /usr/local/bin/crestctl
      ln -sf /usr/local/bin/crestctl /usr/bin/crestctl 2>/dev/null

      log_content "停止 Crest 服务"
      if [[ -f /etc/systemd/system/crest.service ]];then
         systemctl stop crest
      else
         $ctl_cmd stop
      fi

      INSTALL_TYPE='upgrade'
   fi

   set -a
   source ${CURRENT_DIR}/install.conf
   if [[ $CREST_BASE_OLD ]];then
      CREST_BASE=$CREST_BASE_OLD
      export CREST_BASE=$CREST_BASE_OLD
   fi
   if [[ -d $CREST_BASE ]] && [[ -f $CREST_BASE/crest/.env ]]; then
      load_env_file "$CREST_BASE/crest/.env"
      INSTALL_TYPE='upgrade'
      log_content "升级安装"
   else
      INSTALL_TYPE='install'
      mkdir -p ${CREST_BASE}
      log_content "全新安装"
   fi
   set +a
   CREST_INTERNAL_LITE=${CREST_INTERNAL_LITE:-true}
   CREST_EXTERNAL_MYSQL=${CREST_EXTERNAL_MYSQL:-false}
   case "$(echo "$CREST_EXTERNAL_MYSQL" | tr '[:upper:]' '[:lower:]')" in
      true|1|yes)
         CREST_EXTERNAL_MYSQL=true
         ;;
      false|0|no)
         CREST_EXTERNAL_MYSQL=false
         ;;
      *)
         echo "CREST_EXTERNAL_MYSQL 只支持 true 或 false"
         exit 1
         ;;
   esac
   export CREST_EXTERNAL_MYSQL
   CREST_CRYPTO_MODE=${CREST_CRYPTO_MODE:-standard}
   case "$CREST_CRYPTO_MODE" in
      standard|sm-suite) ;;
      *)
         echo "CREST_CRYPTO_MODE 只支持 standard 或 sm-suite"
         exit 1
         ;;
   esac
   export CREST_CRYPTO_MODE

   CREST_LOAD_DEMO=${CREST_LOAD_DEMO:-true}
   case "$(echo "$CREST_LOAD_DEMO" | tr '[:upper:]' '[:lower:]')" in
      true|1|yes)
         CREST_LOAD_DEMO=true
         CREST_FLYWAY_LOCATIONS="classpath:db/migration,classpath:db/demo-migration"
         log_content "初始化内置演示资源"
         ;;
      false|0|no)
         CREST_LOAD_DEMO=false
         CREST_FLYWAY_LOCATIONS="classpath:db/migration"
         log_content "跳过内置演示资源"
         ;;
      *)
         echo "CREST_LOAD_DEMO 只支持 true 或 false"
         exit 1
         ;;
   esac
   export CREST_LOAD_DEMO
   export CREST_FLYWAY_LOCATIONS

   if [[ -z "$CREST_DB_PASSWORD" ]]; then
      CREST_DB_PASSWORD=$(LC_ALL=C tr -dc 'A-Za-z0-9_@%+=' </dev/urandom | head -c 32)
      export CREST_DB_PASSWORD
   fi
   if [[ -z "$CREST_AES_KEY" ]]; then
      CREST_AES_KEY=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 32)
      export CREST_AES_KEY
   fi
   if [[ -z "$CREST_AES_IV" ]]; then
      CREST_AES_IV=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 16)
      export CREST_AES_IV
   fi
   if [[ "$CREST_CRYPTO_MODE" == "sm-suite" ]] && [[ -z "$CREST_SM4_KEY" ]]; then
      CREST_SM4_KEY=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 16)
      export CREST_SM4_KEY
   fi
   if [[ -z "$CREST_INITIAL_PASSWORD" ]]; then
      CREST_INITIAL_PASSWORD=$(LC_ALL=C tr -dc 'A-Za-z0-9_@%+=' </dev/urandom | head -c 20)
      export CREST_INITIAL_PASSWORD
   fi

   read available_disk <<< $(df -H --output=avail "${CREST_BASE}" | tail -1)
   disk_num=${available_disk%[KMGTP]}
   disk_unit=${available_disk##*[0-9.]}
   case $disk_unit in
     K) disk_gb=$(awk -v i="$disk_num" 'BEGIN{printf "%.0f\n", i / 1024 / 1024}') ;;
     M) disk_gb=$(awk -v i="$disk_num" 'BEGIN{printf "%.0f\n", i / 1024}') ;;
     G) disk_gb=${disk_num%.*} ;;
     T) disk_gb=$(awk -v i="$disk_num" 'BEGIN{printf "%.0f\n", 1024 * i}') ;;
     *) disk_gb=${disk_num%.*} ;;
   esac
   [[ $disk_gb -lt 20 ]] && log_content "\033[31m[警告] Crest 运行目录所在磁盘剩余空间不足 20G 可能无法正常启动!\033[0m"
   }

function set_run_base_path() {
   log_title "设置运行目录"
   CREST_RUN_BASE=$CREST_BASE/crest
   export CREST_RUN_BASE
   CREST_LOG_PATH=${CREST_LOG_PATH:-/opt/crest/logs/crest}
   CREST_CACHE_PATH=${CREST_CACHE_PATH:-/opt/crest/cache}
   CREST_SUBSTITULE_PATH=${CREST_SUBSTITULE_PATH:-${CREST_CACHE_PATH}/substitule.json}
   export CREST_LOG_PATH
   export CREST_CACHE_PATH
   export CREST_SUBSTITULE_PATH
   CONF_FOLDER=${CREST_RUN_BASE}/conf
   TEMPLATES_FOLDER=${CREST_RUN_BASE}/templates
   log_content "运行目录 $CREST_RUN_BASE"
   log_content "配置文件目录 $CONF_FOLDER"
}

function prepare_crest_run_base() {
   log_title "初始化运行目录"
   cd ${CURRENT_DIR}
   mkdir -p ${CREST_RUN_BASE}
   log_content "复制安装文件到运行目录"
   cp -r ./crest/* ${CREST_RUN_BASE}/

   cd $CREST_RUN_BASE
   env | grep CREST_ >.env

   mkdir -p ${CREST_RUN_BASE}/{cache,logs,conf}
   mkdir -p ${CREST_RUN_BASE}/data/{mysql,static-resource,appearance,exportData,excel,font,i18n,plugin}
   chown -R 10001:10001 \
      ${CREST_RUN_BASE}/cache \
      ${CREST_RUN_BASE}/logs \
      ${CREST_RUN_BASE}/data/static-resource \
      ${CREST_RUN_BASE}/data/appearance \
      ${CREST_RUN_BASE}/data/exportData \
      ${CREST_RUN_BASE}/data/excel \
      ${CREST_RUN_BASE}/data/font \
      ${CREST_RUN_BASE}/data/i18n \
      ${CREST_RUN_BASE}/data/plugin 2>/dev/null || true

   if [[ "${CREST_INTERNAL_LITE}" != "true" ]]; then
      mkdir -p ${CREST_RUN_BASE}/task/logs
      chmod 777 ${CREST_RUN_BASE}/task/logs
   fi

   if [ "${CREST_EXTERNAL_MYSQL}" = "false" ]; then
      export CREST_DB_PORT=3306
   else
      awk '
         /^  mysql-crest:/ { skip = 1; next }
         /^  crest-service:/ { skip = 0 }
         skip { next }
         /^  crest-service:/ { in_backend = 1; print; next }
         /^  [a-zA-Z0-9_-]+:/ { in_backend = 0 }
         in_backend && /^    depends_on:/ { skip_dep = 1; next }
         skip_dep && /^    [a-zA-Z0-9_-]+:/ { skip_dep = 0 }
         skip_dep { next }
         !skip { print }
      ' docker-compose.yml > docker-compose.yml.tmp && mv docker-compose.yml.tmp docker-compose.yml
   fi

   log_content "调整配置文件参数"
   cd $CREST_RUN_BASE
   cp -r $TEMPLATES_FOLDER/* $CONF_FOLDER

   cd ${TEMPLATES_FOLDER}
   templates_files=( application.yml mysql.env )
   for i in ${templates_files[@]}; do
      if [ -f $i ]; then
         envsubst < $i > $CONF_FOLDER/$i
      fi
   done

}

function update_crestctl() {
   log_title "安装 crestctl 命令行工具"
   log_content "安装至 /usr/local/bin/crestctl & /usr/bin/crestctl"
   cd ${CURRENT_DIR}
   sed -i -e "s#CREST_BASE=.*#CREST_BASE=${CREST_BASE}#g" crestctl
   \cp crestctl /usr/local/bin && chmod +x /usr/local/bin/crestctl
   ln -sf /usr/local/bin/crestctl /usr/bin/crestctl 2>/dev/null
}

function prepare_system_settings() {
   log_title "修改操作系统相关设置"
   if which getenforce >/dev/null 2>&1 && [ $(getenforce) == "Enforcing" ];then
      log_content  "关闭 SELINUX"
      setenforce 0
      sed -i "s/SELINUX=enforcing/SELINUX=disabled/g" /etc/selinux/config
   fi

   if which firewall-cmd >/dev/null 2>&1; then
      if systemctl is-active firewalld &>/dev/null ;then
         log_content "开启防火墙端口 ${CREST_PORT}"
         firewall-cmd --zone=public --add-port=${CREST_PORT}/tcp --permanent
         firewall-cmd --reload
      else
         log_content "防火墙未开启，忽略端口开放"
      fi
   fi
}

function install_docker() {
   log_title "安装 docker"
   #Install docker
   ##Install Latest Stable Docker Release
   cd ${CURRENT_DIR}

   if which docker >/dev/null 2>&1; then
      log_content "检测到 Docker 已安装，跳过安装步骤"
      log_content "启动 Docker "
      service docker start >/dev/null 2>&1 | tee -a ${CURRENT_DIR}/install.log
   else
      if [[ -d docker ]]; then
         log_content "离线安装 docker"
         cp docker/bin/* /usr/bin/
         cp docker/service/docker.service /etc/systemd/system/
         chmod +x /usr/bin/docker*
         chmod 644 /etc/systemd/system/docker.service
      else
         log_content "在线安装 docker"
         curl -fsSL https://get.docker.com -o get-docker.sh 2>&1 | tee -a ${CURRENT_DIR}/install.log
         if [[ ! -f get-docker.sh ]];then
            log_content "docker 在线安装脚本下载失败，请稍候重试"
            exit 1
         fi
         sudo sh get-docker.sh 2>&1 | tee -a ${CURRENT_DIR}/install.log
      fi

      docker_config_folder="/etc/docker"
      if [ ! -d "$docker_config_folder" ];then
         mkdir -p "$docker_config_folder"
         cat <<EOF> $docker_config_folder/daemon.json
         {
            "log-driver": "json-file",
            "log-opts": {
               "max-file": "3",
               "max-size": "10m"
            }
         }
EOF
      fi

      log_content "启动 docker"
      systemctl enable docker >/dev/null 2>&1; systemctl daemon-reload; systemctl start docker 2>&1 | tee -a ${CURRENT_DIR}/install.log

      docker version >/dev/null 2>&1
      if [ $? -ne 0 ]; then
         log_content "docker 安装失败"
         exit 1
      else
         log_content "docker 安装成功"

      fi
   fi
}

function install_docker_compose() {
   log_title "安装 docker-compose"
   #Install docker-compose
   cd ${CURRENT_DIR}
   ##Install Latest Stable Docker Compose Release
   docker-compose version >/dev/null 2>&1
   if [ $? -ne 0 ]; then
      docker compose version >/dev/null 2>&1
      if [ $? -eq 0 ]; then
         echo 'docker compose "$@"' > /usr/bin/docker-compose
         chmod +x /usr/bin/docker-compose
      else
         if [[ -d docker ]]; then
            log_content "离线安装 docker-compose"
            cp docker/bin/docker-compose /usr/bin/
            chmod +x /usr/bin/docker-compose
         else
            log_content "在线安装 docker-compose"
            curl -L https://github.com/docker/compose/releases/download/v2.16.0/docker-compose-$(uname -s | tr A-Z a-z)-$(uname -m) -o /usr/local/bin/docker-compose 2>&1 | tee -a ${CURRENT_DIR}/install.log
            if [[ ! -f /usr/local/bin/docker-compose ]];then
               log_content "docker-compose 下载失败，请稍候重试"
               exit 1
            fi
            chmod +x /usr/local/bin/docker-compose
            ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
         fi
      fi

      docker-compose version >/dev/null
      if [ $? -ne 0 ]; then
         log_content "docker-compose 安装失败"
         exit 1
      else
         log_content "docker-compose 安装成功"
      fi
   else
      log_content "检测到 Docker Compose 已安装，跳过安装步骤"
   fi
   export COMPOSE_HTTP_TIMEOUT=180
}

function load_crest_images() {
   log_title "加载 Crest 镜像"
   cd ${CURRENT_DIR}

   # 加载镜像
   if [[ -d images ]]; then
      shopt -s nullglob
      image_archives=(images/*.tar images/*.tar.gz images/*.tgz)
      if [[ ${#image_archives[@]} -eq 0 ]]; then
         log_content "离线镜像目录为空，后续将从 docker-compose.yml 中定义的镜像仓库拉取"
         return
      fi
      for image_archive in "${image_archives[@]}"; do
         log_content "加载镜像 $(basename "$image_archive")"
         docker load -i "$image_archive" 2>&1 | tee -a ${CURRENT_DIR}/install.log
      done
   else
      log_content "未检测到离线镜像目录，后续将从 docker-compose.yml 中定义的镜像仓库拉取"
   fi
}

function configure_crest_service() {
   log_title "配置 Crest 服务"

   # 判断是否为wsl
   local is_wsl=false
   if grep -qE "(Microsoft|microsoft|WLS)" /proc/version; then
      is_wsl=true
   fi

   if [[ ! -f /etc/systemd/system/crest.service ]];then
      log_content "配置 Crest Service"
      cp ${CREST_RUN_BASE}/bin/crest/crest.service /etc/systemd/system/
      #--- 如果是 WSL，则移除 service 文件中对 docker 的依赖 ---
      if [ "$is_wsl" = true ]; then
         log_content "检测到 WSL 环境，移除 crest.service 中的 Docker 依赖配置"
         sed -i '/docker.service/d' /etc/systemd/system/crest.service
      fi
      #------------------------------------------------------
      chmod 644 /etc/systemd/system/crest.service
      log_content "配置开机自启动"
      systemctl enable crest >/dev/null 2>&1; systemctl daemon-reload | tee -a ${CURRENT_DIR}/install.log
   fi
}

function start_crest_service() {
   log_title "启动 Crest 服务"
   systemctl start crest 2>&1 | tee -a ${CURRENT_DIR}/install.log

   access_port=$CREST_PORT
   echo
   if [[ $INSTALL_TYPE != "upgrade" ]];then
      echo -e "======================= 安装完成 =======================\n" 2>&1 | tee -a ${CURRENT_DIR}/install.log
      echo -e "系统登录信息如下:\n\t访问地址: http://服务器IP:$access_port\n\t用户名: admin\n\t初始密码: ${CREST_INITIAL_PASSWORD}" 2>&1 | tee -a ${CURRENT_DIR}/install.log
   else
      echo -e "======================= 升级完成 =======================\n" 2>&1 | tee -a ${CURRENT_DIR}/install.log
   fi
}

function main() {
   check_and_prepare_env_params
   set_run_base_path
   prepare_crest_run_base
   update_crestctl
   prepare_system_settings
   install_docker
   install_docker_compose
   load_crest_images
   configure_crest_service
   start_crest_service
}

main
