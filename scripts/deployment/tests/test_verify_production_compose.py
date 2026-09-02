from pathlib import Path
import sys
import unittest


SCRIPT_DIRECTORY = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPT_DIRECTORY))

from verify_production_compose import validate, validate_observability  # noqa: E402


def valid_model() -> dict:
    return {
        "services": {
            "backend": {
                "environment": {"SPRING_PROFILES_ACTIVE": "prod"},
                "networks": {
                    "project_app": None,
                    "project_data": None,
                    "project_monitoring": None,
                },
                "logging": {"options": {"max-size": "10m", "max-file": "5"}},
            },
            "frontend": {
                "environment": {"SESSION_COOKIE_SECURE": "true"},
                "networks": {"project_app": None},
                "logging": {"options": {"max-size": "10m", "max-file": "5"}},
            },
            "prometheus": {
                "networks": {"project_monitoring": None},
                "logging": {"options": {"max-size": "10m", "max-file": "5"}},
            },
            "grafana": {
                "environment": {"GF_AUTH_ANONYMOUS_ENABLED": "false"},
                "ports": [{"published": "3001", "host_ip": "127.0.0.1"}],
                "networks": {"project_monitoring": None},
                "logging": {"options": {"max-size": "10m", "max-file": "5"}},
            },
            "gateway": {
                "ports": [{"published": "443", "host_ip": "0.0.0.0"}],
                "logging": {"options": {"max-size": "10m", "max-file": "5"}},
            },
            "postgres": {
                "networks": {"project_data": None},
                "logging": {"options": {"max-size": "10m", "max-file": "5"}},
            },
            "kafka": {
                "networks": {"project_data": None},
                "logging": {"options": {"max-size": "10m", "max-file": "5"}},
            },
        },
        "networks": {
            "project_app": {},
            "project_data": {"internal": True},
            "project_monitoring": {"internal": True},
        },
    }


class ProductionComposeContractTest(unittest.TestCase):
    def test_accepts_private_application_and_data_services(self):
        errors = validate(
            valid_model(),
            "{$DOMAIN} {\n\treverse_proxy frontend:3000\n}",
        )

        self.assertEqual([], errors)

    def test_rejects_public_backend_or_insecure_session(self):
        model = valid_model()
        model["services"]["backend"]["ports"] = [{"published": "8080"}]
        model["services"]["frontend"]["environment"]["SESSION_COOKIE_SECURE"] = "false"

        errors = validate(model, "missing gateway contract")

        self.assertIn("only gateway may publish host ports", errors)
        self.assertIn("frontend session cookies must be secure", errors)
        self.assertIn("gateway must terminate the configured domain and proxy frontend", errors)

    def test_rejects_external_data_network_or_non_prod_backend(self):
        model = valid_model()
        model["networks"]["project_data"]["internal"] = False
        model["services"]["backend"]["environment"]["SPRING_PROFILES_ACTIVE"] = "default"

        errors = validate(model, "{$DOMAIN} reverse_proxy frontend:3000")

        self.assertIn("data network must block external access", errors)
        self.assertIn("backend must use the prod Spring profile", errors)

    def test_rejects_public_grafana_or_unbounded_logs(self):
        model = valid_model()
        model["services"]["grafana"]["ports"][0]["host_ip"] = "0.0.0.0"
        model["services"]["kafka"]["logging"]["options"].pop("max-file")

        errors = validate(model, "{$DOMAIN} reverse_proxy frontend:3000")

        self.assertIn("only gateway may publish host ports", errors)
        self.assertIn("Grafana must bind only to the server loopback interface", errors)
        self.assertIn("all services must use bounded container log rotation", errors)

    def test_requires_scrape_target_alerts_and_dashboard_panels(self):
        errors = validate_observability(
            "metrics_path: /actuator/prometheus\ntargets: [backend:9090]",
            "\n".join([
                "alert: SpendingGuardBackendDown",
                "alert: SpendingGuardHighServerErrorRate",
                "alert: SpendingGuardOutboxPublishFailure",
                "alert: SpendingGuardAnalysisConsumeFailure",
                "alert: SpendingGuardJvmHeapPressure",
            ]),
            {"uid": "spending-guard-operations", "panels": [{}, {}, {}, {}, {}, {}]},
        )

        self.assertEqual([], errors)


if __name__ == "__main__":
    unittest.main()
