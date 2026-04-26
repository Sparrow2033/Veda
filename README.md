# Veda

Android-приложение для учебных заметок, связей между ними (Zettelkasten) и визуализации графа знаний.

## Developer-ready quick start

### Требования
- Android Studio Narwhal 2025.1.1+.
- JDK 17 (Gradle JDK).
- Android SDK Platform 36.

### Запуск (CLI)
```bash
./gradlew clean assembleDebug
./gradlew test
./gradlew lintDebug
```

### Запуск (Android Studio)
1. Открыть проект.
2. Убедиться, что `Gradle JDK = 17`.
3. Выполнить `Sync Project with Gradle Files`.
4. Запустить `app` в конфигурации `debug`.

## Архитектура

Стек: **Java + Android SDK (XML/ViewBinding) + Room + LiveData/ViewModel + Repository**.

```text
UI (Activities + XML)
  -> ViewModel (state + UI сценарии)
    -> VedaRepository (единый API данных)
      -> Room (DAO + Entity + migrations)
```

### Контракт слоев
1. `Activity` — только UI/навигация/рендер.
2. `ViewModel` — состояние экрана, трансформации, вызовы репозитория.
3. `Repository` — единая точка чтения/записи данных.
4. Операции записи и sync-чтения выполняются через `DbExecutors`.

## Модули

- `ui.home` — стартовый экран.
- `ui.main` / `ui.edit` / `ui.view` — заметки (список, редактирование, просмотр).
- `ui.graph` — граф знаний (WebView + assets/graph).
- `ui.homework` — домашние задания.
- `ui.schedule` — расписание.
- `ui.settings` — пользовательские настройки.
- `data.db`, `data.dao`, `data.entity`, `data.repo` — data layer.
- `demo` — импорт демо-библиотеки.

## Release / internal publishing baseline

Для `release`-сборки включены:
- R8 minification (`isMinifyEnabled = true`),
- shrink resources (`isShrinkResources = true`),
- запрет debug-флагов,
- отдельные ProGuard-правила для Room и WebView JS bridge.

Собрать release APK:
```bash
./gradlew assembleRelease
```

## Known issues (MVP baseline)

1. Граф знаний может деградировать по UX/производительности на больших наборах данных.
2. Демо-импорт требует дополнительной проверки на крайних сценариях.
3. Нужна расширенная регрессия сценариев удаления и каскадных эффектов.
4. Release-подпись для production не настроена (только baseline для внутренней публикации).

## Roadmap

### Фаза 1 (post-MVP hardening)
- Повысить наблюдаемость ошибок (crash/telemetry).
- Добавить E2E smoke-покрытие критических пользовательских потоков.
- Профилировать и оптимизировать рендер графа.

### Фаза 2 (product improvements)
- Расширить фильтрацию/группировку домашних заданий.
- Улучшить onboarding и демо-данные.
- Добавить экспорт/backup пользовательских данных.

### Фаза 3 (scalability)
- Подготовить roadmap миграции на более модульную структуру.
- Подготовить безопасный релизный pipeline (signing + CI gating).

## MVP freeze

Базовая точка MVP зафиксирована git-тегом:
- `mvp-baseline-2026-04-26`

Отдельный backlog post-MVP: `docs/process/post-mvp-improvements.md`.
