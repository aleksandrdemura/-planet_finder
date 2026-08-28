## Карта звездного неба (поиск планет)

Многоязычное консольное приложение для определения текущего положения планет Солнечной системы на небесной сфере.  
Вычисляет экваториальные координаты (прямое восхождение и склонение) для заданной даты и отображает их в удобочитаемом виде.

## Особенности
- Расчёт геоцентрических экваториальных координат (RA/Dec) для всех восьми планет.
- Поддержка произвольной даты (по умолчанию — текущий момент).
- Вывод списка всех планет с координатами и созвездием (упрощённо).
- Поиск конкретной планеты по имени.
- Цветное отображение в терминале (где поддерживается).
- Экспорт результатов в JSON и CSV.
- Использование упрощённых астрономических формул (круговые орбиты, средние элементы) — позиции приблизительные, достаточны для образовательных целей.

## Установка и запуск
Для каждого языка требуются соответствующие инструменты и зависимости.

### Запуск на разных языках

1. **Python**  
   Запуск: `python planet_finder.py --date 2026-08-28 --planet Mars`

2. **JavaScript (Node.js)**  
   Установка: `npm install commander chalk`  
   Запуск: `node planet_finder.js --date 2026-08-28 --planet Mars`

3. **Go**  
   Запуск: `go run planet_finder.go --date 2026-08-28 --planet Mars`

4. **Rust**  
   Сборка: `cargo build --release`  
   Запуск: `cargo run -- --date 2026-08-28 --planet Mars`

5. **Java**  
   Сборка: `javac -cp gson.jar PlanetFinder.java`  
   Запуск: `java -cp .;gson.jar PlanetFinder --date 2026-08-28 --planet Mars`

6. **C# (.NET Core)**  
   Установка: `dotnet add package Newtonsoft.Json`  
   Запуск: `dotnet run -- --date 2026-08-28 --planet Mars`

7. **C++ (Linux)**  
   Сборка: `g++ -std=c++11 -o planet_finder planet_finder.cpp -ljsoncpp`  
   Запуск: `./planet_finder --date 2026-08-28 --planet Mars`

8. **Kotlin (JVM)**  
   Сборка: `kotlinc -cp gson.jar PlanetFinder.kt`  
   Запуск: `kotlin -cp .;gson.jar PlanetFinderKt --date 2026-08-28 --planet Mars`

## Использование

Общие аргументы командной строки:

- `--date <YYYY-MM-DD>` – дата для расчёта (по умолчанию сегодня).
- `--planet <имя>` – показать только указанную планету.
- `--list` – показать все планеты.
- `--export-json <файл>` – экспортировать результаты в JSON.
- `--export-csv <файл>` – экспортировать результаты в CSV.
- `--help` – справка.

Пример (Python):
```bash
python planet_finder.py --date 2026-08-28 --list
Структура репозитория
text
/
├── README.md
├── planet_finder.py
├── planet_finder.js
├── planet_finder.go
├── planet_finder.rs
├── PlanetFinder.java
├── PlanetFinder.cs
├── planet_finder.cpp
└── PlanetFinder.kt
Лицензия
MIT
