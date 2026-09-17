from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[3]
WORKFLOW = ROOT / ".github" / "workflows" / "container-images.yml"
GIT_ATTRIBUTES = ROOT / ".gitattributes"


class ContainerWorkflowContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")

    def test_pull_requests_build_without_registry_write_access(self):
        self.assertIn("pull_request:", self.workflow)
        self.assertIn("push: false", self.workflow)
        self.assertEqual(1, self.workflow.count("packages: write"))
        self.assertLess(
            self.workflow.index("publish:"),
            self.workflow.index("packages: write"),
        )

    def test_release_requires_semver_tag_from_main(self):
        self.assertIn('"v*.*.*"', self.workflow)
        self.assertIn("^v[0-9]+\\.[0-9]+\\.[0-9]+$", self.workflow)
        self.assertIn("git merge-base --is-ancestor", self.workflow)
        self.assertIn("type=semver,pattern={{version}}", self.workflow)
        self.assertIn(
            'verify_release_readiness.py --tag "${GITHUB_REF_NAME}"',
            self.workflow,
        )

    def test_published_images_include_supply_chain_metadata(self):
        self.assertIn("sbom: true", self.workflow)
        self.assertIn("provenance: mode=max", self.workflow)
        self.assertIn("ghcr.io/joajy/spending-guard-backend", self.workflow)
        self.assertIn("ghcr.io/joajy/spending-guard-frontend", self.workflow)

    def test_external_actions_are_pinned_to_immutable_commits(self):
        action_references = re.findall(r"uses: ([^\s]+)", self.workflow)

        self.assertGreater(len(action_references), 0)
        for reference in action_references:
            with self.subTest(reference=reference):
                self.assertRegex(reference, r"@[0-9a-f]{40}$")

    def test_runtime_images_do_not_run_as_root(self):
        backend = (ROOT / "backend" / "Dockerfile").read_text(encoding="utf-8")
        frontend = (ROOT / "frontend" / "Dockerfile").read_text(encoding="utf-8")

        self.assertIn("USER spendingguard", backend)
        self.assertIn("USER nextjs", frontend)

    def test_gradle_wrapper_uses_unix_line_endings_in_container_context(self):
        attributes = GIT_ATTRIBUTES.read_text(encoding="utf-8")

        self.assertRegex(attributes, r"(?m)^backend/gradlew text eol=lf$")
        self.assertIn('- ".gitattributes"', self.workflow)


if __name__ == "__main__":
    unittest.main()
