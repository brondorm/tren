#!/usr/bin/env python3
"""
Парсер MD-файлов тренировок → JSON
Формат входных данных:
    ### YYYY-MM-DD
    1. Название упражнения
        1. ВЕС на ПОВТОРЕНИЯ (комментарий)
        2. На ПОВТОРЕНИЯ
"""

import re
import json
import sys
from pathlib import Path
from dataclasses import dataclass, asdict
from typing import Optional


@dataclass
class Set:
    set_number: int
    weight: float
    reps: int
    note: Optional[str] = None


@dataclass
class ExerciseEntry:
    exercise: str
    sets: list[Set]


@dataclass
class Workout:
    date: str
    entries: list[ExerciseEntry]


# Базовый маппинг упражнений на группы мышц
DEFAULT_EXERCISE_MAPPING = {
    # Грудь
    "жим": "Грудь",
    "жис": "Грудь",
    "жим в наклоне": "Грудь",
    "жим гантелей в наклоне": "Грудь",
    "брусья": "Грудь",
    "жим 1 рукой": "Грудь",
    
    # Спина - широчайшие
    "подтягивания": "Широчайшие",
    "сидя на спину": "Широчайшие",
    "спина сидя": "Широчайшие",
    "тяга вертикальная": "Широчайшие",
    "сидя вертикально спина": "Широчайшие",
    "тяга с колена": "Широчайшие",
    "тяга одной": "Широчайшие",
    "спина": "Широчайшие",
    
    # Плечи
    "плечи": "Средняя дельта",
    "плечи стоя": "Средняя дельта",
    "махи": "Средняя дельта",
    "рассомаха": "Задняя дельта",
    
    # Ноги
    "ноги": "Квадрицепс",
    "сидя на ноги": "Квадрицепс",
    "ноги сидя": "Квадрицепс",
    "лежа на ноги": "Бицепс бедра",
    "ноги лежа": "Бицепс бедра",
    "на ноги": "Квадрицепс",
    "лежа": "Бицепс бедра",
    
    # Бицепс
    "битка": "Бицепс",
    "бицуха": "Бицепс",
    "битка сидя и стоя": "Бицепс",
    "битка стоя и сидя": "Бицепс",
    "стоя на битку": "Бицепс",
    "битка лежа": "Бицепс",
    "одной рукой": "Бицепс",
    
    # Трицепс
    "трицепс на руку": "Трицепс",
    "трицепст на руку": "Трицепс",
    "рука стоя триц": "Трицепс",
    "рука и трицепс": "Трицепс",
}


def parse_weight(weight_str: str) -> float:
    """
    Парсит вес из строки.
    '49 45' → 49 + 4.5 = 53.5
    '49 68' → 49 + 6.8 = 55.8
    '80' → 80.0
    """
    parts = weight_str.strip().split()
    if len(parts) == 1:
        return float(parts[0])
    elif len(parts) == 2:
        main_weight = float(parts[0])
        # Второе число — это десятичная часть (45 → 4.5, 68 → 6.8)
        additional = float(parts[1]) / 10
        return main_weight + additional
    else:
        # Пытаемся взять первые два числа
        return float(parts[0]) + float(parts[1]) / 10


def parse_set_line(line: str, prev_weight: float, set_number: int) -> tuple[Set, float]:
    """
    Парсит строку подхода.
    Возвращает (Set, текущий_вес) для передачи следующему подходу.
    
    Форматы:
    - "80 на 8" → вес 80, повторы 8
    - "49 45 на 16" → вес 53.5, повторы 16
    - "На 10" → вес из предыдущего, повторы 10
    - "10" → только повторы, вес из предыдущего
    """
    line = line.strip()
    
    # Извлекаем комментарий в скобках
    note = None
    note_match = re.search(r'\(([^)]+)\)', line)
    if note_match:
        note = note_match.group(1).strip()
        line = re.sub(r'\([^)]+\)', '', line).strip()
    
    # Убираем лишние символы
    line = line.rstrip('.')
    
    # Паттерн: "ВЕС на ПОВТОРЫ" или "На ПОВТОРЫ" или просто число
    
    # Пробуем "X на Y" или "X X на Y"
    match = re.match(r'^([\d\s]+?)\s*на\s*(\d+)', line, re.IGNORECASE)
    if match:
        weight = parse_weight(match.group(1))
        reps = int(match.group(2))
        return Set(set_number, weight, reps, note), weight
    
    # Пробуем "На Y"
    match = re.match(r'^на\s*(\d+)', line, re.IGNORECASE)
    if match:
        reps = int(match.group(1))
        return Set(set_number, prev_weight, reps, note), prev_weight
    
    # Пробуем просто число (повторения)
    match = re.match(r'^(\d+)$', line)
    if match:
        reps = int(match.group(1))
        return Set(set_number, prev_weight, reps, note), prev_weight
    
    # Не удалось распарсить
    return None, prev_weight


def parse_md_file(filepath: Path) -> list[Workout]:
    """Парсит MD-файл и возвращает список тренировок."""
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    workouts = []
    current_workout = None
    current_exercise = None
    current_weight = 0.0
    set_number = 0
    
    for line in content.split('\n'):
        line = line.rstrip()
        
        # Пропускаем пустые строки и ссылки [[...]]
        if not line or line.startswith('[[') or line.startswith(' [['):
            continue
        
        # Дата тренировки: ### YYYY-MM-DD
        date_match = re.match(r'^###\s*(\d{4}-\d{2}-\d{2})', line)
        if date_match:
            # Сохраняем предыдущую тренировку
            if current_workout:
                if current_exercise and current_exercise.sets:
                    current_workout.entries.append(current_exercise)
                if current_workout.entries:
                    workouts.append(current_workout)
            
            current_workout = Workout(date=date_match.group(1), entries=[])
            current_exercise = None
            current_weight = 0.0
            continue
        
        # Упражнение: "1. Название" или "2. Название"
        exercise_match = re.match(r'^\d+\.\s*(.+)', line)
        if exercise_match and not line.startswith('\t') and not line.startswith('    '):
            # Сохраняем предыдущее упражнение
            if current_exercise and current_exercise.sets:
                if current_workout:
                    current_workout.entries.append(current_exercise)
            
            exercise_name = exercise_match.group(1).strip()
            current_exercise = ExerciseEntry(exercise=exercise_name, sets=[])
            current_weight = 0.0
            set_number = 0
            continue
        
        # Подход: "\t1. ..." или "    1. ..."
        set_match = re.match(r'^[\t\s]+\d+\.\s*(.+)', line)
        if set_match and current_exercise is not None:
            set_number += 1
            set_data, current_weight = parse_set_line(
                set_match.group(1), 
                current_weight, 
                set_number
            )
            if set_data:
                current_exercise.sets.append(set_data)
            continue
    
    # Сохраняем последнюю тренировку
    if current_workout:
        if current_exercise and current_exercise.sets:
            current_workout.entries.append(current_exercise)
        if current_workout.entries:
            workouts.append(current_workout)
    
    return workouts


def get_muscle_group(exercise_name: str) -> Optional[str]:
    """Возвращает группу мышц для упражнения."""
    normalized = exercise_name.lower().strip()
    return DEFAULT_EXERCISE_MAPPING.get(normalized)


def build_exercises_list(workouts: list[Workout]) -> list[dict]:
    """Собирает список уникальных упражнений с маппингом."""
    exercises = {}
    
    for workout in workouts:
        for entry in workout.entries:
            name = entry.exercise
            if name not in exercises:
                muscle_group = get_muscle_group(name)
                exercises[name] = {
                    "name": name,
                    "muscle_group": muscle_group
                }
    
    return list(exercises.values())


def build_muscle_groups() -> list[dict]:
    """Возвращает структуру групп мышц."""
    return [
        {"id": 1, "name": "Грудь", "parent_id": None},
        {"id": 2, "name": "Спина", "parent_id": None},
        {"id": 3, "name": "Широчайшие", "parent_id": 2},
        {"id": 4, "name": "Трапеция", "parent_id": 2},
        {"id": 5, "name": "Плечи", "parent_id": None},
        {"id": 6, "name": "Передняя дельта", "parent_id": 5},
        {"id": 7, "name": "Средняя дельта", "parent_id": 5},
        {"id": 8, "name": "Задняя дельта", "parent_id": 5},
        {"id": 9, "name": "Ноги", "parent_id": None},
        {"id": 10, "name": "Квадрицепс", "parent_id": 9},
        {"id": 11, "name": "Бицепс бедра", "parent_id": 9},
        {"id": 12, "name": "Икры", "parent_id": 9},
        {"id": 13, "name": "Руки", "parent_id": None},
        {"id": 14, "name": "Бицепс", "parent_id": 13},
        {"id": 15, "name": "Трицепс", "parent_id": 13},
        {"id": 16, "name": "Предплечья", "parent_id": 13},
    ]


def workouts_to_dict(workouts: list[Workout]) -> list[dict]:
    """Конвертирует список тренировок в словари."""
    result = []
    for workout in workouts:
        w = {
            "date": workout.date,
            "entries": []
        }
        for entry in workout.entries:
            e = {
                "exercise": entry.exercise,
                "sets": [asdict(s) for s in entry.sets]
            }
            w["entries"].append(e)
        result.append(w)
    return result


def main():
    if len(sys.argv) < 2:
        print("Использование: python md_to_json.py <файл.md> [файл2.md ...]")
        print("Или: python md_to_json.py <папка>")
        sys.exit(1)
    
    all_workouts = []
    
    for arg in sys.argv[1:]:
        path = Path(arg)
        
        if path.is_dir():
            # Обрабатываем все MD-файлы в папке
            for md_file in path.glob("*.md"):
                print(f"Парсинг: {md_file}")
                workouts = parse_md_file(md_file)
                all_workouts.extend(workouts)
        elif path.is_file() and path.suffix == '.md':
            print(f"Парсинг: {path}")
            workouts = parse_md_file(path)
            all_workouts.extend(workouts)
        else:
            print(f"Пропуск: {path} (не MD-файл)")
    
    # Сортируем по дате
    all_workouts.sort(key=lambda w: w.date)
    
    # Собираем результат
    result = {
        "muscle_groups": build_muscle_groups(),
        "exercises": build_exercises_list(all_workouts),
        "workouts": workouts_to_dict(all_workouts)
    }
    
    # Выводим JSON
    output_path = Path("gym_data.json")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    
    print(f"\n✓ Сохранено в {output_path}")
    print(f"  Тренировок: {len(all_workouts)}")
    print(f"  Упражнений: {len(result['exercises'])}")
    
    # Показываем упражнения без маппинга
    unmapped = [e for e in result['exercises'] if e['muscle_group'] is None]
    if unmapped:
        print(f"\n⚠ Упражнения без привязки к группе мышц:")
        for e in unmapped:
            print(f"  - {e['name']}")


if __name__ == "__main__":
    main()
