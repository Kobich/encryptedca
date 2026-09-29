#!/usr/bin/env python3
"""Turns a client .p12 and a CA certificate into QR codes for the app's QR import.

Format: docs/qr-profile-format.md. The .p12 password is never put into the codes.

    pip install "qrcode[pil]"
    python make_profile_qr.py client.p12 ca.pem --out qr-out

Writes one PNG per code plus profile_sheet.png with all codes on one image.
"""
import argparse
import base64
import hashlib
import os
import struct
import sys
from pathlib import Path

import qrcode
from PIL import Image

PREFIX = "ECA1"
MAX_PARTS = 99


def build_payload(p12: bytes, ca: bytes) -> bytes:
    """u32 p12 length | p12 | u32 CA length | CA | SHA-256 of everything before."""
    body = struct.pack(">I", len(p12)) + p12 + struct.pack(">I", len(ca)) + ca
    return body + hashlib.sha256(body).digest()


def split_into_codes(payload: bytes, chunk_size: int) -> list[str]:
    text = base64.b64encode(payload).decode("ascii")
    parts = [text[i:i + chunk_size] for i in range(0, len(text), chunk_size)]
    if len(parts) > MAX_PARTS:
        sys.exit(f"{len(parts)} codes needed, at most {MAX_PARTS} allowed: raise --chunk")
    profile_id = os.urandom(4).hex()
    return [f"{PREFIX}:{profile_id}:{n}/{len(parts)}:{part}" for n, part in enumerate(parts, 1)]


def check_round_trip(codes: list[str], p12: bytes, ca: bytes) -> None:
    """Joins the codes back the way the app does and compares with the input."""
    text = "".join(code.split(":", 3)[3] for code in codes)
    payload = base64.b64decode(text)
    body, digest = payload[:-32], payload[-32:]
    assert hashlib.sha256(body).digest() == digest, "checksum mismatch"
    p12_len = struct.unpack(">I", body[:4])[0]
    ca_len = struct.unpack(">I", body[4 + p12_len:8 + p12_len])[0]
    assert body[4:4 + p12_len] == p12 and body[8 + p12_len:8 + p12_len + ca_len] == ca, "payload mismatch"


def render(code: str) -> Image.Image:
    qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=6, border=4)
    qr.add_data(code)
    qr.make(fit=True)
    return qr.make_image(fill_color="black", back_color="white").convert("RGB")


def make_sheet(images: list[Image.Image], columns: int) -> Image.Image:
    cell = max(max(img.size) for img in images)
    rows = (len(images) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * cell, rows * cell), "white")
    for i, img in enumerate(images):
        sheet.paste(img, ((i % columns) * cell, (i // columns) * cell))
    return sheet


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("p12", type=Path, help="client .p12 (password-protected, as is)")
    parser.add_argument("ca", type=Path, help="CA certificate, PEM or DER")
    parser.add_argument("--out", type=Path, default=Path("qr-out"), help="output folder (default: qr-out)")
    parser.add_argument("--chunk", type=int, default=900,
                        help="base64 characters per code (default: 900); smaller codes read easier, but there are more of them")
    parser.add_argument("--columns", type=int, default=3, help="codes per row on the sheet (default: 3)")
    args = parser.parse_args()

    p12, ca = args.p12.read_bytes(), args.ca.read_bytes()
    codes = split_into_codes(build_payload(p12, ca), args.chunk)
    check_round_trip(codes, p12, ca)

    args.out.mkdir(parents=True, exist_ok=True)
    images = [render(code) for code in codes]
    width = len(str(len(codes)))
    for n, img in enumerate(images, 1):
        img.save(args.out / f"profile_{n:0{width}d}.png")
    make_sheet(images, args.columns).save(args.out / "profile_sheet.png")

    print(f".p12 {len(p12)} B, CA {len(ca)} B -> {len(codes)} codes, profile id {codes[0].split(':')[1]}")
    print(f"Saved to {args.out.resolve()}")


if __name__ == "__main__":
    main()
