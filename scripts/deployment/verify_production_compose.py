import json
from pathlib import Path
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[2]
COMPOSE_FILE = ROOT / "deploy" / "compose.production.yml"
ENV_FILE = ROOT / "deploy" / ".env.example"
CADDYFILE = ROOT / "deploy" / "Caddyfile"
PROMETHEUS_CONFIG = ROOT / "deploy" / "observability" / "prometheus.yml"
ALERT_RULES = ROOT / "deploy" / "observability" / "alerts.yml"
GRAFANA_DASHBOARD = (
    ROOT / "deploy" / "observability" / "grafana" / "dashboards" / "operations.json"
)


def validate(model: dict, caddyfile: str) -> list[str]:
    errors: list[str] = []
    services = model.get("services", {})
    expected = {
        "postgres",
        "kafka",
        "backend",
        "frontend",
        "gateway",
        "prometheus",
        "grafana",
    }
    if set(services) != expected:
        errors.append("production compose must define the seven expected services")

    publicly_published = {
        name
        for name, service in services.items()
        if any(
            port.get("host_ip") not in {"127.0.0.1", "::1"}
            for port in service.get("ports", [])
        )
    }
    if publicly_published != {"gateway"}:
        errors.append("only gateway may publish host ports")

    grafana_ports = services.get("grafana", {}).get("ports", [])
    if not grafana_ports or any(port.get("host_ip") != "127.0.0.1" for port in grafana_ports):
        errors.append("Grafana must bind only to the server loopback interface")

    networks = model.get("networks", {})
    data_network = next(
        (network for name, network in networks.items() if name.endswith("_data")),
        networks.get("data", {}),
    )
    if not data_network.get("internal"):
        errors.append("data network must block external access")

    monitoring_network = next(
        (network for name, network in networks.items() if name.endswith("_monitoring")),
        networks.get("monitoring", {}),
    )
    if not monitoring_network.get("internal"):
        errors.append("monitoring network must block external access")

    backend_environment = services.get("backend", {}).get("environment", {})
    if backend_environment.get("SPRING_PROFILES_ACTIVE") != "prod":
        errors.append("backend must use the prod Spring profile")

    frontend_environment = services.get("frontend", {}).get("environment", {})
    if str(frontend_environment.get("SESSION_COOKIE_SECURE")).lower() != "true":
        errors.append("frontend session cookies must be secure")

    grafana_environment = services.get("grafana", {}).get("environment", {})
    if str(grafana_environment.get("GF_AUTH_ANONYMOUS_ENABLED")).lower() != "false":
        errors.append("Grafana anonymous access must be disabled")

    unbounded_logs = {
        name
        for name, service in services.items()
        if service.get("logging", {}).get("options", {}).get("max-size") != "10m"
        or service.get("logging", {}).get("options", {}).get("max-file") != "5"
    }
    if unbounded_logs:
        errors.append("all services must use bounded container log rotation")

    if "{$DOMAIN}" not in caddyfile or "reverse_proxy frontend:3000" not in caddyfile:
        errors.append("gateway must terminate the configured domain and proxy frontend")
    return errors


def validate_observability(
        prometheus_config: str,
        alert_rules: str,
        dashboard: dict,
) -> list[str]:
    errors: list[str] = []
    if "backend:9090" not in prometheus_config or "/actuator/prometheus" not in prometheus_config:
        errors.append("Prometheus must scrape the backend management endpoint")

    expected_alerts = {
        "SpendingGuardBackendDown",
        "SpendingGuardHighServerErrorRate",
        "SpendingGuardOutboxPublishFailure",
        "SpendingGuardAnalysisConsumeFailure",
        "SpendingGuardJvmHeapPressure",
    }
    missing_alerts = {alert for alert in expected_alerts if f"alert: {alert}" not in alert_rules}
    if missing_alerts:
        errors.append("all core operational alerts must be configured")

    if dashboard.get("uid") != "spending-guard-operations":
        errors.append("Grafana dashboard must use the stable operations uid")
    if len(dashboard.get("panels", [])) < 6:
        errors.append("Grafana dashboard must show the six core operational signals")
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
    errors.extend(validate_observability(
        PROMETHEUS_CONFIG.read_text(encoding="utf-8"),
        ALERT_RULES.read_text(encoding="utf-8"),
        json.loads(GRAFANA_DASHBOARD.read_text(encoding="utf-8")),
    ))
    if errors:
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("production compose contract passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
