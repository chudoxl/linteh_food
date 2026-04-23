#!/usr/bin/env bash
# Локальная релизная сборка с интерактивным вводом пароля от keystore.
#
# Использование:
#   ./scripts/release.sh                           # по умолчанию: bundleRelease assembleRelease
#   ./scripts/release.sh assembleRelease           # своя цель
#   ./scripts/release.sh clean bundleRelease       # несколько целей и флагов
#
# Пароль вводится интерактивно (без echo), экспортируется в env только для
# дочернего процесса Gradle и снимается в trap'е на выходе.
# keystore.properties должен содержать KEYSTORE_PATH и KEY_ALIAS.

set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f keystore.properties ]]; then
    echo "error: keystore.properties не найден в корне проекта." >&2
    echo "       Скопируй keystore.properties.example и заполни KEYSTORE_PATH + KEY_ALIAS." >&2
    exit 1
fi

trap 'unset KEYSTORE_PASSWORD KEY_PASSWORD' EXIT

read -rs -p "Keystore password: " KEYSTORE_PASSWORD
echo
if [[ -z "$KEYSTORE_PASSWORD" ]]; then
    echo "error: пустой пароль." >&2
    exit 1
fi

read -rs -p "Key password: " KEY_PASSWORD
echo
if [[ -z "$KEY_PASSWORD" ]]; then
    echo "error: пустой пароль ключа." >&2
    exit 1
fi

export KEYSTORE_PASSWORD KEY_PASSWORD

if [[ $# -eq 0 ]]; then
    set -- bundleRelease assembleRelease
fi

exec ./gradlew "$@"
