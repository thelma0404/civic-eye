# Civic Eye

A web application where **citizens** report civic issues (potholes, garbage, water leaks, broken streetlights…),
**admins** assign them to **field workers**, workers upload photo **proof** of the fix, and admins verify it
before the complaint is closed and the citizen rates the service.

**Stack:** Java 17 · Spring Boot 3.5.16 · Spring Web · Spring Data JPA · Spring Security (BCrypt + session) ·
Thymeleaf · MySQL 8 · HTML / CSS / JavaScript (no frontend framework) · runs on **port 8080**.

**Theme:** dark matcha green, pale lemonade green and earthy tones (clay, ochre, bark, sand).

---

## 1. What you need

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 or newer | `java -version` |
| MySQL Server | 8.x | running on `localhost:3306` |
| VS Code | latest | with the extensions below |
| Maven | optional | VS Code's Java extension can build without it |

VS Code extensions (VS Code will offer to install them when you open the folder):

- **Extension Pack for Java** (`vscjava.vscode-java-pack`)
- **Spring Boot Extension Pack** (`vmware.vscode-boot-dev-pack`)

## 2. Database

Open `src/main/resources/application.properties` and set your MySQL login:

```properties
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}
```

(Replace the `root` after each colon, or set `DB_USERNAME` / `DB_PASSWORD` environment variables.)

You do **not** need to create the database or tables. The app uses its own database, **`civic_eye_db`**,
which is created on first start, and Hibernate creates all tables from the entity classes (`ddl-auto=update`).

Don't point the app at a database that already has tables from another project. Old tables with different
columns cause errors such as "This admin account is disabled" or sign-up failing.

## 3. Run it

**From VS Code**

1. *File → Open Folder…* and pick the `civic-eye` folder.
2. Wait for the Java extension to finish importing the Maven project (bottom status bar). The first import downloads
   the dependencies, so it needs an internet connection.
3. Open the **Run and Debug** panel and start **Civic Eye (Spring Boot)**, or open `CivicEyeApplication.java`
   and click **Run** above `main`. You can also use the **Spring Boot Dashboard**.

**From a terminal** (if Maven is installed)

```bash
mvn spring-boot:run
```

Then open **http://localhost:8080**

## 4. Accounts

Two accounts are created automatically on first start:

| Role | Login page | Email | Password |
|---|---|---|---|
| Admin | http://localhost:8080/admin/login | admin@civic.com | admin123 |
| Field worker | http://localhost:8080/worker/login | worker@civic.com | worker123 |

Citizens register themselves at http://localhost:8080/register. Admins add more workers under **Users & workers**.

Change the admin password from **Profile** after your first login.

## 5. Try the full flow

1. Register as a citizen and **Report an issue** (title, description, location, optional photo).
   The category and priority are suggested live while you type.
2. Log in as admin → **Complaints** → open the complaint → **Assign a worker**.
3. Log in as the worker → **Assigned complaints** → **Start work** → **Upload proof** (a photo).
4. As admin, open the complaint again → **Approve** the proof. The complaint becomes *Resolved*.
5. As the citizen, open the complaint (or **Feedback**) and give a star rating.

Everyone gets in-app notifications along the way (bell icon in the top bar).

## 6. Features

**Citizen** – dashboard, report issue with photo + "use my location", automatic category and priority,
my complaints with filters and search, status timeline, approved proof photos, notifications, feedback, profile.

**Field worker** – dashboard, assigned jobs, complaint details with Google Maps link and citizen contact,
progress notes, photo proof upload, work history with review results, notifications, profile.

**Admin** – dashboard with stats and charts, complaint list with filters, assign / reassign workers,
approve / reject proof, resolve / reject / reopen complaints, edit category and priority, manage citizens and
workers, broadcast notifications, feedback overview, date-range reports with CSV export and print-to-PDF,
and an operations assistant chatbot ("how many pending?", "status of #12", "which workers are available?").

## 7. Project structure

```
src/main/java/com/civic/
  CivicEyeApplication.java     entry point + seeds the default admin and demo worker
  config/SecurityConfig.java     BCrypt, role-based page/API protection, /uploads mapping
  auth/        login, registration, sessions
  user/        User entity (citizens and admins), Role
  citizen/     citizen pages + profile/dashboard API
  complaint/   Complaint entity, citizen complaint API, AiCategoryService (keyword classifier)
  worker/      Worker entity, worker pages/API, admin worker management
  assignment/  complaint ↔ worker assignments and their status
  proof/       work-proof photos and admin review
  notification/ in-app notifications
  feedback/    citizen ratings
  admin/       admin pages, dashboard, complaints, users, notifications, feedback, reports, chatbot
  common/      ApiResponse envelope, exception handling, file uploads, text helpers
src/main/resources/
  application.properties
  templates/   Thymeleaf HTML pages (auth, citizen, worker, admin)
  static/css   style.css (shared + citizen), login.css, worker.css, admin-style.css
  static/js    common.js, login.js, citizen.js, worker.js, admin-*.js
uploads/       complaint photos (issues/) and work proofs (work-proof/) are saved here
src/test/java/ unit tests for WorkerService, AssignmentService and ProofService
```

Files added beyond the original outline: `common/exception/GlobalExceptionHandler.java`,
`common/util/TextUtil.java`, `static/js/common.js`, `static/js/citizen.js`, `static/js/admin-manage.js`,
`static/images/logo.svg` and the `.vscode/` launch settings.

## 8. Complaint lifecycle

```
PENDING ──assign──▶ ASSIGNED ──worker starts──▶ IN_PROGRESS ──proof uploaded──▶ (admin review)
                                                                   │ approve → RESOLVED → citizen rates
                                                                   └ reject  → back to IN_PROGRESS
Admin can also REJECT, RESOLVE or move back to PENDING at any time.
```

## 9. Running the tests

```bash
mvn test
```

The unit tests use Mockito and do not need a database.

## 10. Troubleshooting

- **Want a fresh start / "tables don't match this version" / sign-up or login fails** – stop the app, run
  `DROP DATABASE civic_eye_db;` in MySQL, then run the app again. The tables and the default admin and
  worker are recreated automatically.
- **"Something went wrong on our side (…)"** – the text in brackets is the real cause; the full error is in the
  VS Code terminal.
- **`Access denied for user`** – the MySQL username/password in `application.properties` is wrong.
- **`Communications link failure`** – MySQL isn't running, or it isn't on port 3306.
- **Port 8080 already in use** – stop the other app, or change `server.port` in `application.properties`.
- **Red errors in VS Code right after opening** – wait for the Java project import to finish, or run
  *Java: Clean Java Language Server Workspace* from the command palette.
- **Photos don't show** – they are stored in the `uploads/` folder next to `pom.xml`; start the app from the project
  folder so that relative path resolves.
