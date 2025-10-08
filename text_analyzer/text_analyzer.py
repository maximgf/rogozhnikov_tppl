import collections
import sys

def analyze_file(file_path: str) -> dict:
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        line_count = len(lines)

        full_text = "".join(lines)
        char_count = len(full_text)

        empty_line_count = 0
        for line in lines:
            if not line.strip(): 
                empty_line_count += 1

        char_freq = collections.Counter(full_text)

        return {
            'line_count': line_count,
            'char_count': char_count,
            'empty_line_count': empty_line_count,
            'char_freq': char_freq
        }

    except FileNotFoundError:
        print(f"Файл '{file_path}' не найден.", file=sys.stderr)
        return None
    except Exception as e:
        print(f"Непредвиденная ошибка: {e}", file=sys.stderr)
        return None

def display_results(analysis_data: dict, choices: set):
    if 1 in choices:
        print(f"Количество строк: {analysis_data['line_count']}")

    if 2 in choices:
        print(f"Количество символов: {analysis_data['char_count']}")

    if 3 in choices:
        print(f"Количество пустых строк: {analysis_data['empty_line_count']}")

    if 4 in choices:
        print("Частотный словарь символов (символ: количество):")
        sorted_freq = sorted(
            analysis_data['char_freq'].items(),
            key=lambda item: item[1],
            reverse=True
        )
        for char, count in sorted_freq:
            if char == '\n':
                display_char = '\\n'
            elif char == '\t':
                display_char = '\\t'
            elif char == ' ':
                display_char = "' '"
            else:
                display_char = char
            print(f"  {display_char:<4} -> {count}")

def main():
    # 1. Получаем имя файла от пользователя
    file_name = input("Введите имя текстового файла: ")

    # 2. Анализируем файл
    analysis_results = analyze_file(file_name)

    # Если при анализе произошла ошибка
    if analysis_results is None:
        return

    # 3. Предлагаем пользователю выбрать, что выводить
    print("1. Количество строк")
    print("2. Количество символов")
    print("3. Количество пустых строк")
    print("4. Частотный словарь символов")

    # Получаем выбор пользователя
    user_input = input("Введите номера желаемых пунктов через пробел или запятую (например, 1 3 4): ")

    try:
        cleaned_input = user_input.replace(',', ' ').split()
        selected_options = {int(choice) for choice in cleaned_input if choice.isdigit()}
    except (ValueError, TypeError):
        print("Некорректный ввод. Пожалуйста, вводите только числа.", file=sys.stderr)
        return

    if not selected_options:
        print("Ничего не выбрано для вывода.")
        return

    display_results(analysis_results, selected_options)


if __name__ == "__main__":
    main()