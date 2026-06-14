#!/usr/bin/env python3
"""Send a JSON message to the local DataMaster Kafka container."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Send a UTF-8 JSON message to Kafka without shell quote issues."
    )
    message = parser.add_mutually_exclusive_group(required=True)
    message.add_argument("--id", help='Build a message like {"id":"VALUE","username":"user-VALUE"}.')
    message.add_argument("--message", help="JSON object to send, for example '{\"id\":\"test789\",\"username\":\"zhangsan\"}'.")
    message.add_argument("--file", help="Read the JSON message from a UTF-8 file.")
    parser.add_argument("--username", help="Username value used with --id. Default: user-{id}.")
    parser.add_argument("--count", type=int, default=1, help="Number of messages to send with --id. Default: 1.")
    parser.add_argument("--topic", default="test-topic", help="Kafka topic. Default: test-topic.")
    parser.add_argument(
        "--bootstrap-server",
        default="127.0.0.1:9092",
        help="Kafka bootstrap server inside the container. Default: 127.0.0.1:9092.",
    )
    parser.add_argument("--container", default="dm-kafka", help="Kafka Docker container. Default: dm-kafka.")
    parser.add_argument("--wsl-distro", default="Ubuntu", help="WSL distro running Docker. Default: Ubuntu.")
    return parser.parse_args()


def build_messages(args: argparse.Namespace) -> str:
    if args.id is not None:
        if args.count < 1:
            raise ValueError("--count must be greater than 0.")
        payloads = []
        for index in range(args.count):
            message_id = str(args.id) if args.count == 1 else f"{args.id}{index + 1}"
            username = str(args.username) if args.username is not None and args.count == 1 else (
                f"{args.username}{index + 1}" if args.username is not None else f"user-{message_id}"
            )
            payloads.append({"id": message_id, "username": username})
    elif args.file is not None:
        with open(args.file, "r", encoding="utf-8-sig") as stream:
            payloads = [json.load(stream)]
    else:
        payloads = [json.loads(args.message)]

    for payload in payloads:
        if not isinstance(payload, dict):
            raise ValueError("Kafka message must be a JSON object.")
    return "".join(json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n" for payload in payloads)


def send_message(args: argparse.Namespace, message: str) -> None:
    command = [
        "wsl",
        "-d",
        args.wsl_distro,
        "-e",
        "docker",
        "exec",
        "-i",
        args.container,
        "/opt/kafka/bin/kafka-console-producer.sh",
        "--bootstrap-server",
        args.bootstrap_server,
        "--topic",
        args.topic,
    ]
    completed = subprocess.run(command, input=message, text=True, encoding="utf-8")
    if completed.returncode:
        raise RuntimeError(f"kafka-console-producer exited with status {completed.returncode}")


def main() -> int:
    args = parse_args()
    try:
        message = build_messages(args)
        send_message(args, message)
    except (OSError, json.JSONDecodeError, RuntimeError, ValueError) as error:
        print(f"[send-kafka-message] error: {error}", file=sys.stderr)
        return 1

    print(f"[send-kafka-message] sent to {args.topic}: {message.rstrip()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
