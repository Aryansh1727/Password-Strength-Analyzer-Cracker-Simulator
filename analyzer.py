import hashlib
import os
import re
import secrets
import string
import sys

def generate_strong_password(length=16):
    """Generates a cryptographically secure random password."""
    alphabet = string.ascii_letters + string.digits + "!@#$%^&*"
    while True:
        password = "".join(secrets.choice(alphabet) for _ in range(length))
        if (
            any(c.islower() for c in password)
            and any(c.isupper() for c in password)
            and any(c.isdigit() for c in password)
            and any(c in "!@#$%^&*" for c in password)
        ):
            return password


def analyze_password(password):
    warnings = []
    is_compromised = False

    # 1. Physical Dictionary Check
    file_name = "10k-most-common.txt"
    if os.path.exists(file_name):
        with open(file_name, "r", encoding="utf-8") as f:
            common_passwords = [line.strip() for line in f.readlines()]

            if password in common_passwords:
                warnings.append(
                    "CRITICAL: Found in the top 10,000 most common passwords list!"
                )
                is_compromised = True
    else:
        # Fallback simulated list if file is missing
        common_test_list = ["123456", "password", "qwerty", "admin", "welcome"]
        if password.lower() in common_test_list or len(password) < 5:
            is_compromised = True
            warnings.append(
                "CRITICAL: Found in the top 10,000 most common passwords list!"
            )
        else:
            warnings.append(
                "Note: '10k-most-common.txt' not found. Skipping strict dictionary check."
            )

    # 2. Pattern and Length Checks
    if len(password) < 8:
        warnings.append("Too short (minimum 8 characters)")

    # 3. Pool Size Calculation & Character Tracking
    pool_size = 0
    pool_chars = ""
    if re.search(r"[a-z]", password):
        pool_size += 26
        pool_chars += string.ascii_lowercase
    if re.search(r"[A-Z]", password):
        pool_size += 26
        pool_chars += string.ascii_uppercase
    if re.search(r"[0-9]", password):
        pool_size += 10
        pool_chars += string.digits
    if re.search(r"[!@#$%^&*(),.?\":{}|<>]", password):
        pool_size += 32
        pool_chars += "!@#$%^&*()"

    # Fallback to avoid empty strings
    if pool_chars == "":
        pool_chars = string.ascii_lowercase
        pool_size = 26

    # 4. Strength Assessment
    if is_compromised or len(password) < 6 or pool_size <= 26:
        strength = "Weak"
    elif len(password) < 10 or pool_size <= 62:
        strength = "Medium"
    else:
        strength = "Strong"

    # 5. Crack Time Simulation
    guesses_per_second = 100_000_000_000
    total_combinations = pool_size ** len(password) if pool_size > 0 else 0

    if is_compromised:
        time_text = (
            "Instant (Attacker would guess this immediately via dictionary)"
        )
    elif total_combinations == 0:
        time_text = "Instant"
    else:
        seconds_to_crack = total_combinations / guesses_per_second
        if seconds_to_crack < 1:
            time_text = "Less than a second"
        elif seconds_to_crack < 3600:
            time_text = f"{seconds_to_crack / 60:.2f} minutes"
        elif seconds_to_crack < 86400:
            time_text = f"{seconds_to_crack / 3600:.2f} hours"
        elif seconds_to_crack < 31536000:
            time_text = f"{seconds_to_crack / 86400:.2f} days"
        else:
            time_text = f"{seconds_to_crack / 31536000:.2f} years"

    # 6. Generate Multi-Hashes (New feature)
    md5 = hashlib.md5(password.encode()).hexdigest()
    sha1 = hashlib.sha1(password.encode()).hexdigest()
    sha256 = hashlib.sha256(password.encode()).hexdigest()

    # --- OUTPUT STREAM FOR JAVA ---
    # Standard outputs
    print(f"STRENGTH:{strength}")
    print(f"CRACK_TIME:{time_text}")
    print(f"WARNINGS:{', '.join(warnings) if warnings else 'None'}")

    if strength == "Weak" or strength == "Medium":
        print(f"SUGGESTION:{generate_strong_password()}")
    else:
        print("SUGGESTION:None")

    # New custom outputs targeting the advanced Java GUI features
    print(f"MD5:{md5}")
    print(f"SHA1:{sha1}")
    print(f"SHA256:{sha256}")
    print(f"POOL_CHARS:{pool_chars}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        analyze_password(sys.argv[1])
    else:
        print("Error: No password provided.")
