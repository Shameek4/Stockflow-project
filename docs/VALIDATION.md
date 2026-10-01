# Validation of version 1

- Java 17, Spring Boot 3.5.6, Maven 3.9.9.
- Maven verify: **19 tests passed; 0 failures, 0 errors, 0 skipped**.
- Packaged JAR live HTTP check passed: startup, real password login with CSRF/session cookies,
  dashboard/static asset serving, product creation, stock adjustment, order calculation and
  deduction, repeated cancellation, dashboard revenue, audit records and logout.
- Both frontend JavaScript files passed Node syntax checks.
- Java source formatted with google-java-format.
- MySQL profile/Compose are provided but were not executed here. Integration/concurrency tests use H2.
- Browser visual and interaction automation was not run; the environment could not install
  its browser binary. The live check exercised the API and served UI assets, not rendered clicks.

See ENGINEERING.md for design decisions and README.md for setup and scope.
