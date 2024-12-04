#!/bin/bash

# ===========================================
# Strangler Fig & ACL 패턴 실습환경 구성 스크립트
# ===========================================

# 사용법 출력
print_usage() {
    cat << EOF
사용법:
    $0 <userid>

설명:
    Strangler Fig & ACL 패턴 실습을 위한 Azure 리소스를 생성합니다.
    리소스 이름이 중복되지 않도록 userid를 prefix로 사용합니다.

예제:
    $0 gappa     # gappa-acl-service 등의 리소스가 생성됨
EOF
}

# 유틸리티 함수
log() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $1" | tee -a $LOG_FILE
}

check_error() {
    if [ $? -ne 0 ]; then
        log "Error: $1"
        exit 1
    fi
}

# 환경 변수 설정
setup_environment() {
    log "환경 변수 설정 중..."

    # 기본 설정
    USERID=$1
    NAME="${USERID}-acl"
    RESOURCE_GROUP="tiu-dgga-rg"
    LOCATION="koreacentral"
    AKS_NAME="${USERID}-aks"
    ACR_NAME="${USERID}cr"

    # Namespace 설정
    REDIS_NS="redis"
    ACL_NS="${USERID}-acl"

    # DB 설정
    POSTGRES_HOST="${NAME}-postgres"
    POSTGRES_DB="legacydb"
    POSTGRES_USER="postgres"
    POSTGRES_PASSWORD="P@ssw0rd"
    DB_SECRET_NAME="${USERID}-db-credentials"

    # MongoDB 설정
    MONGODB_HOST="${USERID}-cqrs-mongodb.${USERID}-database.svc.cluster.local"
    MONGODB_PORT="27017"
    MONGODB_DATABASE="telecomdb"
    MONGODB_USER="root"
    MONGODB_PASSWORD="Passw0rd"

    # Event Hub 설정
    EVENT_HUB_NS="dgga-eventhub-ns"
    EVENT_HUB_NAME="usage-notify-events"

    # 이미지 태그
    IMAGE_TAG="v1"

    # 모니터링용 로그 파일
    LOG_FILE="deployment_${NAME}.log"
}

# AKS 자격 증명 가져오기
get_aks_credentials() {
    log "AKS 자격 증명 가져오는 중..."

    az aks get-credentials \
        --resource-group $RESOURCE_GROUP \
        --name $AKS_NAME \
        --overwrite-existing
    check_error "AKS 자격 증명 가져오기 실패"
}

# ACR 권한 설정
setup_acr_permission() {
    log "ACR pull 권한 확인 중..."

    # AKS의 service principal 확인
    SP_ID=$(az aks show \
        --name $AKS_NAME \
        --resource-group $RESOURCE_GROUP \
        --query servicePrincipalProfile.clientId -o tsv)

    if [ "$SP_ID" = "msi" ] || [ "$SP_ID" = "null" ]; then
        log "AKS가 Managed Identity를 사용하고 있습니다."
        log "ACR pull 권한이 이미 설정되어 있다고 가정합니다."
        return 0
    fi

    log "Service Principal을 사용하는 AKS입니다. ACR 권한을 확인합니다..."
    ACR_ID=$(az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP --query "id" -o tsv)
    check_error "ACR ID 조회 실패"

    az aks update \
        --name $AKS_NAME \
        --resource-group $RESOURCE_GROUP \
        --attach-acr $ACR_ID
    check_error "ACR pull 권한 부여 실패"
}

# Namespace 생성
create_namespaces() {
    log "Namespace 생성 중..."

    kubectl create namespace $ACL_NS 2>/dev/null || true

}

# 애플리케이션 빌드
build_applications() {
    log "애플리케이션 빌드 중..."

    # Gradle 빌드 - 각 서비스별로
    chmod +x gradlew
    ./gradlew common:clean common:build -x test
    ./gradlew acl-service:clean acl-service:build -x test
    ./gradlew kos-mock:clean kos-mock:build -x test
    ./gradlew notification-mock:clean notification-mock:build -x test
    ./gradlew sync-mock:clean sync-mock:build -x test
    ./gradlew usage-generator:clean usage-generator:build -x test
    check_error "Gradle 빌드 실패"

    # Dockerfile 생성 - ACL Service
    cat > Dockerfile-acl-service << EOF
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine
COPY acl-service/build/libs/acl-service.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

    # Dockerfile 생성 - KOS Mock
    cat > Dockerfile-kos-mock << EOF
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine
COPY kos-mock/build/libs/kos-mock.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

    # Dockerfile 생성 - Notification Mock
    cat > Dockerfile-notification-mock << EOF
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine
COPY notification-mock/build/libs/notification-mock.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

    # Dockerfile 생성 - Sync Mock
    cat > Dockerfile-sync-mock << EOF
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine
COPY sync-mock/build/libs/sync-mock.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

    # Dockerfile 생성 - Usage Generator
    cat > Dockerfile-usage-generator << EOF
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine
COPY usage-generator/build/libs/usage-generator.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

    # 각 서비스별 Docker 이미지 빌드
    for service in acl-service kos-mock notification-mock sync-mock usage-generator; do
        log "Building $service..."
        docker build -f Dockerfile-$service \
            -t ${ACR_NAME}.azurecr.io/telecom/$service:$IMAGE_TAG .
        check_error "$service 이미지 빌드 실패"
    done
}

# 이미지 Push
push_images() {
    log "Docker 이미지 Push 중..."

    # ACR 로그인
    az acr login --name $ACR_NAME
    check_error "ACR 로그인 실패"

    # 각 이미지 Push
    for service in acl-service kos-mock notification-mock sync-mock usage-generator; do
        log "Pushing $service..."
        docker push ${ACR_NAME}.azurecr.io/telecom/$service:$IMAGE_TAG
        check_error "$service 이미지 Push 실패"
    done
}

# 기존 리소스 정리
cleanup_resources() {
    log "기존 리소스 정리 중..."

    # Deployments 삭제
    kubectl delete deployment $NAME-service -n $ACL_NS --ignore-not-found
    kubectl delete deployment $NAME-kos -n $ACL_NS --ignore-not-found
    kubectl delete deployment $NAME-notification -n $ACL_NS --ignore-not-found
    kubectl delete deployment $NAME-sync -n $ACL_NS --ignore-not-found
    kubectl delete deployment $NAME-usage -n $ACL_NS --ignore-not-found

    # StatefulSet 삭제
    kubectl delete statefulset $NAME-postgres -n $ACL_NS --ignore-not-found

    # ConfigMap 삭제
    kubectl delete configmap $NAME-config -n $ACL_NS --ignore-not-found
    kubectl delete configmap postgres-init-script -n $ACL_NS --ignore-not-found

    # Secret 삭제
    kubectl delete secret eventhub-secret -n $ACL_NS --ignore-not-found
    kubectl delete secret $DB_SECRET_NAME -n $ACL_NS --ignore-not-found

    # 삭제 완료 대기
    for deployment in service kos notification sync usage; do
        kubectl wait --for=delete deployment/$NAME-$deployment -n $ACL_NS --timeout=60s 2>/dev/null || true
    done
    kubectl wait --for=delete statefulset/$NAME-postgres -n $ACL_NS --timeout=60s 2>/dev/null || true
}

setup_storage() {
    log "Storage Account 및 Blob Container 설정 중..."

    # Storage Account가 없으면 생성
    STORAGE_EXISTS=$(az storage account show \
        --name dggastorage \
        --resource-group $RESOURCE_GROUP \
        --query name \
        --output tsv 2>/dev/null)

    if [ -z "$STORAGE_EXISTS" ]; then
        az storage account create \
            --name dggastorage \
            --resource-group $RESOURCE_GROUP \
            --location $LOCATION \
            --sku Standard_LRS
        check_error "Storage Account 생성 실패"
    fi

    # 연결 문자열 얻기
    STORAGE_CONNECTION_STRING=$(az storage account show-connection-string \
        --name dggastorage \
        --resource-group $RESOURCE_GROUP \
        --query connectionString \
        --output tsv)
    check_error "Storage 연결 문자열 가져오기 실패"

    # Blob Container 생성
    az storage container create \
        --name eventhub-checkpoints \
        --connection-string "$STORAGE_CONNECTION_STRING" \
        2>/dev/null || true

    # Secret으로 저장
    kubectl create secret generic storage-secret \
        --namespace $ACL_NS \
        --from-literal=connection-string="$STORAGE_CONNECTION_STRING" \
        2>/dev/null || true
    check_error "Storage Secret 저장 실패"
}

# Event Hub 네임스페이스 및 이벤트 허브 생성
setup_event_hub() {
   log "Event Hub 확인 중..."

   # Event Hub 네임스페이스가 이미 있는지 확인
   EXISTING_NS=$(az eventhubs namespace show \
       --name $EVENT_HUB_NS \
       --resource-group $RESOURCE_GROUP \
       --query name \
       --output tsv 2>/dev/null)

   if [ -z "$EXISTING_NS" ]; then
       log "공용 Event Hub 네임스페이스 생성 중... (약 2-3분 소요)"
       az eventhubs namespace create \
           --name $EVENT_HUB_NS \
           --resource-group $RESOURCE_GROUP \
           --location $LOCATION \
           --sku Basic
       check_error "Event Hub 네임스페이스 생성 실패"
   else
       log "기존 Event Hub 네임스페이스 사용"
   fi

   # Event Hub가 존재하는지 확인
   EXISTING_HUB=$(az eventhubs eventhub show \
       --name $EVENT_HUB_NAME \
       --namespace-name $EVENT_HUB_NS \
       --resource-group $RESOURCE_GROUP \
       --query name \
       --output tsv 2>/dev/null)

   if [ -z "$EXISTING_HUB" ]; then
       log "Event Hub 생성 중... (약 1-2분 소요)"
       az eventhubs eventhub create \
           --name $EVENT_HUB_NAME \
           --namespace-name $EVENT_HUB_NS \
           --resource-group $RESOURCE_GROUP \
           --partition-count 1 \
           --cleanup-policy Delete \
           --retention-time 24
       check_error "Event Hub 생성 실패"
   else
       log "기존 Event Hub 사용"
   fi

   log "Event Hub 연결 문자열 가져오는 중..."
   # 연결 문자열 가져오기
   CONNECTION_STRING=$(az eventhubs namespace authorization-rule keys list \
       --resource-group $RESOURCE_GROUP \
       --namespace-name $EVENT_HUB_NS \
       --name RootManageSharedAccessKey \
       --query primaryConnectionString -o tsv)
   check_error "Event Hub 연결 문자열 가져오기 실패"

   # Secret으로 저장
   log "Event Hub 연결 정보를 Secret으로 저장 중..."
   kubectl create secret generic eventhub-secret \
       --namespace $ACL_NS \
       --from-literal=connection-string="$CONNECTION_STRING" \
       2>/dev/null || true
   check_error "Event Hub Secret 저장 실패"

   log "Event Hub 설정 완료"
}

# PostgreSQL 설정
setup_postgresql() {
    log "PostgreSQL 데이터베이스 설정 중..."

    # PostgreSQL 초기화 스크립트용 ConfigMap 생성
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-init-script
  namespace: $ACL_NS
data:
  init.sql: |
    CREATE DATABASE legacydb;
EOF
    check_error "PostgreSQL 초기화 스크립트 ConfigMap 생성 실패"

    # PostgreSQL StatefulSet 배포
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: $NAME-postgres
  namespace: $ACL_NS
spec:
  serviceName: "$NAME-postgres"
  replicas: 1
  selector:
    matchLabels:
      app: postgres
      userid: $USERID
  template:
    metadata:
      labels:
        app: postgres
        userid: $USERID
    spec:
      containers:
      - name: postgres
        image: postgres:15
        env:
        - name: POSTGRES_USER
          value: $POSTGRES_USER
        - name: POSTGRES_PASSWORD
          value: $POSTGRES_PASSWORD
        - name: POSTGRES_DB
          value: $POSTGRES_DB
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-data
          mountPath: /var/lib/postgresql/data
          subPath: postgres
        - name: init-script
          mountPath: /docker-entrypoint-initdb.d
      volumes:
      - name: init-script
        configMap:
          name: postgres-init-script
  volumeClaimTemplates:
  - metadata:
      name: postgres-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 10Gi
---
apiVersion: v1
kind: Service
metadata:
  name: $NAME-postgres
  namespace: $ACL_NS
spec:
  ports:
  - port: 5432
    targetPort: 5432
  selector:
    app: postgres
    userid: $USERID
EOF
    check_error "PostgreSQL 배포 실패"

    # PostgreSQL Pod가 Ready 상태가 될 때까지 대기
    log "PostgreSQL 준비 상태 대기 중..."
    kubectl wait --for=condition=ready pod -l "app=postgres,userid=$USERID" -n $ACL_NS --timeout=120s
    check_error "PostgreSQL Pod Ready 상태 대기 실패"

    # 데이터베이스 생성 확인
    POD_NAME=$(kubectl get pod -l "app=postgres,userid=$USERID" -n $ACL_NS -o jsonpath='{.items[0].metadata.name}')

    for i in {1..10}; do
        if kubectl exec $POD_NAME -n $ACL_NS -- psql -U postgres -lqt | cut -d \| -f 1 | grep -qw legacydb; then
            log "데이터베이스 'legacydb' 생성 확인 완료"
            break
        fi
        if [ $i -eq 10 ]; then
            log "Error: 데이터베이스 생성 실패"
            exit 1
        fi
        log "데이터베이스 생성 확인 중... (${i}/10)"
        sleep 5
    done
}

# 애플리케이션 배포
deploy_applications() {
    log "애플리케이션 배포 중..."

    # ConfigMap 생성
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: $NAME-config
  namespace: $ACL_NS
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SERVER_PORT: "8082"
  NOTIFICATION_HOST: "${NAME}-notification"
  NOTIFICATION_PORT: "8083"
EOF

    # ACL Service 배포
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $NAME-service
  namespace: $ACL_NS
spec:
  replicas: 1
  selector:
    matchLabels:
      app: acl-service
      userid: $USERID
  template:
    metadata:
      labels:
        app: acl-service
        userid: $USERID
    spec:
      containers:
      - name: acl-service
        image: ${ACR_NAME}.azurecr.io/telecom/acl-service:$IMAGE_TAG
        imagePullPolicy: Always
        ports:
        - containerPort: 8082
        envFrom:
        - configMapRef:
            name: $NAME-config
        env:
        - name: EVENT_HUB_CONNECTION_STRING
          valueFrom:
            secretKeyRef:
              name: eventhub-secret
              key: connection-string
        - name: EVENT_HUB_NAME
          value: $EVENT_HUB_NAME
        - name: STORAGE_CONNECTION_STRING
          valueFrom:
            secretKeyRef:
              name: storage-secret
              key: connection-string
        - name: STORAGE_CONTAINER_NAME
          value: "eventhub-checkpoints"
---
apiVersion: v1
kind: Service
metadata:
 name: $NAME-service
 namespace: $ACL_NS
spec:
 ports:
 - port: 8082
   targetPort: 8082
 selector:
   app: acl-service
   userid: $USERID
 type: ClusterIP
EOF

   # Notification Mock 배포
   cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
 name: $NAME-notification
 namespace: $ACL_NS
spec:
 replicas: 1
 selector:
   matchLabels:
     app: notification-mock
     userid: $USERID
 template:
   metadata:
     labels:
       app: notification-mock
       userid: $USERID
   spec:
     containers:
     - name: notification-mock
       image: ${ACR_NAME}.azurecr.io/telecom/notification-mock:$IMAGE_TAG
       imagePullPolicy: Always
       ports:
       - containerPort: 8083
---
apiVersion: v1
kind: Service
metadata:
 name: $NAME-notification
 namespace: $ACL_NS
spec:
 ports:
 - port: 8083
   targetPort: 8083
 selector:
   app: notification-mock
   userid: $USERID
 type: LoadBalancer
EOF

   # KOS Mock 배포
   cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
 name: $NAME-kos
 namespace: $ACL_NS
spec:
 replicas: 1
 selector:
   matchLabels:
     app: kos-mock
     userid: $USERID
 template:
   metadata:
     labels:
       app: kos-mock
       userid: $USERID
   spec:
     containers:
     - name: kos-mock
       image: ${ACR_NAME}.azurecr.io/telecom/kos-mock:$IMAGE_TAG
       imagePullPolicy: Always
       ports:
       - containerPort: 8081
       env:
       - name: EVENT_HUB_CONNECTION_STRING
         valueFrom:
           secretKeyRef:
             name: eventhub-secret
             key: connection-string
       - name: EVENT_HUB_NAME
         value: $EVENT_HUB_NAME
---
apiVersion: v1
kind: Service
metadata:
 name: $NAME-kos
 namespace: $ACL_NS
spec:
 ports:
 - port: 8081
   targetPort: 8081
 selector:
   app: kos-mock
   userid: $USERID
 type: ClusterIP
EOF

   # Sync Mock 배포
# Sync Mock 배포
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $NAME-sync
  namespace: $ACL_NS
spec:
  replicas: 1
  selector:
    matchLabels:
      app: sync-mock
      userid: $USERID
  template:
    metadata:
      labels:
        app: sync-mock
        userid: $USERID
    spec:
      containers:
      - name: sync-mock
        image: ${ACR_NAME}.azurecr.io/telecom/sync-mock:$IMAGE_TAG
        imagePullPolicy: Always
        env:
        - name: POSTGRES_HOST
          value: $POSTGRES_HOST
        - name: POSTGRES_PORT
          value: "5432"
        - name: POSTGRES_DB
          value: $POSTGRES_DB
        - name: POSTGRES_USER
          value: $POSTGRES_USER
        - name: POSTGRES_PASSWORD
          value: $POSTGRES_PASSWORD
        - name: MONGODB_HOST
          value: "${MONGODB_HOST}"
        - name: MONGODB_PORT
          value: "${MONGODB_PORT}"
        - name: MONGODB_DATABASE
          value: "${MONGODB_DATABASE}"
        - name: MONGODB_USER
          value: "${MONGODB_USER}"
        - name: MONGODB_PASSWORD
          value: "${MONGODB_PASSWORD}"
---
apiVersion: v1
kind: Service
metadata:
  name: $NAME-sync
  namespace: $ACL_NS
spec:
  ports:
  - port: 8085
    targetPort: 8085
  selector:
    app: sync-mock
    userid: $USERID
  type: ClusterIP
EOF

   # Usage Generator 배포
   cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
 name: $NAME-usage
 namespace: $ACL_NS
spec:
 replicas: 1
 selector:
   matchLabels:
     app: usage-generator
     userid: $USERID
 template:
   metadata:
     labels:
       app: usage-generator
       userid: $USERID
   spec:
     containers:
     - name: usage-generator
       image: ${ACR_NAME}.azurecr.io/telecom/usage-generator:$IMAGE_TAG
       imagePullPolicy: Always
       env:
       - name: POSTGRES_HOST
         value: $POSTGRES_HOST
       - name: POSTGRES_PORT
         value: "5432"
       - name: POSTGRES_DB
         value: $POSTGRES_DB
       - name: POSTGRES_USER
         value: $POSTGRES_USER
       - name: POSTGRES_PASSWORD
         value: $POSTGRES_PASSWORD
---
apiVersion: v1
kind: Service
metadata:
 name: $NAME-usage
 namespace: $ACL_NS
spec:
 ports:
 - port: 8084
   targetPort: 8084
 selector:
   app: usage-generator
   userid: $USERID
 type: ClusterIP
EOF

   # LoadBalancer IP 대기
   log "LoadBalancer IP 대기 중..."
   for i in {1..30}; do
       ACL_IP=$(kubectl get svc $NAME-service -n $ACL_NS -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
       NOTIFICATION_IP=$(kubectl get svc $NAME-notification -n $ACL_NS -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)

       if [ ! -z "$ACL_IP" ] && [ ! -z "$NOTIFICATION_IP" ]; then
           break
       fi
       log "LoadBalancer IP 대기 중... (${i}/30)"
       sleep 10
   done

   if [ -z "$ACL_IP" ] || [ -z "$NOTIFICATION_IP" ]; then
       log "Error: LoadBalancer IP를 얻는데 실패했습니다."
       exit 1
   fi
}

# 리소스 준비 상태 대기
wait_for_resources() {
   log "리소스 준비 상태 대기 중..."

   kubectl wait --for=condition=ready pod -l "userid=$USERID" -n $ACL_NS --timeout=300s
   check_error "리소스 준비 대기 실패"
}

# 결과 출력
print_completion_message() {
   log "=== 배포 완료 ==="

   # Service URL 출력
   ACL_IP=$(kubectl get svc $NAME-service -n $ACL_NS -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

   log "ACL Service Swagger UI: http://$ACL_IP:8082/swagger-ui.html"
}

# 메인 실행 함수
main() {
   log "Strangler Fig & ACL 패턴 실습환경 구성을 시작합니다..."

   # 사전 체크 & 환경 설정
   if [ $# -ne 1 ]; then
       print_usage
       exit 1
   fi

   if [[ ! $1 =~ ^[a-z0-9]+$ ]]; then
       echo "Error: userid는 영문 소문자와 숫자만 사용할 수 있습니다."
       exit 1
   fi

   setup_environment "$1"
   get_aks_credentials
   setup_acr_permission
   create_namespaces

   # 애플리케이션 빌드 및 배포
   build_applications
   push_images

   # 기존 리소스 정리
   cleanup_resources

   # 인프라 및 서비스 배포
   setup_storage
   setup_event_hub
   setup_postgresql
   deploy_applications
   wait_for_resources
   print_completion_message
}

# 스크립트 시작
main "$@"
