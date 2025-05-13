#!/usr/bin/env python3
"""
Database Population Script for JaBa-Banking

This script populates the SQLite database with dummy data for testing purposes.
It creates users, accounts, transactions, and loans with realistic relationships.

Usage:
  python populate_db.py [--db-path PATH] [--users N] [--accounts N] [--transactions N] [--loans N]

Options:
  --db-path PATH     Path to the SQLite database file (default: ../banking.db)
  --users N          Number of users to generate (default: 20)
  --accounts N       Number of accounts to generate (default: 30)
  --transactions N   Number of transactions to generate (default: 100)
  --loans N          Number of loans to generate (default: 25)
  --clear            Clear existing data before inserting new data
  --help             Show this help message
"""

import sqlite3
import argparse
import random
import datetime
import os
import hashlib
import base64
from typing import List, Dict, Tuple, Any


def parse_args() -> argparse.Namespace:
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description="Populate JaBa-Banking database with test data"
    )
    parser.add_argument(
        "--db-path", default="../banking.db", help="Path to SQLite database"
    )
    parser.add_argument(
        "--users", type=int, default=20, help="Number of users to generate"
    )
    parser.add_argument(
        "--accounts", type=int, default=30, help="Number of accounts to generate"
    )
    parser.add_argument(
        "--transactions",
        type=int,
        default=100,
        help="Number of transactions to generate",
    )
    parser.add_argument(
        "--loans", type=int, default=25, help="Number of loans to generate"
    )
    parser.add_argument(
        "--clear", action="store_true", help="Clear existing data before inserting"
    )
    return parser.parse_args()


def connect_to_db(db_path: str) -> sqlite3.Connection:
    """Connect to the SQLite database."""
    # Ensure the database directory exists
    db_dir = os.path.dirname(db_path)
    if db_dir and not os.path.exists(db_dir):
        os.makedirs(db_dir)

    return sqlite3.connect(db_path)


def generate_salt() -> str:
    """Generate a random salt for password hashing."""
    return base64.b64encode(os.urandom(16)).decode("utf-8")


def hash_password(password: str, salt: str) -> str:
    """Hash a password with the given salt."""
    # This mimics PasswordUtil.hashPassword method
    salted_password = (password + salt).encode("utf-8")
    return hashlib.sha256(salted_password).hexdigest()


def clear_data(conn: sqlite3.Connection) -> None:
    """Clear all data from the database tables."""
    print("Clearing existing data...")
    cursor = conn.cursor()
    # Delete in reverse order of dependencies
    cursor.execute("DELETE FROM loans")
    cursor.execute("DELETE FROM transactions")
    cursor.execute("DELETE FROM accounts")
    cursor.execute(
        "DELETE FROM users WHERE email != 'admin@jababanking.com'"
    )  # Preserve default admin
    conn.commit()


def generate_users(conn: sqlite3.Connection, count: int) -> List[int]:
    """Generate random users and insert them into the database. Returns list of user IDs."""
    print(f"Generating {count} users...")
    cursor = conn.cursor()

    first_names = [
        "James",
        "Mary",
        "John",
        "Patricia",
        "Robert",
        "Jennifer",
        "Michael",
        "Linda",
        "William",
        "Elizabeth",
        "David",
        "Barbara",
        "Richard",
        "Susan",
        "Joseph",
        "Jessica",
        "Thomas",
        "Sarah",
        "Charles",
        "Karen",
        "Emma",
        "Noah",
        "Olivia",
        "Liam",
        "Ava",
        "Sophia",
        "Mason",
        "Isabella",
        "Jacob",
        "Mia",
        "Muhammad",
        "Charlotte",
        "Ethan",
        "Amelia",
        "Joshua",
        "Harper",
        "Oliver",
        "Evelyn",
    ]

    last_names = [
        "Smith",
        "Johnson",
        "Williams",
        "Jones",
        "Brown",
        "Davis",
        "Miller",
        "Wilson",
        "Moore",
        "Taylor",
        "Anderson",
        "Thomas",
        "Jackson",
        "White",
        "Harris",
        "Martin",
        "Thompson",
        "Garcia",
        "Martinez",
        "Robinson",
        "Clark",
        "Rodriguez",
        "Lewis",
        "Lee",
        "Walker",
        "Hall",
        "Allen",
        "Young",
        "Hernandez",
        "King",
        "Wright",
        "Lopez",
        "Hill",
        "Scott",
        "Green",
        "Adams",
        "Baker",
        "Gonzalez",
        "Nelson",
        "Carter",
    ]

    domains = [
        "gmail.com",
        "yahoo.com",
        "outlook.com",
        "hotmail.com",
        "aol.com",
        "icloud.com",
    ]

    user_ids = []
    for _ in range(count):
        first_name = random.choice(first_names)
        last_name = random.choice(last_names)
        name = f"{first_name} {last_name}"

        # Create a deterministic email from the name
        email_base = f"{first_name.lower()}.{last_name.lower()}"
        email_domain = random.choice(domains)
        # Add a random number to ensure uniqueness
        email = f"{email_base}{random.randint(1, 999)}@{email_domain}"

        # Password will be "password123" for all test users
        salt = generate_salt()
        password_hash = hash_password("password123", salt)

        # Assign roles - mostly customers, some employees, admins already exist
        role_choice = random.randint(1, 10)
        if role_choice == 1:
            role = "employee"
        else:
            role = "customer"

        # Insert the user
        try:
            cursor.execute(
                """
                INSERT INTO users (name, email, password_hash, salt, role, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
                (
                    name,
                    email,
                    password_hash,
                    salt,
                    role,
                    "active",
                    datetime.datetime.now(),
                ),
            )
            user_id = cursor.lastrowid
            user_ids.append(user_id)
        except sqlite3.IntegrityError:
            # Email might already exist, just skip
            print(f"Skipping duplicate email: {email}")

    conn.commit()
    return user_ids


def generate_accounts(
    conn: sqlite3.Connection, user_ids: List[int], count: int
) -> List[int]:
    """Generate random accounts for users. Returns list of account IDs."""
    print(f"Generating {count} accounts...")
    cursor = conn.cursor()

    account_ids = []
    for _ in range(count):
        user_id = random.choice(user_ids)
        account_type = random.choice(["checking", "savings"])

        # Generate a unique account number
        account_number = f"{random.randint(100000000, 999999999)}"

        # Random balance between $100 and $50,000
        balance = round(random.uniform(100, 50000), 2)

        # Random creation date within the last 2 years
        days_ago = random.randint(1, 730)
        created_at = datetime.datetime.now() - datetime.timedelta(days=days_ago)

        try:
            cursor.execute(
                """
                INSERT INTO accounts (user_id, account_number, type, balance, created_at)
                VALUES (?, ?, ?, ?, ?)
            """,
                (user_id, account_number, account_type, balance, created_at),
            )
            account_id = cursor.lastrowid
            account_ids.append(account_id)
        except sqlite3.IntegrityError:
            # Account number might already exist, just skip
            print(f"Skipping duplicate account number: {account_number}")

    conn.commit()
    return account_ids


def generate_transactions(
    conn: sqlite3.Connection, account_ids: List[int], count: int
) -> None:
    """Generate random transactions between accounts."""
    print(f"Generating {count} transactions...")
    cursor = conn.cursor()

    transaction_types = ["deposit", "withdrawal", "transfer"]
    descriptions = [
        "Salary Deposit",
        "ATM Withdrawal",
        "Online Transfer",
        "Bill Payment",
        "Grocery Shopping",
        "Restaurant",
        "Rent Payment",
        "Subscription",
        "Refund",
        "Investment",
        "Coffee Shop",
        "Amazon Purchase",
        "Netflix",
        "Uber",
        "Utilities",
        "Mobile Phone",
        "Insurance",
        "Medical",
        "Travel",
        "Entertainment",
    ]

    for _ in range(count):
        # For transfers, we need 2 accounts
        from_account_id = random.choice(account_ids)

        # Determine transaction type
        transaction_type = random.choice(transaction_types)

        # Handle different transaction types
        if transaction_type == "deposit":
            to_account_id = from_account_id
            from_account_id = None  # External source
        elif transaction_type == "withdrawal":
            to_account_id = None  # External destination
        else:  # transfer
            # Ensure different accounts for transfers
            to_account_id = random.choice(
                [acc for acc in account_ids if acc != from_account_id]
            )

        # Generate random amount between $5 and $3000
        amount = round(random.uniform(5, 3000), 2)

        # Random description
        description = random.choice(descriptions)

        # Random timestamp within the last 90 days
        days_ago = random.randint(0, 90)
        hours_ago = random.randint(0, 23)
        minutes_ago = random.randint(0, 59)
        timestamp = datetime.datetime.now() - datetime.timedelta(
            days=days_ago, hours=hours_ago, minutes=minutes_ago
        )

        cursor.execute(
            """
            INSERT INTO transactions (from_account_id, to_account_id, type, amount, description, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
        """,
            (
                from_account_id,
                to_account_id,
                transaction_type,
                amount,
                description,
                timestamp,
            ),
        )

    conn.commit()


def generate_loans(conn: sqlite3.Connection, user_ids: List[int], count: int) -> None:
    """Generate random loan requests for users."""
    print(f"Generating {count} loans...")
    cursor = conn.cursor()

    loan_types = ["Personal", "Home", "Auto", "Education", "Business"]
    loan_statuses = ["pending", "approved", "rejected"]

    rejection_reasons = [
        "Insufficient income",
        "Poor credit history",
        "High debt-to-income ratio",
        "Incomplete documentation",
        "Employment history concerns",
    ]

    for _ in range(count):
        user_id = random.choice(user_ids)
        loan_type = random.choice(loan_types)

        # Random amount based on loan type
        if loan_type == "Personal":
            amount = round(random.uniform(1000, 25000), 2)
        elif loan_type == "Home":
            amount = round(random.uniform(50000, 500000), 2)
        elif loan_type == "Auto":
            amount = round(random.uniform(5000, 50000), 2)
        elif loan_type == "Education":
            amount = round(random.uniform(5000, 100000), 2)
        else:  # Business
            amount = round(random.uniform(10000, 250000), 2)

        # Random date within the last 180 days
        days_ago = random.randint(0, 180)
        date = datetime.datetime.now() - datetime.timedelta(days=days_ago)

        # Status and admin comment
        status = random.choice(loan_statuses)
        admin_comment = None

        # If rejected, add a reason
        if status == "rejected":
            admin_comment = random.choice(rejection_reasons)

        cursor.execute(
            """
            INSERT INTO loans (userid, amount, type, date, status, adminComment)
            VALUES (?, ?, ?, ?, ?, ?)
        """,
            (user_id, amount, loan_type, date, status, admin_comment),
        )

    conn.commit()


def main() -> None:
    """Main function to run the script."""
    args = parse_args()

    # Connect to the database
    conn = connect_to_db(args.db_path)
    print(f"Connected to database: {args.db_path}")

    # Clear existing data if requested
    if args.clear:
        clear_data(conn)

    # Generate data
    user_ids = generate_users(conn, args.users)
    account_ids = generate_accounts(conn, user_ids, args.accounts)
    generate_transactions(conn, account_ids, args.transactions)
    generate_loans(conn, user_ids, args.loans)

    # Close connection
    conn.close()
    print("Database population complete!")


if __name__ == "__main__":
    main()
