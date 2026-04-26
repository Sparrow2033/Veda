# Pull Request policy

## Правило merge

Ветка `main` должна быть защищена через Branch protection / Ruleset с обязательной проверкой `merge-gate / required-ci`.

Если обязательная проверка не зелёная (failed/cancelled), merge запрещён.

## Что проверяет обязательный CI

Workflow `merge-gate` запускает:

- `:app:assembleDebug`
- `:app:testDebugUnitTest`
- `:app:lintDebug` (Android Lint)
- `:app:checkstyle` (статический анализ Java)

## Настройка в GitHub (однократно)

1. `Settings` → `Branches` (или `Rulesets`).
2. Добавить правило для `main`.
3. Включить `Require status checks to pass before merging`.
4. Отметить обязательным check `merge-gate / required-ci`.
5. Рекомендуется включить `Require branches to be up to date before merging`.
