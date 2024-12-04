#!/bin/bash

# ===========================================
# Strangler Fig & ACL 패턴 실습환경 정리 스크립트
# ===========================================

# 사용법 출력
print_usage() {
    cat << EOF
사용법:
    $0 <userid>

설명:
    Strangler Fig & ACL 패턴 실습을 위해 생성한 Azure 리소스를 삭제합니다.
    Event Hub와 Event Hub namespace는 보존됩니다.

예제:
    $0 gappa     # gappa-acl로 시작하는 모든 리소스 삭제
EOF
}

# 유틸리티 함수
log() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $1"
}

check_error() {
    if [ $? -ne 0 ]; then
        log "Error: $1"
        exit 1
    fi
}

# userid 파라미터 체크
if [ $# -ne 1 ]; then
    print_usage
    exit 1
fi

# userid 유효성 검사
if [[ ! $1 =~ ^[a-z0-9]+$ ]]; then
    echo "Error: userid는 영문 소문자와 숫자만 사용할 수 있습니다."
    exit 1
fi

# 환경 변수 설정
USERID=$1
NAME="${USERID}-acl"
RESOURCE_GROUP="tiu-dgga-rg"
AKS_NAME="${USERID}-aks"
ACL_NS="${USERID}-acl"

# 리소스 삭제 전 확인
confirm() {
    read -p "모든 리소스를 삭제하시겠습니까? (y/N) " response
    case "$response" in
        [yY][eE][sS]|[yY])
            return 0
            ;;
        *)
            echo "작업을 취소합니다."
            exit 1
            ;;
    esac
}

# Kubernetes 리소스 정리
cleanup_kubernetes_resources() {
    log "Kubernetes 리소스 삭제 중..."

    # AKS 자격 증명 가져오기
    az aks get-credentials \
        --resource-group $RESOURCE_GROUP \
        --name $AKS_NAME \
        --overwrite-existing

    # Deployments 삭제
    kubectl delete deployment $NAME-service -n $ACL_NS 2>/dev/null || true
    kubectl delete deployment $NAME-kos -n $ACL_NS 2>/dev/null || true
    kubectl delete deployment $NAME-notification -n $ACL_NS 2>/dev/null || true
    kubectl delete deployment $NAME-sync -n $ACL_NS 2>/dev/null || true
    kubectl delete deployment $NAME-usage -n $ACL_NS 2>/dev/null || true

    # Services 삭제
    kubectl delete service $NAME-service -n $ACL_NS 2>/dev/null || true
    kubectl delete service $NAME-kos -n $ACL_NS 2>/dev/null || true
    kubectl delete service $NAME-notification -n $ACL_NS 2>/dev/null || true
    kubectl delete service $NAME-sync -n $ACL_NS 2>/dev/null || true
    kubectl delete service $NAME-usage -n $ACL_NS 2>/dev/null || true

    # StatefulSet & PVC 삭제
    kubectl delete statefulset $NAME-postgres -n $ACL_NS 2>/dev/null || true
    kubectl delete pvc -l "app=postgres,userid=$USERID" -n $ACL_NS 2>/dev/null || true

    # Secrets & ConfigMaps 삭제
    kubectl delete secret eventhub-secret -n $ACL_NS 2>/dev/null || true
    kubectl delete secret storage-secret -n $ACL_NS 2>/dev/null || true
    kubectl delete configmap $NAME-config -n $ACL_NS 2>/dev/null || true
    kubectl delete configmap postgres-init-script -n $ACL_NS 2>/dev/null || true

    # Namespace 삭제
    kubectl delete namespace $ACL_NS 2>/dev/null || true

    log "Kubernetes 리소스 삭제 완료"
}

# 컨테이너 이미지 정리
cleanup_container_images() {
    log "컨테이너 이미지 정리 중..."

    # ACR에서 이미지 삭제
    for service in acl-service kos-mock notification-mock sync-mock usage-generator; do
        az acr repository delete \
            --name ${USERID}cr \
            --image telecom/$service:v1 \
            --yes 2>/dev/null || true
    done

    log "컨테이너 이미지 정리 완료"
}

# 리소스 삭제 대기
wait_for_deletion() {
    log "리소스 삭제 대기 중..."

    # Namespace 삭제 완료 대기
    for i in {1..30}; do
        if ! kubectl get namespace $ACL_NS > /dev/null 2>&1; then
            break
        fi
        log "Namespace 삭제 대기 중... (${i}/30)"
        sleep 10
    done
}

# 메인 실행 함수
main() {
    log "리소스 정리를 시작합니다..."

    # 사전 체크
    confirm

    # 리소스 삭제
    cleanup_kubernetes_resources
    cleanup_container_images
    wait_for_deletion

    log "모든 리소스가 정리되었습니다."
}

# 스크립트 시작
main