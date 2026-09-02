from pathlib import Path
import sys
import unittest


SCRIPT_DIRECTORY = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPT_DIRECTORY))

from verify_production_compose import validate  # noqa: E402


def valid_model() -> dict:
    return {
        "services": {
            "postgres": {"networks": {"project_data": None}},
            "kafka": {"networks": {"project_data": None}},
            "backend": {
                "environment": {"SPRING_PROFILES_ACTIVE": "prod"},
                "networks": {"project_app": None, "project_data": None},
            },
            "frontend": {
                "environment": {"SESSION_COOKIE_SECURE": "true"},
                "networks": {"project_app": None},
            },
            "gateway": {"ports": [{"published": "443"}]},
        },
        "networks": {"project_app": {}, "project_data": {"internal": True}},
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


if __name__ == "__main__":
    unittest.main()
