import json
from pathlib import Path
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[2]
COMPOSE_FILE = ROOT / "deploy" / "compose.production.yml"
ENV_FILE = ROOT / "deploy" / ".env.example"
CADDYFILE = ROOT / "deploy" / "Caddyfile"


def validate(model: dict, caddyfile: str) -> list[str]:
    errors: list[str] = []
    services = model.get("services", {})
    expected = {"postgres", "kafka", "backend", "frontend", "gateway"}
    if set(services) != expected:
        errors.append("production compose must define the five expected services")

    published = {
        name for name, service in services.items() if service.get("ports")
    }
    if published != {"gateway"}:
        errors.append("only gateway may publish host ports")

    networks = model.get("networks", {})
    data_network = next(
        (network for name, network in networks.items() if name.endswith("_data")),
        networks.get("data", {}),
    )
    if not data_network.get("internal"):
        errors.append("data network must block external access")

    backend_environment = services.get("backend", {}).get("environment", {})
    if backend_environment.get("SPRING_PROFILES_ACTIVE") != "prod":
        errors.append("backend must use the prod Spring profile")

    frontend_environment = services.get("frontend", {}).get("environment", {})
    if str(frontend_environment.get("SESSION_COOKIE_SECURE")).lower() != "true":
        errors.append("frontend session cookies must be secure")

    if "{$DOMAIN}" not in caddyfile or "reverse_proxy frontend:3000" not in caddyfile:
        errors.append("gateway must terminate the configured domain and proxy frontend")
    return errors


def load_compose_model() -> dict:
    command = [
        "docker",
        "compose",
        "--env-file",
        str(ENV_FILE),
        "-f",
        str(COMPOSE_FILE),
        "config",
        "--format",
        "json",
    ]
    result = subprocess.run(command, check=True, capture_output=True, text=True)
    return json.loads(result.stdout)


def main() -> int:
    errors = validate(load_compose_model(), CADDYFILE.read_text(encoding="utf-8"))
    if errors:
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("production compose contract passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
