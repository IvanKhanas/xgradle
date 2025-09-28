#!/bin/bash

# Создаем временный файл для объединенного содержимого
temp_file=$(mktemp) || exit 1

# Находим все .java файлы и обрабатываем их
while IFS= read -r -d '' file; do
    echo "//=== $(basename "$file") ===" >> "$temp_file"
    cat "$file" >> "$temp_file"
    echo -e "\n" >> "$temp_file"
done < <(find . -type f -name "*.java" -print0)

# Копируем содержимое в буфер обмен
xclip -selection clipboard < "$temp_file" && 
echo "Содержимое .java файлов скопировано в буфер обмена"

# Удаляем временный файл
rm -f "$temp_file"
