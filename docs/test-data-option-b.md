# Option B: authenticated API dataset

Option B creates synthetic integration data through the authenticated REST API instead of inserting rows directly into PostgreSQL.

## Scope

The script creates or reuses one linked scenario containing:

- company and contact;
- potential client;
- professional service;
- proposal and proposal item;
- subscription;
- client-related task;
- job opportunity and job application.

The script is idempotent for the synthetic names, contact email, and opportunity URL. Re-running it reuses existing records instead of creating duplicates.

## Requirements

- Test backend running and healthy.
- The integration user already exists in the selected environment.
- PowerShell with access to `Invoke-RestMethod`.

The password is requested interactively and is never stored in the repository or printed by the script.

## Run

From the repository root, use the test backend by default:

```powershell
& .\scripts\test-data\option-b-api.ps1
```

For another local backend port, provide only the base URL at runtime:

```powershell
& .\scripts\test-data\option-b-api.ps1 -BaseUrl 'http://localhost:8084'
```

The script obtains a CSRF token, logs in with the session cookie, creates the records in dependency order, and prints only generated record IDs at the end.

## Verification

After the script finishes, verify the resulting records through the frontend or the authenticated API. The script does not modify migrations, containers, images, volumes, or production data.
