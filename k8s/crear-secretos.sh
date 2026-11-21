#!/usr/bin/env bash
# Crea los secretos del clúster local. Las claves se generan al azar y se muestran UNA vez:
# anótalas. No las subas a ningún repositorio.
set -euo pipefail

azar() { openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 20; }

DB_PASSWORD="${DB_PASSWORD:-$(azar)}"
RABBIT_PASSWORD="${RABBIT_PASSWORD:-$(azar)}"
GRAFANA_PASSWORD="${GRAFANA_PASSWORD:-$(azar)}"
OPERADOR_CLAVE="${OPERADOR_CLAVE:-$(azar)}"
SUPERVISOR_CLAVE="${SUPERVISOR_CLAVE:-$(azar)}"
ADMIN_CLAVE="${ADMIN_CLAVE:-$(azar)}"
# El secreto del JWT es el MISMO para las tres APIs: la de envíos firma y las demás validan.
JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c 48)}"

kubectl -n trackflow delete secret trackflow-secretos --ignore-not-found
kubectl -n trackflow create secret generic trackflow-secretos \
  --from-literal=db-user=postgres \
  --from-literal=db-password="$DB_PASSWORD" \
  --from-literal=rabbit-user=trackflow \
  --from-literal=rabbit-password="$RABBIT_PASSWORD" \
  --from-literal=grafana-password="$GRAFANA_PASSWORD" \
  --from-literal=operador-clave="$OPERADOR_CLAVE" \
  --from-literal=supervisor-clave="$SUPERVISOR_CLAVE" \
  --from-literal=admin-clave="$ADMIN_CLAVE" \
  --from-literal=jwt-secret="$JWT_SECRET"

echo "Secretos creados. Anota estos valores (no se vuelven a mostrar):"
echo "  Usuario operador:    operador   / $OPERADOR_CLAVE"
echo "  Usuario supervisor:  supervisor / $SUPERVISOR_CLAVE"
echo "  Usuario admin:       admin      / $ADMIN_CLAVE"
echo "  Grafana (admin):     admin      / $GRAFANA_PASSWORD"
echo "  RabbitMQ:            trackflow  / $RABBIT_PASSWORD"
