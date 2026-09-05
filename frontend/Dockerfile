# syntax=docker/dockerfile:1
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund

FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1

# NEXT_PUBLIC_* variables are inlined into the client bundle at `next build` time — passing them
# as plain container `environment:` in docker-compose.yml (a runtime mechanism) has no effect on
# them, so they must come in as build args instead. See docker-compose.yml at the repo root.
ARG NEXT_PUBLIC_API_BASE_URL
ARG NEXT_PUBLIC_CHAIN
ARG NEXT_PUBLIC_ANVIL_RPC_URL
ARG NEXT_PUBLIC_SEPOLIA_RPC_URL
ARG NEXT_PUBLIC_SEPOLIA_EXPLORER_URL
ARG NEXT_PUBLIC_WALLETCONNECT_PROJECT_ID
ARG NEXT_PUBLIC_OTEL_EXPORTER_OTLP_ENDPOINT
ARG NEXT_PUBLIC_OTEL_SERVICE_NAME
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL} \
    NEXT_PUBLIC_CHAIN=${NEXT_PUBLIC_CHAIN} \
    NEXT_PUBLIC_ANVIL_RPC_URL=${NEXT_PUBLIC_ANVIL_RPC_URL} \
    NEXT_PUBLIC_SEPOLIA_RPC_URL=${NEXT_PUBLIC_SEPOLIA_RPC_URL} \
    NEXT_PUBLIC_SEPOLIA_EXPLORER_URL=${NEXT_PUBLIC_SEPOLIA_EXPLORER_URL} \
    NEXT_PUBLIC_WALLETCONNECT_PROJECT_ID=${NEXT_PUBLIC_WALLETCONNECT_PROJECT_ID} \
    NEXT_PUBLIC_OTEL_EXPORTER_OTLP_ENDPOINT=${NEXT_PUBLIC_OTEL_EXPORTER_OTLP_ENDPOINT} \
    NEXT_PUBLIC_OTEL_SERVICE_NAME=${NEXT_PUBLIC_OTEL_SERVICE_NAME}

RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

CMD ["node", "server.js"]
