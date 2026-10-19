import os
import unittest
from unittest.mock import patch
from database_config import get_db_config


class DatabaseConfigTests(unittest.TestCase):
    def test_missing_password_fails_before_connection(self):
        for value in ('', '   '):
            with patch.dict(os.environ, {'DB_PASSWORD': value}, clear=True):
                with self.assertRaisesRegex(RuntimeError, 'DB_PASSWORD'):
                    get_db_config()

    def test_database_defaults_and_overrides(self):
        with patch.dict(os.environ, {'DB_PASSWORD': 'test-only'}, clear=True):
            self.assertEqual(get_db_config('formdata')['database'], 'formdata')
        with patch.dict(os.environ, {
            'DB_PASSWORD': 'test-only', 'DB_HOST': 'database', 'DB_PORT': '5433',
            'DB_NAME': 'demo', 'DB_USER': 'demo_user'
        }, clear=True):
            self.assertEqual(get_db_config(), {
                'host': 'database', 'port': 5433, 'database': 'demo',
                'user': 'demo_user', 'password': 'test-only'
            })


if __name__ == '__main__':
    unittest.main()
