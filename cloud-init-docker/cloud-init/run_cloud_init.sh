#!/usr/bin/env bash
export CLOUD_INIT_DIR="$(dirname "$0")"
export K8S_CLUSTER_API_URL="${KUBERNETES_SERVICE_HOST}:${KUBERNETES_SERVICE_PORT_HTTPS}"

kubectl config set-cluster ${NAMESPACE} --server=${K8S_CLUSTER_API_URL}
kubectl config set-context ${NAMESPACE} --namespace=${NAMESPACE}
kubectl config use-context ${NAMESPACE}

# Skip version control assertion in cloud-init with this file
touch offline.txt

"${CLOUD_INIT_DIR}"/scripts/cloud-init.sh "${NAMESPACE}" "${APP_NAME}"
