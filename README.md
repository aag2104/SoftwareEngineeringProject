# SMU Scientific Hub

This portal aims to provide an open platform that supports and facilitates collaborations between SMU and local industry. Industry can publish a challenge, and our platform will recommend SMU researcher(s), based on AI, Machine Learning, Deep Learning, Natural Language Processing, Knowledge Graph, and Data Mining techniques.

## Project Overview
This is a frontend-backend separated project, with each running on different ports.

- Frontend Port: `9036`
- Backend Port: `9037`

### How to Run the Project
To run the project, open both the backend and frontend projects and manage them using the sbt shell.

Steps
1. Open the project with the `build.sbt` file.

2. In the sbt shell, enter the following command:

```bash
run
```

This will start the frontend and backend on their respective ports. You can access the landing page by navigating to `http://localhost:9036`.

## Features

### F2 - Schedule RA Job Interview

This feature enables students to respond to faculty interview requests for RA job applications.

**Student Capabilities**:
- View all interview requests from faculty
- Accept one of three faculty-proposed time slots
- Propose alternative meeting times with optional notes
- Cancel confirmed interviews (within 24-hour window)
- Receive automated email reminders (24 hours and 1 hour before interview)

**Interview Status Flow**:
```
PENDING (awaiting student response)
    ↓
├─→ ACCEPTED (student accepted slot)
│   ├─→ CONFIRMED (faculty confirmed)
│   └─→ CANCELLED (student cancelled within 24h)
│
└─→ COUNTERED (student proposed alternative time)
```

**Usage**:

1. **Access Interview List**: Navigate to `/interview/myInterviews` to view all interviews
2. **View Interview Details**: Click "View" to see proposed times and faculty information
3. **Accept Slot**: Select a proposed time and click "Accept Selected Slot"
4. **Propose Alternative**: Click "Propose Alternative Time" to suggest a different slot
5. **Cancel Interview**: For accepted interviews, click "Cancel Interview" (available within 24-hour window)

**Development Mode**:
- Auto-login enabled: Session automatically created for student (ID: 1)
- Test data included: 2 sample interviews (Dr. Smith - PENDING, Dr. Johnson - ACCEPTED)
- Email reminders: Log to console (set `email.enabled=true` in `backend/conf/application.conf` to enable actual email delivery)

**Configuration** (in `backend/conf/application.conf`):
```conf
email {
  enabled = false          # Set to true to send actual emails
  from    = "no-reply@rajob.edu"
  smtp {
    host     = "smtp.gmail.com"
    port     = 587
    auth     = true
    starttls = true
    user     = ${?MAIL_USER}       # Set MAIL_USER environment variable
    password = ${?MAIL_PASSWORD}   # Set MAIL_PASSWORD environment variable
  }
}
```

**API Endpoints**:
- `GET /interview/myInterviews` - List student's interviews
- `GET /interview/detail/:id` - View interview details
- `POST /interview/accept/:id` - Accept proposed slot
- `POST /interview/counter/:id` - Submit counter-offer
- `POST /interview/cancel/:id` - Cancel interview

**Documentation**: See [docs/features/f2.md](docs&features/f2.md) for comprehensive feature documentation.

**Testing**: Run tests with `sbt test` in backend directory. Current coverage: Unit tests, integration tests, and E2E test scenarios.

## Contribution Guidelines
For contributors, please follow these guidelines when making changes to the code:

- Create a new branch named after yourself.
- Make modifications in your own branch.
- Submit your changes via a pull request for review and merging.

By following this process, we ensure that all code changes are tracked and reviewed properly before being merged into the main project.
