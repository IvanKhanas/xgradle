#!/bin/bash

# Создаем папку tmp, если она еще не существует
mkdir -p tmp

# Находим все .java файлы, исключая папку buildExamples, и копируем их в tmp
find . -name "*.java" -not -path "./buildExamples/*" -exec cp {} tmp \;

echo "Все .java файлы (кроме из buildExamples) скопированы в папку tmp"
