import sys
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).parents[1] / "scripts"))

from summarize_results import calculate_metrics, meets_thresholds  # noqa: E402


class SummarizeResultsTest(unittest.TestCase):

    def test_calculates_nearest_rank_percentiles_and_throughput(self):
        samples = [
            {"timeStamp": "0", "elapsed": "100", "success": "true"},
            {"timeStamp": "100", "elapsed": "200", "success": "true"},
            {"timeStamp": "200", "elapsed": "300", "success": "true"},
            {"timeStamp": "300", "elapsed": "400", "success": "false"},
        ]

        metrics = calculate_metrics(samples)

        self.assertEqual(4, metrics["requests"])
        self.assertEqual(1, metrics["failedRequests"])
        self.assertEqual(25.0, metrics["errorRatePercent"])
        self.assertEqual(5.71, metrics["throughputRequestsPerSecond"])
        self.assertEqual(250.0, metrics["averageResponseTimeMs"])
        self.assertEqual(200, metrics["p50ResponseTimeMs"])
        self.assertEqual(400, metrics["p95ResponseTimeMs"])
        self.assertEqual(400, metrics["p99ResponseTimeMs"])

    def test_rejects_empty_results(self):
        with self.assertRaisesRegex(ValueError, "did not record"):
            calculate_metrics([])

    def test_requires_both_error_rate_and_p95_thresholds(self):
        metrics = {"errorRatePercent": 0.0, "p95ResponseTimeMs": 999}

        self.assertTrue(meets_thresholds(metrics, max_error_rate=0, max_p95=1000))

        metrics["errorRatePercent"] = 0.01
        self.assertFalse(meets_thresholds(metrics, max_error_rate=0, max_p95=1000))

        metrics["errorRatePercent"] = 0.0
        metrics["p95ResponseTimeMs"] = 1001
        self.assertFalse(meets_thresholds(metrics, max_error_rate=0, max_p95=1000))


if __name__ == "__main__":
    unittest.main()
