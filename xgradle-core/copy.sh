#!/bin/bash

# Проверяем, установлен ли xclip
if ! command -v xclip &> /dev/null; then
    echo "Ошибка: xclip не установлен. Установите его командой:"
    echo "sudo apt-get install xclip"  # для Debian/Ubuntu
    echo "или"
    echo "sudo yum install xclip"      # для RedHat/CentOS
    exit 1
fi

# Находим все .java файлы и копируем их содержимое в буфер обмена
find . -name "*.java" -exec cat {} + | xclip -selection clipboard

# Подсчитываем количество скопированных файлов
file_count=$(find . -name "*.java" | wc -l)

echo "Содержимое $file_count .java файлов скопировано в буфер обмена"
