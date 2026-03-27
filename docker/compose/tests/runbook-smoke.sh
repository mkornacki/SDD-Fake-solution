#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$ROOT_DIR"

wait_for_readiness() {
	local attempts=0

	until curl -sf http://localhost:8081/actuator/health/readiness | grep '"status":"UP"' >/dev/null; do
		attempts=$((attempts + 1))
		if [ "$attempts" -ge 60 ]; then
			echo "readiness endpoint did not report UP within 120 seconds" >&2
			return 1
		fi
		sleep 2
	done
}

./scripts/dev/start-env.sh
wait_for_readiness
./scripts/dev/stop-env.sh
./scripts/dev/reset-env.sh
wait_for_readiness
