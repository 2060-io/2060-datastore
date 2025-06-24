# Data Store Helm Chart

## Overview

This Helm chart deploys the Data Store application along with the necessary Kubernetes resources. It includes:

- **Namespace** (optional): Can be created by the chart or managed externally.
- **StatefulSet**: Manages the Data Store pod and App-Check-Proxy.
- **Service**: Exposes Data Store within the cluster.
- **Ingress**: Routes external traffic to Data Store.
- **ConfigMap**: Stores environment variables for App-Check-Proxy.

## Installation

### 1️⃣ Lint the Chart

Ensure the chart is correctly formatted:

```bash
helm lint ./charts
```

### 2️⃣ Render Templates

Preview the generated Kubernetes manifests:

```bash
helm template <release-name> ./charts --namespace <your-namespace>
```

### 3️⃣ Dry-Run Installation

Simulate the installation without making changes to your cluster:

```bash
helm install --dry-run --debug <release-name> ./charts --namespace <your-namespace>
```

### 4️⃣ Install the Chart

```bash
helm install <release-name> ./charts --namespace <your-namespace>
```

**Note:**  

- Replace `<release-name>` with the desired release name.  
- Example:

```bash
helm install data-store-dev ./charts --namespace 2060-core-dev
```

## Configuration

All configurable parameters are located in the `values.yaml` file.

## Uninstalling the Chart

To remove the deployed release:

```bash
helm uninstall <release-name> --namespace <your-namespace>
```

## Support

For additional information, please refer to the [Helm documentation](https://helm.sh/docs/).