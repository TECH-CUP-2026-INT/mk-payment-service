#!/bin/bash
set -e

RESOURCE_GROUP="rg-techcup-payments-${ENVIRONMENT}"
LOCATION="eastus2"
AKS_NAME="aks-techcup-payments-${ENVIRONMENT}"
ACR_NAME="acrtechcuppayments${ENVIRONMENT}"
KEY_VAULT_NAME="kv-techcup-payments-${ENVIRONMENT}"
LOG_ANALYTICS_WS="la-techcup-payments-${ENVIRONMENT}"
APP_INSIGHTS="appi-techcup-payments-${ENVIRONMENT}"
MONITOR_WS="amw-techcup-payments-${ENVIRONMENT}"
GRAFANA_NAME="grafana-techcup-payments-${ENVIRONMENT}"

echo "=== Creando Resource Group ==="
az group create --name "${RESOURCE_GROUP}" --location "${LOCATION}"

echo "=== Creando Azure Container Registry ==="
az acr create \
  --resource-group "${RESOURCE_GROUP}" \
  --name "${ACR_NAME}" \
  --sku Standard \
  --admin-enabled false

echo "=== Creando Azure Key Vault ==="
az keyvault create \
  --resource-group "${RESOURCE_GROUP}" \
  --name "${KEY_VAULT_NAME}" \
  --location "${LOCATION}" \
  --enable-rbac-authorization true

echo "=== Creando Log Analytics Workspace ==="
az monitor log-analytics workspace create \
  --resource-group "${RESOURCE_GROUP}" \
  --workspace-name "${LOG_ANALYTICS_WS}" \
  --location "${LOCATION}"

LA_WORKSPACE_ID=$(az monitor log-analytics workspace show \
  --resource-group "${RESOURCE_GROUP}" \
  --workspace-name "${LOG_ANALYTICS_WS}" \
  --query customerId -o tsv)

echo "=== Creando Application Insights ==="
az monitor app-insights component create \
  --resource-group "${RESOURCE_GROUP}" \
  --app "${APP_INSIGHTS}" \
  --location "${LOCATION}" \
  --workspace "${LOG_ANALYTICS_WS}" \
  --kind web \
  --application-type java

APP_INSIGHTS_CS=$(az monitor app-insights component show \
  --resource-group "${RESOURCE_GROUP}" \
  --app "${APP_INSIGHTS}" \
  --query connectionString -o tsv)

echo "=== Creando Azure Monitor Workspace (Prometheus) ==="
az monitor account create \
  --resource-group "${RESOURCE_GROUP}" \
  --name "${MONITOR_WS}" \
  --location "${LOCATION}"

echo "=== Creando AKS Cluster ==="
az aks create \
  --resource-group "${RESOURCE_GROUP}" \
  --name "${AKS_NAME}" \
  --node-count 2 \
  --node-vm-size Standard_D4s_v5 \
  --enable-cluster-autoscaler \
  --min-count 2 \
  --max-count 6 \
  --network-plugin azure \
  --network-policy calico \
  --enable-oidc-issuer \
  --enable-workload-identity \
  --enable-azure-monitor-metrics \
  --azure-monitor-workspace-resource-id "$(az monitor account show --resource-group "${RESOURCE_GROUP}" --name "${MONITOR_WS}" --query id -o tsv)" \
  --enable-msi-auth-for-monitoring \
  --enable-azure-monitor-app-monitoring \
  --workspace-resource-id "$(az monitor log-analytics workspace show --resource-group "${RESOURCE_GROUP}" --workspace-name "${LOG_ANALYTICS_WS}" --query id -o tsv)" \
  --attach-acr "${ACR_NAME}" \
  --generate-ssh-keys

echo "=== Obteniendo credenciales AKS ==="
az aks get-credentials --resource-group "${RESOURCE_GROUP}" --name "${AKS_NAME}" --overwrite-existing

echo "=== Instalando cert-manager ==="
helm repo add jetstack https://charts.jetstack.io --force-update
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --set installCRDs=true

echo "=== Instalando NGINX Ingress Controller ==="
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx --force-update
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-health-probe-request-path"=/healthz

echo "=== Instalando Secrets Store CSI Driver ==="
helm repo add csi-secrets-store-provider-azure https://azure.github.io/secrets-store-csi-driver-provider-azure/charts --force-update
helm upgrade --install csi-secrets-store csi-secrets-store-provider-azure/csi-secrets-store-provider-azure \
  --namespace kube-system \
  --set secrets-store-csi-driver.syncSecret.enabled=true

echo "=== Creando Workload Identity para la app ==="
AKS_OIDC_ISSUER=$(az aks show -n "${AKS_NAME}" -g "${RESOURCE_GROUP}" --query oidcIssuerProfile.issuerUrl -o tsv)

az identity create \
  --resource-group "${RESOURCE_GROUP}" \
  --name "identity-payments-api-${ENVIRONMENT}"

IDENTITY_CLIENT_ID=$(az identity show \
  --resource-group "${RESOURCE_GROUP}" \
  --name "identity-payments-api-${ENVIRONMENT}" \
  --query clientId -o tsv)

IDENTITY_PRINCIPAL_ID=$(az identity show \
  --resource-group "${RESOURCE_GROUP}" \
  --name "identity-payments-api-${ENVIRONMENT}" \
  --query principalId -o tsv)

az identity federated-credential create \
  --name "fed-payments-api-${ENVIRONMENT}" \
  --identity-name "identity-payments-api-${ENVIRONMENT}" \
  --resource-group "${RESOURCE_GROUP}" \
  --issuer "${AKS_OIDC_ISSUER}" \
  --subject "system:serviceaccount:payments:payments-api-sa" \
  --audience "api://AzureADTokenExchange"

echo "=== Asignando permisos Key Vault ==="
az role assignment create \
  --assignee "${IDENTITY_PRINCIPAL_ID}" \
  --role "Key Vault Secrets User" \
  --scope "$(az keyvault show --name "${KEY_VAULT_NAME}" --resource-group "${RESOURCE_GROUP}" --query id -o tsv)"

echo "=== Asignando permisos ACR ==="
az role assignment create \
  --assignee "${IDENTITY_PRINCIPAL_ID}" \
  --role "AcrPull" \
  --scope "$(az acr show --name "${ACR_NAME}" --resource-group "${RESOURCE_GROUP}" --query id -o tsv)"

echo "=== Guardando connection string de App Insights en Key Vault ==="
az keyvault secret set \
  --vault-name "${KEY_VAULT_NAME}" \
  --name "application-insights-connection-string" \
  --value "${APP_INSIGHTS_CS}"

echo ""
echo "=== INFRAESTRUCTURA CREADA (${ENVIRONMENT}) ==="
echo "AKS Cluster:      ${AKS_NAME}"
echo "ACR:              ${ACR_NAME}"
echo "Key Vault:        ${KEY_VAULT_NAME}"
echo "Log Analytics:    ${LOG_ANALYTICS_WS}"
echo "App Insights:     ${APP_INSIGHTS}"
echo "Monitor Workspace: ${MONITOR_WS}"
echo "Workload Identity Client ID: ${IDENTITY_CLIENT_ID}"
echo ""
echo "Configura los secrets de Key Vault y las credenciales de GitHub Actions."
