# fishing-backend

REST API для PWA-приложения рыбаков (курсовая работа). Spring Boot 4, Java 25, PostgreSQL 16 + PostGIS.

Подробные правила проекта (архитектура, соглашения по БД и API, разделение блоков A/Б, план работ)
лежат в [CLAUDE.md](CLAUDE.md). Его стоит прочитать целиком: это общий договор команды.

## 1. Что установить (один раз)

| Что | Где взять | Проверка в терминале |
|---|---|---|
| JDK 25 | IntelliJ IDEA → Project Structure → SDK → Download JDK → 25, Eclipse Temurin | `java -version` |
| Docker Desktop | docker.com → Docker Desktop | `docker ps` (без ошибки) |
| IntelliJ IDEA 2025.2+ | jetbrains.com (Community хватает) | Help → About |
| Git | git-scm.com | `git --version` |

Maven ставить не нужно: в проекте есть Maven Wrapper (`mvnw` / `mvnw.cmd`), он сам скачает нужную версию.

## 2. Первый запуск

1. Клонировать репозиторий и открыть папку в IDEA.
2. **JDK в IDEA:** File → Project Structure → Project → SDK = **25**.
3. **Запуск без Maven:** Settings → Build, Execution, Deployment → Build Tools → Maven → Runner →
   снять галочку **Delegate IDE build/run actions to Maven**.
4. **Файл `.env`:** скопировать `.env.example` в `.env` (в корне проекта) и заполнить:
   - `DB_PASSWORD` — любой пароль для локальной базы;
   - `JWT_SECRET` — любая строка от 32 символов (латиница и цифры).

   `.env` в git не попадает, у каждого свой.
5. Запустить Docker Desktop, затем в терминале:
   ```
   docker compose up -d db
   ```
6. Открыть `FishingBackendApplication.java` → ▶ слева от `main` → Run.
7. Проверить: http://localhost:8080/actuator/health → `{"status":"UP", ...}`.
   Swagger UI: http://localhost:8080/swagger-ui/index.html

При старте Flyway сам создаёт все таблицы из `src/main/resources/db/migration`. Руками в базе ничего не создаём.

## 3. Тесты

```
.\mvnw.cmd test        # Windows
./mvnw test            # Linux / macOS
```
Тесты сами поднимают временную базу PostGIS в Docker (Testcontainers), поэтому Docker Desktop должен быть запущен.
`.env` и `docker compose` для тестов не нужны.

## 4. Частые проблемы

| Ошибка | Причина | Что сделать |
|---|---|---|
| `JDK isn't specified for module` | в IDEA не выбран JDK | шаг 2 выше |
| `ports are not available ... 5432` | порт занят локально установленным PostgreSQL | `services.msc` → служба `postgresql-x64-…` → Остановить, тип запуска «Вручную» |
| `Port 8080 was already in use` | осталось запущенное приложение | `netstat -ano \| findstr :8080` → `taskkill /PID <номер> /F` |
| `Could not find a valid Docker environment` | не запущен Docker Desktop | запустить его и дождаться «Engine running» |
| при старте ошибка подключения к БД (`jdbcUrl`, `password authentication failed`) | нет `.env`, не заполнен пароль или база не запущена | шаги 4–5 выше |

Если база создалась до того, как появился `.env`, пересоздать её: `docker compose down -v`, затем `docker compose up -d db`.

## 5. Как работаем с git

- `master` защищён: изменения только через Pull Request.
- Одна задача — одна ветка от свежего `master`:
  ```
  git checkout master
  git pull
  git checkout -b feature/<что-делаю>
  ```
- Коммиты в формате `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`.
- Перед PR: `.\mvnw.cmd test` проходит.
- **Миграции:** применённые файлы `V<N>__*.sql` не редактируются, изменения схемы — только новым файлом.
  Перед созданием миграции проверьте открытые PR, чтобы номера `V<N>` не совпали.
- Схему БД и контракт API меняем только по договорённости обоих разработчиков (CLAUDE.md, разделы 5 и 6).
