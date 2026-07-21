#!/usr/bin/env bash
# Contract pipeline: running backend → openapi.yaml → typed clients (TS + Dart).
#
# Prereq: backend up on :8090 (docker compose up -d db && cd backend && ./mvnw spring-boot:run)
# Usage:  ./scripts/sync-api-contract.sh
set -euo pipefail
cd "$(dirname "$0")/.."

echo "→ Exporting OpenAPI from springdoc..."
curl -sf http://localhost:8090/v3/api-docs.yaml -o shared/api-contract/openapi.yaml
echo "  ✓ shared/api-contract/openapi.yaml"

echo "→ Generating Angular client (typescript-angular)..."
npx -y @openapitools/openapi-generator-cli generate \
  -i shared/api-contract/openapi.yaml \
  -g typescript-angular \
  -o web/src/app/core/api/generated \
  --additional-properties=providedInRoot=true,withInterfaces=true \
  >/dev/null
echo "  ✓ web/src/app/core/api/generated"

echo "→ Generating Dart client (dio)..."
npx -y @openapitools/openapi-generator-cli generate \
  -i shared/api-contract/openapi.yaml \
  -g dart-dio \
  -o app/packages/api_client \
  --additional-properties=pubName=controlleads_api \
  >/dev/null
echo "  ✓ app/packages/api_client (run: cd app/packages/api_client && dart run build_runner build)"

echo "Done. Commit shared/api-contract/openapi.yaml with your PR — CI diffs it to catch breaking changes."
