#!/bin/bash
set -e

echo "=== Hyperliquid Trading Bot Deploy ==="
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo ""
    echo "============================================"
    echo "IMPORTANT: Edit .env file before continuing!"
    echo "============================================"
    echo ""
    echo "Required values to set:"
    echo "  - POSTGRES_PASSWORD (secure password)"
    echo "  - JWT_SECRET (generate with: openssl rand -base64 32)"
    echo ""
    echo "Run this script again after editing .env"
    exit 1
fi

# Stop existing containers
echo "[1/4] Stopping existing containers..."
docker compose down 2>/dev/null || true

# Build and start
echo "[2/4] Building and starting containers..."
docker compose up -d --build

# Wait for services
echo "[3/4] Waiting for services to start (60s)..."
sleep 60

# Health check
echo "[4/4] Checking application health..."
if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
    echo "✓ Application is healthy!"
else
    echo "✗ Health check failed - checking logs..."
    docker compose logs --tail=50 app
    exit 1
fi

# Show status
echo ""
echo "=== Container Status ==="
docker compose ps

echo ""
echo "=== Recent Logs ==="
docker compose logs --tail=10 app

echo ""
echo "=========================================="
echo "       Deployment Complete!"
echo "=========================================="
echo ""
echo "Access Points:"
echo "  Swagger UI: http://localhost:8080/swagger-ui/index.html"
echo "  Health:     http://localhost:8080/actuator/health"
echo "  H2 Console: http://localhost:8080/h2-console"
echo ""
echo "Useful Commands:"
echo "  View logs:  docker compose logs -f app"
echo "  Stop:       docker compose down"
echo "  Restart:    docker compose restart app"
echo ""
