#!/usr/bin/env python3
"""Follow the audit trail of every service at once.

Each service writes JSON-lines to logs/audit-<service>.log. This merges all of
them into one live stream, colour-coded per service, so a single terminal shows
the whole system's audit activity in the order it happened.

Standard library only - no jq, no pip install, nothing to set up.

Examples
--------
    ./scripts/tail-audit.py                        follow everything
    ./scripts/tail-audit.py -n 100                 replay the last 100 entries first
    ./scripts/tail-audit.py --no-follow            print what exists and exit
    ./scripts/tail-audit.py -s account-service     one service only
    ./scripts/tail-audit.py --failures             only refused/failed attempts
    ./scripts/tail-audit.py -a FUNDS_TRANSFERRED   one action
    ./scripts/tail-audit.py --actor teller-42      one actor
    ./scripts/tail-audit.py --raw                  original JSON lines, for piping
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import signal
import sys
import time
from typing import Iterator

DEFAULT_LOG_DIR = os.environ.get("AUDIT_LOG_DIR", "logs")
POLL_SECONDS = 0.25

# Distinct, readable on both light and dark terminals.
PALETTE = ["\033[36m", "\033[35m", "\033[32m", "\033[33m", "\033[34m", "\033[95m", "\033[96m"]
RESET = "\033[0m"
BOLD = "\033[1m"
DIM = "\033[2m"
RED = "\033[31m"
GREEN = "\033[32m"


def supports_colour(force: bool) -> bool:
    if force:
        return True
    return sys.stdout.isatty() and os.environ.get("TERM") != "dumb" and "NO_COLOR" not in os.environ


class Painter:
    def __init__(self, enabled: bool) -> None:
        self.enabled = enabled

    def __call__(self, text: str, *codes: str) -> str:
        if not self.enabled or not codes:
            return text
        return "".join(codes) + text + RESET


class LogFile:
    """One service's audit file, followed across truncation and rotation."""

    def __init__(self, path: str, service: str) -> None:
        self.path = path
        self.service = service
        self._handle = None
        self._inode = None

    def open_at(self, from_start: bool) -> None:
        try:
            self._handle = open(self.path, "r", encoding="utf-8", errors="replace")
            self._inode = os.fstat(self._handle.fileno()).st_ino
            if not from_start:
                self._handle.seek(0, os.SEEK_END)
        except FileNotFoundError:
            self._handle = None

    def _reopen_if_rotated(self) -> None:
        """Logback rolls the file out from under us; follow the new one."""
        try:
            inode_on_disk = os.stat(self.path).st_ino
        except FileNotFoundError:
            return
        if self._handle is None:
            self.open_at(from_start=True)
            return
        if inode_on_disk != self._inode:
            self._handle.close()
            self.open_at(from_start=True)
            return
        # Truncated in place (a redeploy wiping the file) - rewind.
        if os.fstat(self._handle.fileno()).st_size < self._handle.tell():
            self._handle.seek(0)

    def read_new(self) -> Iterator[str]:
        self._reopen_if_rotated()
        if self._handle is None:
            return
        while True:
            line = self._handle.readline()
            if not line:
                return
            line = line.strip()
            if line:
                yield line


def discover(log_dir: str, only_service: str | None) -> list[LogFile]:
    files = []
    for path in sorted(glob.glob(os.path.join(log_dir, "audit-*.log"))):
        service = os.path.basename(path)[len("audit-"):-len(".log")]
        if only_service and service != only_service:
            continue
        files.append(LogFile(path, service))
    return files


def matches(entry: dict, args: argparse.Namespace) -> bool:
    if args.failures and entry.get("outcome") != "FAILURE":
        return False
    if args.action and entry.get("action") != args.action:
        return False
    if args.actor and entry.get("actor") != args.actor:
        return False
    return True


def format_entry(entry: dict, service: str, paint: Painter, colour: str, width: int) -> str:
    stamp = str(entry.get("occurredAt", ""))[11:23] or "-"
    outcome = entry.get("outcome", "?")
    action = entry.get("action", "?")
    resource_type = entry.get("resourceType") or ""
    resource_id = entry.get("resourceId") or ""
    resource = f"{resource_type}:{resource_id}" if resource_type else resource_id

    mark = paint("OK  ", GREEN) if outcome == "SUCCESS" else paint("FAIL", RED, BOLD)

    parts = [
        paint(stamp, DIM),
        paint(service.ljust(width), colour, BOLD),
        mark,
        action,
    ]
    if resource:
        parts.append(paint(resource, DIM))

    line = "  ".join(parts)

    actor = entry.get("actor")
    if actor and actor != "anonymous":
        line += paint(f"  actor={actor}", DIM)

    reason = entry.get("reason")
    if reason:
        line += paint(f"  reason={reason}", RED)

    attributes = entry.get("attributes") or {}
    if attributes:
        rendered = " ".join(f"{k}={v}" for k, v in attributes.items())
        line += paint(f"  {rendered}", DIM)

    trace_id = entry.get("traceId")
    if trace_id:
        line += paint(f"  trace={trace_id}", DIM)

    return line


def emit(raw: str, service: str, args: argparse.Namespace, paint: Painter, colour: str, width: int) -> None:
    if args.raw:
        print(raw, flush=True)
        return
    try:
        entry = json.loads(raw)
    except json.JSONDecodeError:
        # Never hide a line just because it will not parse: a corrupt entry in an
        # audit trail is itself worth seeing.
        print(paint(f"{service}  <unparseable> {raw}", RED), flush=True)
        return
    if matches(entry, args):
        print(format_entry(entry, service, paint, colour, width), flush=True)


def tail(args: argparse.Namespace) -> int:
    paint = Painter(supports_colour(args.color))
    files = discover(args.dir, args.service)

    if not files:
        where = os.path.abspath(args.dir)
        print(f"No audit logs found in {where}", file=sys.stderr)
        print("Start a service with ./gradlew :customer-service:bootRun -Pdev, "
              "or set AUDIT_LOG_DIR if the logs live elsewhere.", file=sys.stderr)
        return 1

    width = max(len(f.service) for f in files)
    colours = {f.service: PALETTE[i % len(PALETTE)] for i, f in enumerate(files)}

    print(paint(f"Following {len(files)} audit log(s) in {os.path.abspath(args.dir)}:", BOLD), file=sys.stderr)
    for f in files:
        print("  " + paint(f.service, colours[f.service], BOLD) + f"  {f.path}", file=sys.stderr)
    print(file=sys.stderr)

    # Replay the tail of each file, merged into true chronological order rather
    # than grouped by file - the point of this tool is cross-service sequence.
    if args.lines > 0:
        backlog = []
        for f in files:
            try:
                with open(f.path, "r", encoding="utf-8", errors="replace") as handle:
                    for line in handle.read().splitlines()[-args.lines:]:
                        if line.strip():
                            backlog.append((sort_key(line), line.strip(), f.service))
            except FileNotFoundError:
                continue
        backlog.sort(key=lambda row: row[0])
        for _, line, service in backlog[-args.lines:]:
            emit(line, service, args, paint, colours[service], width)

    for f in files:
        f.open_at(from_start=False)

    if not args.follow:
        return 0

    try:
        while True:
            # Collect everything that arrived this cycle across all files, then
            # emit it in timestamp order. Without the sort, a poll that picks up
            # new lines from two services prints one file's batch and then the
            # other's, which reads as out-of-order - and cross-service ordering is
            # the entire reason this tool exists.
            #
            # Ordering is exact within a poll window. An entry flushed late by one
            # service can still land after a newer entry from another; the window
            # is 250ms, which is close enough for reading a live system.
            batch = []
            for f in files:
                for line in f.read_new():
                    batch.append((sort_key(line), line, f.service))

            if not batch:
                time.sleep(POLL_SECONDS)
                continue

            batch.sort(key=lambda row: row[0])
            for _, line, service in batch:
                emit(line, service, args, paint, colours[service], width)
    except KeyboardInterrupt:
        return 0


def sort_key(line: str) -> str:
    try:
        return str(json.loads(line).get("occurredAt", ""))
    except json.JSONDecodeError:
        return ""


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Follow the audit trail of every service at once.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__.split("Examples\n--------\n")[-1],
    )
    parser.add_argument("-d", "--dir", default=DEFAULT_LOG_DIR,
                        help=f"audit log directory (default: {DEFAULT_LOG_DIR})")
    parser.add_argument("-s", "--service", help="only this service")
    parser.add_argument("-a", "--action", help="only this action, e.g. FUNDS_TRANSFERRED")
    parser.add_argument("--actor", help="only this actor")
    parser.add_argument("--failures", action="store_true", help="only failed or refused attempts")
    parser.add_argument("-n", "--lines", type=int, default=20,
                        help="entries of history to replay first (default: 20, 0 for none)")
    parser.add_argument("-F", "--no-follow", dest="follow", action="store_false",
                        help="print and exit instead of following")
    parser.add_argument("--raw", action="store_true", help="emit the original JSON lines")
    parser.add_argument("--color", action="store_true", help="force colour even when piped")
    args = parser.parse_args()

    # Exit quietly on a broken pipe, e.g. when piped into head.
    signal.signal(signal.SIGPIPE, signal.SIG_DFL)
    return tail(args)


if __name__ == "__main__":
    sys.exit(main())
