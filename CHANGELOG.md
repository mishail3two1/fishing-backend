# Изменения для команды

Сюда записываем изменения, после которых другому участнику нужно что-то сделать или знать:
контракт API, общий код, настройки окружения. Новые записи сверху. Подробности — в PR по ссылке.

## 2026-11-09 — рыба на карточке водоёма (PR #18)

**Фронтенду**
- В ответе `GET /api/v1/water-bodies/{id}` новое поле `fish` — массив видов, которые водятся в водоёме:
  `[{"speciesId": 2, "nameRu": "Окунь", "isProtected": false, "abundance": "COMMON"}, ...]`.
  Отсортирован по `nameRu` по правилам русского алфавита.
- `abundance`: `RARE`, `COMMON`, `ABUNDANT` или `null` (неизвестно). `isProtected: true` — вид охраняемый, вылов запрещён.
- Если данных о рыбе нет, `fish` — пустой массив `[]`, не `null`. Подробности о виде — в `GET /api/v1/fish-species` по `speciesId`.

**Бэкенду**
- `WaterBodyEntity.getFish()` — список `WaterBodyFish` (`@ElementCollection` по таблице `water_body_fish`), загружается лениво:
  обращаться к нему только внутри `@Transactional` (у нас `open-in-view: false`).

## 2026-11-08 — справочник рыб (PR #17)

**Фронтенду**
- `GET /api/v1/fish-species` — справочник видов рыб, открыт без входа. Список целиком, без пагинации,
  отсортирован по `nameRu`.
- Поля: `id`, `nameRu`, `nameLat`, `minCatchSizeCm` (число или `null`, если ограничения нет),
  `isProtected` (`true` — охраняемый вид, вылов запрещён), `releaseAdvice` (может быть `null`).

**Бэкенду**
- `speciesId` в улове — это `id` из этого справочника. Сущность `fish/FishSpeciesEntity` (только чтение).
- Справочник в базе пока пустой: как его наполнять, решим вместе с импортом водоёмов (в миграции данные не зашиваем).

## 2026-11-01 — вход и JWT (PR #16)

**Фронтенду**
- `POST /api/v1/auth/login`, тело `{"email": "...", "password": "..."}` → `200` и `{"accessToken": "...", "user": {"id", "email", "displayName"}}`.
  Неверный email или пароль → `401` с одним текстом «Неверный email или пароль» в обоих случаях.
- **Изменение:** `POST /api/v1/auth/register` теперь возвращает тот же формат `{"accessToken", "user"}`, а не только профиль.
- Токен отправлять в каждом запросе к закрытым адресам: `Authorization: Bearer <accessToken>`.
  Токен живёт 7 дней; refresh-токенов пока нет — после истечения нужен повторный вход.
- `GET /api/v1/users/me` — профиль текущего пользователя по токену.
- Без токена или с недействительным токеном → `401` ProblemDetail «Требуется вход» и заголовок `WWW-Authenticate: Bearer`.
- **Важно:** просроченный или подделанный токен даёт `401` даже на открытых адресах (карта, карточка водоёма).
  При `401` удалите сохранённый токен и повторите запрос без него или отправьте пользователя на вход.

**Бэкенду**
- `id` текущего пользователя в контроллере вместо `@RequestParam UUID userId` (пример — `UserController.me`):
  ```java
  public X method(@AuthenticationPrincipal Jwt jwt) {
      UUID userId = UUID.fromString(jwt.getSubject());
        //....
  }
  ```
- Закрытые эндпоинты помечайте `@SecurityRequirement(name = "bearerAuth")`: в Swagger у них появится замок,
  а кнопка **Authorize** позволит вставить токен.
- В MockMvc-тестах, где контроллер читает `@AuthenticationPrincipal Jwt`, `@WithMockUser` не подойдёт (`jwt` будет `null`).
  Нужен настоящий токен (пример — `UserControllerTest.registerAndGetToken`) или
  `.with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))` из `spring-security-test`.
- В `.env` нужен `JWT_SECRET` не короче 32 символов, иначе приложение не стартует. Тесты берут свой секрет из
  `src/test/resources/application.properties`, `.env` им не нужен.

## 2026-11-01 — Spring Security и регистрация (PR #15)

**Фронтенду**
- `POST /api/v1/auth/register`, тело `{"email": "...", "password": "...", "displayName": "..."}`
  → `201` и профиль `{"id": "<uuid>", "email": "...", "displayName": "..."}`. Email возвращается в нижнем регистре.
  JWT пока не выдаётся: он появится в следующем PR вместе со входом `POST /api/v1/auth/login`.
- Ограничения: email — корректный адрес до 255 символов; пароль 8–72 символа; `displayName` не пустой, до 100 символов.
  Нарушение → `400` ProblemDetail (общий текст, без указания поля).
- Email уже зарегистрирован (регистр букв не важен) → `409` ProblemDetail.

**Всем**
- Подключён Spring Security. Без входа доступны только: `POST /api/v1/auth/**`, `GET /api/v1/water-bodies`,
  `GET /api/v1/water-bodies/{id}`, Swagger, `/actuator/health`. Остальные адреса отвечают `403`,
  пока не появится JWT (следующий PR, тогда ответ для анонима станет `401`).

**Бэкенду**
- Эндпоинты уловов `/api/v1/catches` после слияния закрыты для анонима. Новый открытый эндпоинт нужно явно
  добавить в `SecurityConfig.securityFilterChain`.
- В MockMvc-тестах закрытых эндпоинтов добавляйте `@WithMockUser` (зависимость `spring-boot-starter-security-test`
  уже подключена), пример — `ErrorHandlingTest`.

## 2026-10-26 — карточка водоёма (PR #14)

**Фронтенду**
- Новый эндпоинт `GET /api/v1/water-bodies/{id}` — карточка опубликованного водоёма. Пример ответа:
  ```json
  {
    "id": 1, "name": "Озеро Смолино", "waterType": "LAKE", "lat": 55.13, "lon": 61.36,
    "description": "Озеро в черте города", "district": "Челябинск",
    "accessType": "FREE", "accessNotes": "Бесплатно",
    "hasParking": true, "parkingNotes": "У пляжа",
    "rulesNotes": "Весной с лодок нельзя", "verifiedAt": "2026-10-26T05:33:25.536676Z"
  }
  ```
- `accessType`: `FREE`, `PAID`, `PERMIT`, `UNKNOWN`. `hasParking` может быть `null` — «неизвестно»,
  это не то же самое, что `false`. Текстовые поля и `verifiedAt` тоже могут быть `null`.
  `verifiedAt` — ISO-8601 в UTC, доли секунды могут быть, а могут отсутствовать.
- Водоём не найден или не опубликован → `404` ProblemDetail; нечисловой `id` → `400`.
- Рыба, последние уловы и погода появятся в карточке позже, новыми полями.
  
## 2026-10-25 — список водоёмов по области карты (PR #12)

**Фронтенду**
- `GET /api/v1/water-bodies` требует параметр `bbox=minLon,minLat,maxLon,maxLat`:
  сначала долгота, градусы WGS 84. Пример: `bbox=61.0,54.9,61.8,55.4`.
- Без `bbox` или с некорректным значением ответ `400` в формате ProblemDetail
  (`application/problem+json`), текст ошибки в поле `detail`.

**Бэкенду**
- `WaterBodyRepository.findAllByIsPublishedTrue` удалён; водоёмы для карты — `findPublishedInBbox`.

## 2026-10-22 — CORS (PR #11)

**Фронтенду**
- Браузерные запросы к `/api/**` разрешены только с origin из `CORS_ALLOWED_ORIGIN`
  (по умолчанию `http://localhost:5173`). Если dev-сервер фронтенда на другом порту,
  этот адрес нужно прописать в `.env` бэкенда.
- Разрешены методы `GET, POST, PUT, PATCH, DELETE` и заголовки `Content-Type`, `Authorization`.
