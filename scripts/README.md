# JaBa Banking Database Population Script

This folder contains utility scripts for the JaBa Banking application.

## populate_db.py

A Python script for populating the SQLite database with dummy data for testing and development purposes.

### Prerequisites

- Python 3.6 or higher
- SQLite3

### Usage

```bash
python populate_db.py [options]
```

### Options

- `--db-path PATH`: Path to the SQLite database file (default: ../banking.db)
- `--users N`: Number of users to generate (default: 20)
- `--accounts N`: Number of accounts to generate (default: 30)
- `--transactions N`: Number of transactions to generate (default: 100)
- `--loans N`: Number of loans to generate (default: 25)
- `--clear`: Clear existing data before inserting new data
- `--help`: Show the help message

### Examples

Generate default amount of test data:

```bash
python populate_db.py
```

Generate specific amounts of data:

```bash
python populate_db.py --users 50 --accounts 80 --transactions 500 --loans 100
```

Clear existing data and generate new test data:

```bash
python populate_db.py --clear
```

Specify a different database location:

```bash
python populate_db.py --db-path "C:/path/to/your/banking.db"
```

### Generated Data

1. **Users**: A mix of customers and employees with realistic names and email addresses.
2. **Accounts**: Checking and savings accounts with random balances.
3. **Transactions**: Various transaction types including deposits, withdrawals, and transfers.
4. **Loans**: Loan requests with different types, amounts, and statuses.
