#!/usr/bin/env python3
import argparse
import sys
import getpass
from datetime import datetime, timezone

from trino.dbapi import connect
from trino.auth import BasicAuthentication

import boto3
boto3.compat.filter_python_deprecation_warnings()
from botocore.exceptions import ClientError

CATALOG = "vast"
SCHEMA = 'vast-audit-log-bucket|vast_audit_log_schema'
TAG_KEY = "trueatime"


def upsert_object_tag(s3, bucket: str, key: str, tag_key: str, tag_value: str, dry_run: bool = False):
    if dry_run:
        print(f"DRYRUN tag s3://{bucket}/{key} -> {tag_key}={tag_value}")
        return

    # Get existing tags (if any)
    try:
        existing = s3.get_object_tagging(Bucket=bucket, Key=key).get("TagSet", [])
    except ClientError as e:
        # If the object doesn't exist, skip
        if e.response.get("Error", {}).get("Code") in ("NoSuchKey", "404"):
            print(f"SKIP missing object: s3://{bucket}/{key}")
            return
        raise

    # Merge/update by Key
    merged = {t["Key"]: t["Value"] for t in existing}
    merged[tag_key] = tag_value
    new_tagset = [{"Key": k, "Value": v} for k, v in merged.items()]

    print(f"tag s3://{bucket}/{key} -> {tag_key}={tag_value}")
    s3.put_object_tagging(
        Bucket=bucket,
        Key=key,
        Tagging={"TagSet": new_tagset},
    )


def parse_since_arg(since: str) -> str:
    """
    Convert a flag like '1h', '2d', '30m' into a Trino interval literal string.
    """
    if not since:
        raise ValueError("since value is required")

    unit = since[-1]
    value = since[:-1]

    try:
        float(value)
    except ValueError:
        raise ValueError(f"Invalid since value: {since!r}")

    # Map short unit to Trino interval unit
    if unit == "s":
        trino_unit = "second"
    elif unit == "m":
        trino_unit = "minute"
    elif unit == "h":
        trino_unit = "hour"
    elif unit == "d":
        trino_unit = "day"
    else:
        raise ValueError(f"Unsupported time unit {unit!r}; use s, m, h, or d")

    # Trino interval literal, e.g. INTERVAL '1' hour
    return f"INTERVAL '{value}' {trino_unit}"


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Query latest audit log entries per file from vast_audit_log_table."
    )
    parser.add_argument(
        "--since",
        required=True,
        help="How far back to search, e.g. 1h, 2d, 30m (seconds, minutes, hours, days).",
    )
    parser.add_argument("--host", required=True, help="Trino coordinator host")
    parser.add_argument("--port", type=int, default=8080, help="Trino coordinator port")
    parser.add_argument("--user", required=True, help="Trino user")
    parser.add_argument("--password", default=None, help="Trino password (if omitted, will prompt securely)")
    parser.add_argument("--view-path", required=True, help="If set, only print/tag objects whose path starts with this prefix, e.g. /testdir/. Note, this gets parsed from the path")
    parser.add_argument("--bucket", required=True, help="S3 bucket name to tag")
    parser.add_argument("--s3-endpoint", required=True, help="Custom S3 endpoint URL, e.g. https://my-vast-s3.example.com")
    parser.add_argument("--access-key", required=True, help="S3 access key ID")
    parser.add_argument("--secret-key", required=True, help="S3 secret access key")
    parser.add_argument("--dry-run", action="store_true", help="Do not modify tags; just log actions")


    args = parser.parse_args(argv)

    password = args.password or getpass.getpass(prompt=f"Trino password for {args.user}: ")

    interval_literal = parse_since_arg(args.since)
    where_view_path = ""
    params = []
    if args.view_path:
        # Safely escape single quotes in the path value, then inline it
        safe_path = args.view_path.replace("'", "''")
        where_view_path = f"AND starts_with(path.path, '{safe_path}')"

    # Build SQL: latest time per path within the given time window
    # Assume "time" is a timestamp column and "path" is the file path.
    sql = f"""
    SELECT path.path AS file_path, max("time") AS latest_time
    FROM vast_audit_log_table
    WHERE "time" >= current_timestamp - {interval_literal}
    {where_view_path}
    GROUP BY path.path
    """

    conn = connect(
        host=args.host,
        port=args.port,
        user=args.user,
        auth=BasicAuthentication(args.user, password),
        http_scheme="https",
        verify=False,
        catalog=CATALOG,
        schema=SCHEMA,
    )
    cursor = conn.cursor()

    # Execute the query; cursor now holds (path, latest_time) rows
    cursor.execute(sql)

    s3 = boto3.client(
        "s3",
        endpoint_url=args.s3_endpoint,
        aws_access_key_id=args.access_key,
        aws_secret_access_key=args.secret_key
    )

    # Example: consume the cursor, or you can return it from a function instead.
    rows = cursor.fetchall()
    for file_path, latest_time in rows:
        key = file_path.removeprefix(args.view_path).lstrip("/")

        # Skip if the result is empty (means the row is exactly the view path itself)
        if not key:
            print(f"SKIP directory or empty key from path: {file_path}")
            continue

        # Optionally also skip “directory marker” keys if you don’t want to tag them:
        if key.endswith("/"):
            print(f"SKIP directory marker: s3://{args.bucket}/{key}")
            continue

        # S3 tag values must be strings; latest_time may be datetime-like
        tag_value = str(latest_time)

        upsert_object_tag(
            s3,
            bucket=args.bucket,
            key=key,
            tag_key=TAG_KEY,
            tag_value=tag_value,
            dry_run=args.dry_run,
        )

if __name__ == "__main__":
    sys.exit(main())
