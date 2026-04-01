#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# --------------------------------------------------------------
# replace_gpl_header.py
#
# Рекурсивно заменяет/добавляет GPL‑v3‑заголовок во всех
# исходных файлах проекта.
#
#   usage:  python replace_gpl_header.py [path]
#   default path = текущий каталог
#
# Автор:   Федотов Владислав Игоревич (niktoonion)
# Год:     берётся автоматически (по системному времени)
# --------------------------------------------------------------

import argparse
import os
import re
import sys
from datetime import datetime

# --------------------------------------------------------------
# 1️⃣  На какие файлы «смотрим». Добавляйте расширения, если нужно.
# --------------------------------------------------------------
SRC_EXT = {
    ".c", ".cpp", ".cc", ".cxx", ".h", ".hpp",
    ".java", ".js", ".ts", ".php"
}

def is_source_file(path: str) -> bool:
    """True, если расширение файла присутствует в SRC_EXT (регистронезависимо)."""
    _, ext = os.path.splitext(path)
    return ext.lower() in SRC_EXT


# --------------------------------------------------------------
# 2️⃣  Формируем текст нового GPL‑v3‑заголовка.
# --------------------------------------------------------------
def make_header(year: int) -> str:
    """
    Возвращает готовый заголовок в виде строки.
    Пример (на русском, как в C‑варианте):
    /*
     * Copyright (C) 2026 Fedotov Vladislav Igorevich (niktoonion)
     *
     * This program is free software: you can redistribute it and/or modify it
     * under the terms of the GNU General Public License as published by the
     * Free Software Foundation, either version 3 of the License, or (at your
     * option) any later version.
     *
     * This program is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     * GNU General Public License for more details.
     *
     * You should have received a copy of the GNU General Public License
     * along with this program.  If not, see https://www.gnu.org/licenses/.
     *
     * SPDX-License-Identifier: GPL-3.0-or-later
     */
    """
    return f"""/*
 * Copyright (C) {year} Fedotov Vladislav Igorevich (niktoonion)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
"""


# --------------------------------------------------------------
# 3️⃣  Основная логика обработки отдельного файла.
# --------------------------------------------------------------
def process_file(path: str, header: str) -> None:
    """
    Читает файл, удаляет найденный старый блок лицензии (если он есть)
    и вставляет `header`. Файл перезаписывается в‑жe.
    """
    # 1️⃣ читаем полностью (error‑tolerant)
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            content = f.read()
    except OSError as e:
        print(f"[ERR] can't read {path}: {e}", file=sys.stderr)
        return

    original_content = content  # для сравнения в конце

    # ----------------------------------------------------------
    # 2️⃣  Выделяем возможный shebang (#!)‑строку.
    # ----------------------------------------------------------
    shebang = ""
    rest = content
    if content.startswith("#!"):
        nl = content.find("\n")
        if nl != -1:
            shebang = content[: nl + 1]    # включаем \n
            rest = content[nl + 1 :]

    # ----------------------------------------------------------
    # 3️⃣  Пропускаем начальные пробельные символы (spaces/tabs/newlines)
    # ----------------------------------------------------------
    leading_ws_match = re.match(r"[ \t\r\n]*", rest)
    leading_ws = leading_ws_match.group(0)
    rest_body = rest[len(leading_ws) :]

    # ----------------------------------------------------------
    # 4️⃣  Ищем старый блок комментария /* … */ в начале файла.
    #    Если найден и содержит слова "copyright" или "license"
    #    – считаем его «старым» и отбрасываем.
    # ----------------------------------------------------------
    old_block_removed = False
    if rest_body.startswith("/*"):
        end_pos = rest_body.find("*/")
        if end_pos != -1:
            possible_block = rest_body[: end_pos + 2]          # включаем */
            if re.search(r"copyright|license", possible_block, re.I):
                # Удаляем старый блок полностью
                rest_body = rest_body[end_pos + 2 :]
                # Убираем пробелы/пустые строки, оставшиеся сразу после блока
                rest_body = rest_body.lstrip("\r\n")
                old_block_removed = True

    # ----------------------------------------------------------
    # 5️⃣  Собираем новый файл:
    #     shebang + (опциональные) leading_ws + header + "\n" + оставшееся тело
    # ----------------------------------------------------------
    new_content = f"{shebang}{leading_ws}{header}\n{rest_body}"

    # ----------------------------------------------------------
    # 6️⃣  Записываем только если действительно изменилось.
    # ----------------------------------------------------------
    if new_content != original_content:
        try:
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(new_content)
            action = "replaced" if old_block_removed else "added"
            print(f"[OK] {action:8} → {path}")
        except OSError as e:
            print(f"[ERR] can't write {path}: {e}", file=sys.stderr)


# --------------------------------------------------------------
# 7️⃣  Рекурсивный обход каталога и вызов process_file() для подходящих.
# --------------------------------------------------------------
def walk_and_process(root: str, header: str) -> None:
    for dirpath, dirnames, filenames in os.walk(root):
        # Не спускаемся в .git (и любые скрытые каталоги, начинающиеся с .)
        dirnames[:] = [d for d in dirnames if not d.startswith(".")]

        for name in filenames:
            full_path = os.path.join(dirpath, name)
            if is_source_file(full_path):
                process_file(full_path, header)


# --------------------------------------------------------------
# 8️⃣  Точка входа – парсим аргументы и стартуем.
# --------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Replace (or add) a GPL‑v3 header in source files."
    )
    parser.add_argument(
        "path",
        nargs="?",
        default=".",
        help="Root directory to process (default: current directory)",
    )
    args = parser.parse_args()

    root = os.path.abspath(args.path)
    if not os.path.isdir(root):
        print(f"[ERR] {root} is not a directory", file=sys.stderr)
        sys.exit(1)

    year = datetime.now().year
    header = make_header(year)

    print(f"[INFO] processing {root} with GPL‑v3 header ({year})")
    walk_and_process(root, header)
    print("[INFO] done.")


if __name__ == "__main__":
    main()
