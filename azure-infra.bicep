// Documania — Azure Infrastructure (Bicep)
// Usage: az deployment group create --resource-group <rg> --template-file azure-infra.bicep --parameters environment=dev
//
// This provisions the Azure resources used in production:
//   - Container Apps (app + revision)
//   - Azure Blob Storage (ticket attachments)
//   - Container Registry (Docker images)
//   - Log Analytics + App Insights (monitoring)
//
// MySQL is hosted externally on Aiven Cloud (https://console.aiven.io).
// Create a MySQL database service, then set DB_URL / DB_USERNAME / DB_PASSWORD
// as Container App environment variables.

@allowed(['dev', 'staging', 'prod'])
param environment string = 'dev'

param location string = resourceGroup().location
param appName string = 'documania'
param mailFromAddress string = 'noreply@documania.local'

var uniqueSuffix = uniqueString(resourceGroup().id)
var fullName = '${appName}-${environment}-${uniqueSuffix}'
var tags = { project: appName, environment: environment }

// ─────────────────────────────────────────────
// Container Registry
// ─────────────────────────────────────────────
resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: replace('acr-${fullName}', '-', '')
  location: location
  tags: tags
  sku: { name: 'Basic' }
  adminUserEnabled: false
}

// ─────────────────────────────────────────────
// Log Analytics + App Insights
// ─────────────────────────────────────────────
resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: 'logs-${fullName}'
  location: location
  tags: tags
  properties: { sku: { name: 'PerGB2018' } }
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: 'ai-${fullName}'
  location: location
  tags: tags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalytics.id
  }
}

// ─────────────────────────────────────────────
// Container Apps Environment
// ─────────────────────────────────────────────
resource environment_ 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: 'env-${fullName}'
  location: location
  tags: tags
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: { customerId: logAnalytics.properties.customerId }
    }
  }
}

// ─────────────────────────────────────────────
// Azure Blob Storage
// ─────────────────────────────────────────────
resource storage 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: replace('st${fullName}', '-', '')
  location: location
  tags: tags
  kind: 'StorageV2'
  sku: { name: 'Standard_LRS' }
  properties: {
    minimumTlsVersion: 'TLS1_2'
    allowBlobPublicAccess: false
  }
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-05-01' = {
  parent: storage
  name: 'default'
}

resource ticketContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobService
  name: 'ticket-attachments'
  properties: { publicAccess: 'None' }
}

// ─────────────────────────────────────────────
// Container Apps — Backend
// ─────────────────────────────────────────────
resource backend 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'app-${fullName}'
  location: location
  tags: tags
  identity: { type: 'SystemAssigned' }
  properties: {
    environmentId: environment_.id
    configuration: {
      ingress: {
        external: true
        targetPort: 8080
        transport: 'http'
        allowInsecure: false
      }
    }
    template: {
      containers: [
        {
          name: 'backend'
          image: '${acr.properties.loginServer}/${appName}:latest'
          resources: { cpu: 1, memory: '2Gi' }
          env: [
            { name: 'AZURE_STORAGE_BLOB_ENABLED', value: 'true' }
            { name: 'AZURE_STORAGE_BLOB_ENDPOINT', value: storage.properties.primaryEndpoints.blob }
          ]
          probes: [
            { type: 'liveness', httpGet: { path: '/api/health', port: 8080 }, periodSeconds: 30 }
            { type: 'readiness', httpGet: { path: '/api/health', port: 8080 }, periodSeconds: 10 }
          ]
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 5
        rules: [{ http: { metadata: { concurrentRequests: '50' } } }]
      }
    }
  }
}

// Give Container Apps access to Blob Storage (managed identity)
resource blobContributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(fullName, 'blob-contributor')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'ba92f5b4-2d11-453d-a403-e96b0029c9fe') // Storage Blob Data Contributor
    principalId: backend.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

// ─────────────────────────────────────────────
// Outputs
// ─────────────────────────────────────────────
output containerAppName string = backend.name
output containerAppUrl string = 'https://${backend.properties.configuration.ingress.fqdn}'
output acrLoginServer string = acr.properties.loginServer
output storageAccountName string = storage.name
