#!/bin/sh
# Los nodos de un clúster deben compartir la misma Erlang cookie.
# Se escribe desde la variable RABBITMQ_COOKIE para no versionar el secreto.
set -e
echo "$RABBITMQ_COOKIE" > /var/lib/rabbitmq/.erlang.cookie
chown rabbitmq:rabbitmq /var/lib/rabbitmq/.erlang.cookie
chmod 400 /var/lib/rabbitmq/.erlang.cookie
exec docker-entrypoint.sh rabbitmq-server
