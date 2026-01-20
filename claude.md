# GymTracker - Приложение для отслеживания тренировок

## Суть проекта

**GymTracker** - это Android-приложение для отслеживания тренировок в спортзале. Позволяет:
- Записывать тренировки с упражнениями, подходами и весами
- Отслеживать прогресс по упражнениям с графиками
- Анализировать статистику по группам мышц
- Вычислять максимум на 1 повторение (формула Эпли)
- Отмечать личные рекорды с анимацией
- Импортировать/экспортировать данные в JSON

## Технологический стек

- **Язык:** Kotlin
- **UI:** Jetpack Compose (Material Design 3)
- **База данных:** Room (SQLite)
- **Навигация:** Jetpack Navigation Compose
- **Графики:** Vico Library
- **Сериализация:** Gson
- **Минимальная версия:** Android 8.0 (API 26)

## Архитектура

**Паттерн:** MVVM + Repository Pattern

```
UI (Compose Screens)
    ↓
Navigation (Sealed Classes)
    ↓
Repository (GymRepository) - бизнес-логика
    ↓
DAO Layer (6 DAOs) - запросы к БД
    ↓
Room Database (5 таблиц)
```

## Структура проекта

```
android-app/app/src/main/java/com/gymtracker/
├── data/
│   ├── database/          # Room DAO, Database, Миграции
│   ├── model/             # Entities и JSON модели
│   └── repository/        # GymRepository
└── ui/
    ├── screens/           # 6 основных экранов
    ├── components/        # Переиспользуемые компоненты
    ├── theme/             # Material 3 тема
    ├── Navigation.kt      # Маршруты навигации
    └── MainActivity.kt    # Точка входа
```

## Основные экраны

1. **WorkoutsScreen** - список всех тренировок с датами
2. **EditWorkoutScreen** (614 строк) - добавление/редактирование упражнений и подходов
3. **ExercisesScreen** - управление справочником упражнений
4. **StatsScreen** - статистика по группам мышц (неделя/месяц/год)
5. **ProgressScreen** - графики прогресса веса по упражнениям
6. **SettingsScreen** - импорт/экспорт данных

## Ключевые файлы

### Данные
- **Entities.kt** - 5 сущностей БД: MuscleGroup, Exercise, Workout, WorkoutExercise, ExerciseSet
- **Daos.kt** - 6 DAO интерфейсов с запросами
- **GymDatabase.kt** (195 строк) - настройка БД, миграции, предзаполнение данных
- **GymRepository.kt** (450 строк) - вся бизнес-логика, расчеты, статистика

### UI
- **Navigation.kt** - sealed class с маршрутами для 7 экранов
- **MainActivity.kt** - инициализация БД, репозитория, Compose
- **RecordCelebration.kt** - анимация при установке личных рекордов

## База данных

**5 таблиц с связями:**

```
muscle_groups (группы мышц)
    ↓
exercises (упражнения) - ссылка на muscle_group + до 2 синергистов
    ↓
workouts (тренировки по датам)
    ↓
workout_exercises (упражнения в тренировке)
    ↓
sets (подходы с весом и повторениями)
```

**Предзаполненные данные:**
- 16 групп мышц (включая синергисты)
- 20+ упражнений

**Миграции:** активны миграции 1→2 и 2→3

## Ключевые особенности

### 1. Формула Эпли (1RM)
Вычисление максимального веса на 1 повторение:
```kotlin
1RM = вес × (1 + повторения / 30)
```

### 2. Логика "второй предыдущей тренировки"
Для PPL-сплита: загружает данные не из последней тренировки, а из предпоследней (чтобы чередовать группы мышц)

### 3. Синергисты
Каждое упражнение имеет:
- Основную группу мышц
- До 2 синергистов (вспомогательных мышц)

### 4. Обнаружение личных рекордов
Автоматически сравнивает 1RM текущего подхода с историческими данными и показывает анимацию при превышении

### 5. Импорт/Экспорт
Полный JSON экспорт всех данных для бэкапа и переноса

## Навигация

```kotlin
sealed class AppNavigation(val route: String) {
    object Workouts : AppNavigation("workouts")
    object EditWorkout : AppNavigation("editWorkout")
    object Exercises : AppNavigation("exercises")
    object Stats : AppNavigation("stats")
    object Progress : AppNavigation("progress")
    object Settings : AppNavigation("settings")
}
```

## Material Design 3

- Адаптивная светлая/темная тема
- Material Icons Extended
- Compose UI компоненты
- Современный дизайн с анимациями

## Статус проекта

- Ветка: `main`
- Последние коммиты: исправление багов скролла, добавление иконок групп мышц
- Чистое состояние Git (нет незакоммиченных изменений)

## Быстрый старт для работы с кодом

1. **Добавление нового экрана:**
   - Создать Composable в `ui/screens/`
   - Добавить маршрут в `Navigation.kt`
   - Добавить в NavHost

2. **Изменение схемы БД:**
   - Обновить Entity в `Entities.kt`
   - Создать миграцию в `GymDatabase.kt`
   - Обновить DAO если нужно

3. **Добавление бизнес-логики:**
   - Расширить `GymRepository.kt`
   - Добавить методы в соответствующий DAO

4. **UI компоненты:**
   - Добавлять переиспользуемые компоненты в `ui/components/`
   - Следовать Material 3 guidelines

## Известные паттерны кода

- Compose State Management через `remember` и `rememberCoroutineScope`
- Flow для реактивных обновлений из Room
- Coroutines для асинхронных операций
- Sealed classes для типобезопасной навигации
- Foreign keys с CASCADE DELETE для целостности данных
