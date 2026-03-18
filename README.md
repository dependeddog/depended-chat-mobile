# Depended Chat Mobile (Android)

Production-ready MVP Android client for `depended-chat` backend.

## 1) Анализ и архитектурный план

- **Stack:** Kotlin, Gradle, Jetpack Compose, Navigation Compose, MVVM, Coroutines + StateFlow, Retrofit + OkHttp, OkHttp WebSocket, DataStore, Hilt, Material3.
- **Слои:**
  - `data` — API DTO, Retrofit сервисы, DataStore session storage, websocket manager, repository implementation.
  - `domain` — модели приложения и контракты репозиториев.
  - `ui` — экраны `splash/auth/chats/chat`, ViewModel для каждого потока, компоненты темы.
  - `navigation` — маршруты и graph.
  - `di` — Hilt-модули сети/репозиториев.
- **Auth/session flow:**
  - Access/refresh токены в DataStore.
  - Каждый REST-запрос получает `Authorization` через `AuthInterceptor`.
  - На 401 срабатывает `TokenAuthenticator`, выполняется `/auth/refresh`, запрос повторяется.
  - Logout только вручную (или при реально невалидном refresh) с очисткой DataStore + закрытием сокетов.
- **Realtime flow:**
  - Глобальный канал `/ws/events` для апдейтов списка чатов.
  - Канал чата `/ws/chats/{chat_id}` для входящих сообщений.

## 2) Структура проекта

```text
app/src/main/java/com/depended/chat
├── data
│   ├── auth
│   ├── local
│   ├── remote/api
│   ├── remote/dto
│   ├── repository
│   └── websocket
├── di
├── domain
│   ├── model
│   └── repository
├── navigation
└── ui
    ├── auth
    ├── chat
    ├── chats
    ├── splash
    └── theme
```

## 3) Backend base URL

В `app/build.gradle.kts`:
- `BASE_URL` для REST
- `WS_BASE_URL` для WebSocket

По умолчанию стоит `10.0.2.2` (эмулятор Android -> localhost хоста).

## 4) Персистентная сессия без лишнего logout

- При старте `SplashViewModel` вызывает `AuthRepository.refreshIfNeeded()`.
- Если access токен ещё жив или успешно обновился по refresh — переход на список чатов.
- На логин возвращаемся **только** если нет валидной сессии или refresh реально отклонён.

## 5) Статусы сообщений

- `SENT` — после успешного POST `/chats/{chat_id}/messages`.
- `DELIVERED` — при подтверждении через общий поток сообщений (включая websocket-эхо с тем же message id).
- `READ` — архитектура готова, базовая логика read связана с `POST /chats/{chat_id}/read`; можно усилить по `chat.read` event при расширении контракта в UI.

## 6) Запуск

1. Открыть проект в Android Studio (Giraffe+/Koala+).
2. Sync Gradle.
3. Убедиться, что backend запущен и доступен по URL из BuildConfig.
4. Запустить `app` на эмуляторе/устройстве.

## 7) Рекомендованные улучшения backend для ещё лучшего mobile UX

- Явный контракт typing start/stop (включая формат исходящих клиентских команд).
- Endpoint `/auth/me` для надёжного определения `my_user_id` и идеального выравнивания bubble/статусов.
- Явный `delivered_at`/`read_at` на уровне message entity.
- Cursor pagination для сообщений (lazy loading истории).
