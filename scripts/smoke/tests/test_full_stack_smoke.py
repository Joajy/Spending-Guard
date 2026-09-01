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


if __name__ == "__main__":
    unittest.main()
