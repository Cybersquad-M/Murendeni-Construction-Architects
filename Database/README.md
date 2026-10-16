# Database

This folder contains the database design for the Murendeni Construction Architects mobile app.

## Contents
- schema_azure_sql.sql: table definitions (client, employee, profile, design, payment, inquiry, user_settings)
- ERD: see the Documentation folder (Figure 17)

## Tables
| Table | Purpose |
|---|---|
| client | App users who are clients |
| employee | Staff members |
| user_settings | Theme, language and notification settings |
| profile | Client project profiles |
| design | Designs linked to a profile |
| payment | Payments for profiles and designs |
| inquiry | Client questions and support requests |

## Notes
- Passwords are not stored in the database (handled by the authentication service).
- Sample data: at least 10 records per table (to be added).