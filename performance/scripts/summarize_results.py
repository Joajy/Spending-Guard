import argparse
import csv
import json
import math
from pathlib import Path


def calculate_metrics(samples):
    if not samples:
        raise ValueError("JMeter did not record any samples")

    elapsed = sorted(int(sample["elapsed"]) for sample in samples)
    failed = sum(sample["success"].lower() != "true" for sample in samples)
    started_at = min(int(sample["timeStamp"]) for sample in samples)
    finished_at = max(
        int(sample["timeStamp"]) + int(sample["elapsed"])
        for sample in samples
    )
    duration_seconds = max((finished_at - started_at) / 1000, 0.001)

    def percentile(ratio):
        return elapsed[max(math.ceil(len(elapsed) * ratio) - 1, 0)]

    return {
        "requests": len(samples),
        "failedRequests": failed,
        "errorRatePercent": round(failed / len(samples) * 100, 2),
        "throughputRequestsPerSecond": round(len(samples) / duration_seconds, 2),
        "averageResponseTimeMs": round(sum(elapsed) / len(elapsed), 2),
        "p50ResponseTimeMs": percentile(0.50),
        "p95ResponseTimeMs": percentile(0.95),
        "p99ResponseTimeMs": percentile(0.99),
    }


def meets_thresholds(metrics, max_error_rate, max_p95):
    return (
        metrics["errorRatePercent"] <= max_error_rate
        and metrics["p95ResponseTimeMs"] <= max_p95
    )


def format_summary(metrics, max_error_rate, max_p95):
    succeeded = metrics["requests"] - metrics["failedRequests"]
    return "\n".join([
        "## Transaction ingestion baseline",
        "",
        f"Requests: {succeeded}/{metrics['requests']} succeeded",
        f"Error rate: {metrics['errorRatePercent']:.2f}% "
        f"(required: <= {max_error_rate:.2f}%)",
        f"Throughput: {metrics['throughputRequestsPerSecond']:.2f} requests/s",
        f"Average response time: {metrics['averageResponseTimeMs']:.2f} ms",
        "p50 / p95 / p99: "
        f"{metrics['p50ResponseTimeMs']} / {metrics['p95ResponseTimeMs']} / "
        f"{metrics['p99ResponseTimeMs']} ms (p95 required: <= {max_p95} ms)",
        "Profile: 10 threads, 20 iterations per thread, 10-second ramp-up",
        "Scope: API ingestion transaction only; Kafka publisher and consumer disabled",
    ])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("result", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--github-summary", type=Path)
    parser.add_argument("--max-error-rate", type=float, default=0)
    parser.add_argument("--max-p95", type=int, default=1000)
    args = parser.parse_args()

    with args.result.open(encoding="utf-8") as source:
        metrics = calculate_metrics(list(csv.DictReader(source)))

    metrics["thresholds"] = {
        "errorRatePercent": args.max_error_rate,
        "p95ResponseTimeMs": args.max_p95,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(metrics, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    summary = format_summary(metrics, args.max_error_rate, args.max_p95)
    print(summary)
    if args.github_summary:
        with args.github_summary.open("a", encoding="utf-8") as output:
            output.write(summary.replace("\n", "  \n") + "\n")

    if not meets_thresholds(metrics, args.max_error_rate, args.max_p95):
        raise SystemExit("Performance baseline threshold was not met")


if __name__ == "__main__":
    main()
