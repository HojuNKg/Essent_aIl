# Вынос управления Глифами из Accessibility Service

Цель: Сделать управление Glyph-интерфейсом независимым от службы специальных возможностей, чтобы UI приложения мог управлять подсветкой в любое время.

## User Review Required

> [!IMPORTANT]
> Мы переносим жизненный цикл `GlyphManager` из `EssentialKeyService` в глобальный объект `GlyphController`. Это позволит управлять глифами из UI, даже если сервис кнопок выключен.

## Proposed Changes

### [Core Logic]

#### [NEW] [GlyphController.kt](file:///C:/Users/Hoju_/essent_all/app/src/main/java/com/HojuNK/essent_all/util/GlyphController.kt)
Создание нового компонента для инкапсуляции всей логики Nothing SDK.
- Управление `GlyphManager`.
- Хранение состояния `glyphActive`.
- Методы: `init`, `toggle`, `playPattern`, `stop`.

#### [MODIFY] [EssentialKeyService.kt](file:///C:/Users/Hoju_/essent_all/app/src/main/java/com/HojuNK/essent_all/EssentialKeyService.kt)
- Удаление внутреннего `GlyphManager` и связанных с ним методов.
- Изменение обработчиков действий (Action Handlers): теперь они будут вызывать методы `GlyphController`.
- Удаление `StateFlow` статуса глифов (теперь он в контроллере).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Hoju_/essent_all/app/src/main/java/com/HojuNK/essent_all/MainActivity.kt)
- Инициализация `GlyphController` в `onCreate`.
- Обновление ссылок на статус активности глифов (подписка на `GlyphController.glyphActive` вместо сервиса).

## Verification Plan

### Manual Verification
1. **Тест без сервиса**:
   - Выключить "Essent.aI" в настройках Спецвозможностей.
   - Зайти в приложение и попробовать включить глифы через кнопку в UI или запустить паттерн. Глифы должны работать.
2. **Тест с кнопкой**:
   - Включить сервис Спецвозможностей.
   - Назначить на Essential Key действие "Toggle Glyphs".
   - Нажать физическую кнопку — глифы должны включаться/выключаться.
3. **Проверка стабильности**:
   - Запустить длинный паттерн («бегунок») и выйти из приложения. Анимация не должна прерываться.
