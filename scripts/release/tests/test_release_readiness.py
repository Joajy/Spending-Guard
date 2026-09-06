from pathlib import Path
import sys
import tempfile
import unittest


SCRIPT_DIRECTORY = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPT_DIRECTORY))

from verify_release_readiness import ROOT, validate  # noqa: E402


class ReleaseReadinessTest(unittest.TestCase):

    def test_repository_versions_and_release_artifacts_are_consistent(self):
        self.assertEqual([], validate(ROOT, "v0.1.0"))

    def test_rejects_tag_that_does_not_match_version(self):
        errors = validate(ROOT, "v0.2.0")

        self.assertIn("release tag must be v0.1.0", errors)

    def test_rejects_component_version_drift(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.copy_release_contract(root)
            package = root / "frontend" / "package.json"
            package.write_text(
                package.read_text(encoding="utf-8").replace('"version": "0.1.0"', '"version": "0.2.0"'),
                encoding="utf-8",
            )

            errors = validate(root)

        self.assertIn(
            "frontend version must match VERSION: expected 0.1.0, found 0.2.0",
            errors,
        )

    def copy_release_contract(self, target: Path):
        files = [
            "VERSION",
            "CHANGELOG.md",
            "backend/build.gradle",
            "backend/Dockerfile",
            "frontend/package.json",
            "frontend/package-lock.json",
            "frontend/Dockerfile",
            "deploy/.env.example",
            "deploy/compose.production.yml",
            "docs/deployment/database-backup-recovery.md",
            "docs/deployment/operational-observability.md",
        ]
        for relative_path in files:
            source = ROOT / relative_path
            destination = target / relative_path
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(source.read_bytes())


if __name__ == "__main__":
    unittest.main()
