#!/usr/bin/env python3
import json
import os
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from http.cookiejar import CookieJar
from zoneinfo import ZoneInfo


class SmokeFailure(RuntimeError):
    pass


class ApiClient:
    def __init__(self, origin: str):
        self.origin = origin.rstrip("/")
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(CookieJar())
        )

    def request(self, path: str, method: str = "GET", body=None, expected=(200,)):
        payload = None if body is None else json.dumps(body).encode()
        headers = {"Content-Type": "application/json"} if payload else {}
        request = urllib.request.Request(
            f"{self.origin}{path}", data=payload, headers=headers, method=method
        )
        try:
            with self.opener.open(request, timeout=10) as response:
                status = response.status
                content = response.read()
        except urllib.error.HTTPError as error:
            status = error.code
            content = error.read()
        if status not in expected:
            detail = content.decode(errors="replace")
            raise SmokeFailure(f"{method} {path}: expected {expected}, got {status}: {detail}")
        return json.loads(content) if content else None


def find_verification_code(message: str) -> str:
    match = re.search(r"\b(\d{6})\b", message)
    if not match:
        raise SmokeFailure("verification code was not found")
    return match.group(1)


def wait_for_verification_code(mailpit_origin: str, email: str, timeout: int = 40) -> str:
    query = urllib.parse.urlencode({"query": f"to:{email}"})
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(
                f"{mailpit_origin.rstrip('/')}/view/latest.txt?{query}", timeout=5
            ) as response:
                return find_verification_code(response.read().decode(errors="replace"))
        except (urllib.error.URLError, SmokeFailure):
            time.sleep(1)
    raise SmokeFailure("verification email did not arrive before timeout")


def wait_for_analysis(client: ApiClient, event_id: str, timeout: int = 40):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        detail = client.request(f"/api/transactions/{event_id}")
        if detail.get("fastParse") is not None:
            return detail
        time.sleep(1)
    raise SmokeFailure("spend event analysis did not finish before timeout")


def wait_for_dashboard(
    client: ApiClient,
    month: str,
    minimum_count: int,
    minimum_spending: int,
    attempts: int = 40,
    interval: float = 1,
):
    last_dashboard = None
    for attempt in range(attempts):
        last_dashboard = client.request(f"/api/dashboard?month={month}")
        if (
            last_dashboard.get("transactionCount", 0) >= minimum_count
            and last_dashboard.get("totalSpending", 0) >= minimum_spending
        ):
            return last_dashboard
        if attempt + 1 < attempts:
            time.sleep(interval)
    raise SmokeFailure(f"dashboard did not reflect the event: {last_dashboard}")


def run():
    frontend = os.getenv("FRONTEND_ORIGIN", "http://localhost:3000")
    mailpit = os.getenv("MAILPIT_ORIGIN", "http://localhost:8025")
    email = f"full-stack-{int(time.time() * 1000)}@example.com"
    password = "smoke-password-123"
    client = ApiClient(frontend)

    client.request(
        "/api/registration", "POST", {"email": email, "password": password}, (201,)
    )
    client.request("/api/email-verification", "POST", expected=(202,))
    code = wait_for_verification_code(mailpit, email)
    client.request("/api/email-verification", "PUT", {"code": code}, (204,))
    client.request("/api/session", "POST", {"email": email, "password": password})

    accepted = client.request(
        "/api/transactions", "POST", {"message": "쿠팡 12,800원 결제"}, (202,)
    )
    detail = wait_for_analysis(client, accepted["eventId"])
    parsed = detail["fastParse"]
    if parsed.get("status") != "PARSED" or parsed.get("amount") != 12800:
        raise SmokeFailure(f"unexpected analysis result: {parsed}")

    month = datetime.now(ZoneInfo("Asia/Seoul")).strftime("%Y-%m")
    dashboard = wait_for_dashboard(client, month, minimum_count=1, minimum_spending=12800)

    client.request("/api/session", "DELETE", expected=(204,))
    print(json.dumps({
        "status": "passed",
        "eventId": accepted["eventId"],
        "amount": parsed["amount"],
        "category": parsed.get("category"),
        "dashboardTransactionCount": dashboard["transactionCount"],
    }, ensure_ascii=False))


if __name__ == "__main__":
    run()
