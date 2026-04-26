# Post-MVP improvements backlog

Статус: draft backlog после заморозки MVP (`mvp-baseline-2026-04-26`).

## P0 — reliability & release safety
- [ ] Настроить release signing через защищенные CI secrets и отдельный keystore lifecycle.
- [ ] Включить crash reporting + structured logging для ключевых экранов.
- [ ] Добавить release checklist (версия, миграции, smoke test, rollback).

## P1 — quality & testing
- [ ] Добавить smoke-набор instrumentation-тестов для main/user flows.
- [ ] Добавить regression-набор для операций удаления (notes/links/homework).
- [ ] Включить lint baseline policy и обязательный `lintDebug` gate в CI.

## P1 — graph module hardening
- [ ] Профилировать graph payload и время рендера на 1k/5k узлах.
- [ ] Оптимизировать фильтрацию и локальный режим графа.
- [ ] Добавить graceful fallback при ошибках WebView/JS.

## P2 — UX improvements
- [ ] Расширить фильтры домашних заданий (комбинированные статусы/приоритеты/даты).
- [ ] Улучшить сценарии onboarding и демо-импорта.
- [ ] Добавить быстрые действия на Home для повторяющихся задач.

## P2 — data & portability
- [ ] Добавить экспорт/импорт пользовательских данных.
- [ ] Подготовить пользовательский backup/restore сценарий.
- [ ] Подготовить документ миграционной совместимости схемы БД.
