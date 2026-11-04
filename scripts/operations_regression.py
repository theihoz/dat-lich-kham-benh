#!/usr/bin/env python3
"""Safety tests run without Docker, credentials, or network."""
import os, subprocess, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class OperationsTests(unittest.TestCase):
    def run_script(self, script, args=(), env=None):
        return subprocess.run(['bash',str(ROOT/script),*args],env={**os.environ,**(env or {})},text=True,capture_output=True,cwd=ROOT)
    def test_restore_refuses_production_name_before_accessing_docker(self):
        r=self.run_script('ops/backup/restore.sh',env={'BACKUP_ENCRYPTION_PASSWORD':'test-only','CLINIC_RESTORE_DATABASE':'clinic'})
        self.assertEqual(r.returncode,2); self.assertIn('Refusing restore',r.stderr)
    def test_restore_requires_decryption_secret(self):
        r=self.run_script('ops/backup/restore.sh',env={'BACKUP_ENCRYPTION_PASSWORD':''})
        self.assertNotEqual(r.returncode,0); self.assertIn('passphrase',r.stderr)
    def test_deploy_refuses_mutable_tag(self):
        r=self.run_script('ops/deploy.sh',['ghcr.io/org/clinic:latest'])
        self.assertEqual(r.returncode,2); self.assertIn('immutable',r.stderr)
    def test_backup_refuses_invalid_retention_before_accessing_docker(self):
        r=self.run_script('ops/backup/backup.sh',env={'BACKUP_ENCRYPTION_PASSWORD':'test-only','CLINIC_BACKUP_RETENTION_DAYS':'0'})
        self.assertEqual(r.returncode,2)
if __name__=='__main__': unittest.main()
