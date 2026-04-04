#!/bin/bash
# .claude/hooks/proteger.sh
# Hook de seguridad para yolo mode — bloquea comandos destructivos

COMMAND=$(echo "$TOOL_INPUT" | jq -r '.command')

# Bloquear rm -rf
if echo "$COMMAND" | grep -q 'rm -rf'; then
  echo "Comando peligroso bloqueado: $COMMAND" >&2
  exit 2
fi

# Bloquear DROP TABLE
if echo "$COMMAND" | grep -qi 'drop table'; then
  echo "Operacion de base de datos bloqueada" >&2
  exit 2
fi

# Bloquear force push
if echo "$COMMAND" | grep -q 'push.*--force\|push.*-f'; then
  echo "Force push bloqueado: $COMMAND" >&2
  exit 2
fi

# Bloquear git reset --hard
if echo "$COMMAND" | grep -q 'reset --hard'; then
  echo "Git reset --hard bloqueado: $COMMAND" >&2
  exit 2
fi

exit 0
