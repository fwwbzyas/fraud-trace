"""Read database settings from the process environment; .env is not loaded implicitly."""
import os


def get_db_config(default_database="banksys"):
    password = os.environ.get("DB_PASSWORD")
    if not password or not password.strip():
        raise RuntimeError("Set DB_PASSWORD before running a data generator")
    return {
        "host": os.environ.get("DB_HOST", "localhost"),
        "port": int(os.environ.get("DB_PORT", "5432")),
        "database": os.environ.get("DB_NAME", default_database),
        "user": os.environ.get("DB_USER", "postgres"),
        "password": password,
    }
