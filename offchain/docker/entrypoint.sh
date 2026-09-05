#!/bin/sh
# Reads the on-chain contract addresses written by the `contracts-deploy` one-shot service
# (see the root docker-compose.yml) and exports them before starting the app — the
# `TOKEN_FACTORY_ADDRESS` / `IDENTITY_GATEWAY_ADDRESS` / `DISTRIBUTION_REGISTRY_ADDRESS` env vars
# consumed by `rwashift.blockchain.*` (application.yml) can't be known until DeployLocal.s.sol
# actually runs against Anvil, so they can't just be hardcoded compose environment values.
set -e

DEPLOYMENT_FILE="${DEPLOYMENT_FILE:-/deployments/local.json}"

if [ -f "$DEPLOYMENT_FILE" ]; then
  echo "Loading on-chain contract addresses from $DEPLOYMENT_FILE"
  # forge's vm.writeJson pretty-prints with a space after the colon ("key": "value"), not the
  # compact "key":"value" — match either so this keeps working if that ever changes.
  TOKEN_FACTORY_ADDRESS=$(grep -o '"rwaShiftTokenFactory"[[:space:]]*:[[:space:]]*"[^"]*"' "$DEPLOYMENT_FILE" | cut -d'"' -f4)
  IDENTITY_GATEWAY_ADDRESS=$(grep -o '"rwaShiftIdentityGateway"[[:space:]]*:[[:space:]]*"[^"]*"' "$DEPLOYMENT_FILE" | cut -d'"' -f4)
  DISTRIBUTION_REGISTRY_ADDRESS=$(grep -o '"rwaShiftDistributionRegistry"[[:space:]]*:[[:space:]]*"[^"]*"' "$DEPLOYMENT_FILE" | cut -d'"' -f4)
  USDC_TOKEN_ADDRESS=$(grep -o '"usdcTokenAddress"[[:space:]]*:[[:space:]]*"[^"]*"' "$DEPLOYMENT_FILE" | cut -d'"' -f4)
  PAYMENT_COLLECTION_ADDRESS=$(grep -o '"paymentCollectionAddress"[[:space:]]*:[[:space:]]*"[^"]*"' "$DEPLOYMENT_FILE" | cut -d'"' -f4)
  PAYMENT_ROUTER_ADDRESS=$(grep -o '"paymentRouterAddress"[[:space:]]*:[[:space:]]*"[^"]*"' "$DEPLOYMENT_FILE" | cut -d'"' -f4)
  export TOKEN_FACTORY_ADDRESS IDENTITY_GATEWAY_ADDRESS DISTRIBUTION_REGISTRY_ADDRESS USDC_TOKEN_ADDRESS PAYMENT_COLLECTION_ADDRESS PAYMENT_ROUTER_ADDRESS
  echo "TOKEN_FACTORY_ADDRESS=$TOKEN_FACTORY_ADDRESS"
  echo "IDENTITY_GATEWAY_ADDRESS=$IDENTITY_GATEWAY_ADDRESS"
  echo "DISTRIBUTION_REGISTRY_ADDRESS=$DISTRIBUTION_REGISTRY_ADDRESS"
  echo "USDC_TOKEN_ADDRESS=$USDC_TOKEN_ADDRESS"
  echo "PAYMENT_COLLECTION_ADDRESS=$PAYMENT_COLLECTION_ADDRESS"
  echo "PAYMENT_ROUTER_ADDRESS=$PAYMENT_ROUTER_ADDRESS"
else
  echo "WARNING: $DEPLOYMENT_FILE not found — starting without on-chain contract addresses set."
  echo "Tokenization will fail until the contracts-deploy service has run successfully."
fi

exec java -jar /app/app.jar
