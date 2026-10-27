# NoDue Student Clearance

Java Swing desktop app with student and administrator dashboards. It uses the in-memory demo store by default. Configure MySQL as below to persist student records, login credentials, and clearance status.

## Connect to MySQL

1. Start MySQL Server and execute `sql/no_due.sql` in MySQL Workbench, or run `mysql -u root -p < sql/no_due.sql` from this project directory. The script creates `nodue`, its tables, and two example student records.
2. Set these environment variables before launching the app (replace the password with your MySQL account password):

   Linux/macOS:

   ```sh
   export NODUE_DB_URL='jdbc:mysql://localhost:3306/nodue'
   export NODUE_DB_USER='root'
   export NODUE_DB_PASSWORD='your_mysql_password'
   ```

   Windows PowerShell:

   ```powershell
   $env:NODUE_DB_URL = 'jdbc:mysql://localhost:3306/nodue'
   $env:NODUE_DB_USER = 'root'
   $env:NODUE_DB_PASSWORD = 'your_mysql_password'
   ```

3. Include MySQL Connector/J on the runtime classpath. With Maven installed, the included `pom.xml` downloads the JDBC driver:

   ```sh
   mvn dependency:copy-dependencies
   javac -d out src/*.java
   java -cp 'out:target/dependency/*' Main
   ```

   On Windows, use `java -cp "out;target/dependency/*" Main` (classpath separator is `;` instead of `:`).

   On Fedora, install Maven first with `sudo dnf install maven`. If you prefer not to install Maven, download MySQL Connector/J and place its `.jar` file in a `lib` directory, then launch with `java -cp 'out:lib/*' Main`.

When `NODUE_DB_URL` is set, the app reads and writes the MySQL `students` table. Without it, the app runs with temporary sample data. Use **New student registration** to create an account. The app displays the generated numeric student ID; students can log in with that ID or their roll number plus the password they chose. Passwords are stored as salted PBKDF2 hashes, not plain text. A student row pre-created by an administrator can claim its account by registering with details matching that row. You can inspect student rows with `USE nodue; SELECT id, roll_no, name, department, semester, cleared FROM students;`.

If you imported an older copy of the schema before the `cleared` and password columns were added, run this once in MySQL Workbench before starting the database-backed app:

```sql
USE nodue;
ALTER TABLE students ADD COLUMN cleared BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE students ADD COLUMN password_hash VARCHAR(100) NULL;
ALTER TABLE students ADD COLUMN password_salt VARCHAR(50) NULL;
```

## Demo accounts and limitations

The SQL script inserts sample student rows `CSE001` and `ECE002`; they must register before login. The **Pay due** button only sets the clearance flag in the database; it does not charge money or integrate with a payment provider. This remains a local prototype, not a production payment system.

## Compile without MySQL

Java 8 or newer is required. The code compiles without the driver when using the in-memory mode:

```sh
mkdir -p out
javac -d out src/*.java
java -cp out Main
```
