import importlib.util
import pathlib
import unittest


SCRIPT = pathlib.Path(__file__).parents[1] / "full_stack_smoke.py"
SPEC = importlib.util.spec_from_file_location("full_stack_smoke", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class FullStackSmokeTest(unittest.TestCase):
    def test_extracts_six_digit_verification_code(self):
        self.assertEqual(
            "381204",
            MODULE.find_verification_code("Spending Guard 인증번호는 381204 입니다."),
        )

    def test_rejects_mail_without_verification_code(self):
        with self.assertRaises(MODULE.SmokeFailure):
            MODULE.find_verification_code("인증번호가 아직 발급되지 않았습니다."),

    def test_waits_until_dashboard_reflects_the_event(self):
        client = SequenceClient([
            {"transactionCount": 0, "totalSpending": 0},
            {"transactionCount": 1, "totalSpending": 12800},
        ])

        dashboard = MODULE.wait_for_dashboard(
            client, "2026-09", minimum_count=1, minimum_spending=12800,
            attempts=2, interval=0,
        )

        self.assertEqual(1, dashboard["transactionCount"])
        self.assertEqual(2, client.requests)

    def test_fails_with_last_dashboard_after_retry_limit(self):
        client = SequenceClient([{"transactionCount": 0, "totalSpending": 0}])

        with self.assertRaisesRegex(MODULE.SmokeFailure, "transactionCount.*0"):
            MODULE.wait_for_dashboard(
                client, "2026-09", minimum_count=1, minimum_spending=12800,
                attempts=2, interval=0,
            )


class SequenceClient:
    def __init__(self, responses):
        self.responses = responses
        self.requests = 0

    def request(self, _path):
        response = self.responses[min(self.requests, len(self.responses) - 1)]
        self.requests += 1
        return response


if __name__ == "__main__":
    unittest.main()
