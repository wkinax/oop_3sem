import java.io.BufferedReader;                    // Для эффективного чтения текста
import java.io.InputStreamReader;                 // Для чтения байтовых потоков как текста
import java.io.IOException;                       // Для обработки ошибок ввода-вывода
import java.net.HttpURLConnection;                // Для HTTP-соединений
import java.net.URLEncoder;                       // Для кодирования URL (замены пробелов на %20 и т.д.)
import java.net.URL;                              // Для работы с web-адресами
import java.net.URI;                              // Для создания URI (нужен для открытия браузера)
import java.awt.Desktop;                          // Для открытия браузера
import java.util.Scanner;                         // Для чтения ввода пользователя с консоли
import com.google.gson.JsonArray;                 // Библиотека Gson - для работы с JSON массивами
import com.google.gson.JsonObject;                // Gson - для работы с JSON объектами
import com.google.gson.JsonParser;                // Gson - для разбора JSON строк


public class WikiSearch {
    private static JsonArray currentSearchResults = null;  // Хранит результаты текущего поиска
    private static String currentQuery = "";      // Хранит текущий поисковый запрос

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); // Создаём сканер для чтения ввода пользователя

        System.out.println("=== Поиск в Википедии ===");

        while (true) {                            // Бесконечный цикл - программа работает пока пользователь не выйдет
            try {
                if (currentSearchResults == null) {  // Если нет результатов поиска
                    performNewSearch(scanner);    // Выполняем новый поиск
                } else {
                    displayMainMenu(scanner);     // Иначе показываем главное меню
                }
            } catch (Exception e) {               // Ловим любые ошибки
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private static void performNewSearch(Scanner scanner) throws IOException {
        System.out.print("Введите поисковый запрос: ");
        String query = scanner.nextLine().trim();

        if (query.isEmpty()) {
            System.out.println("Пустой запрос. Попробуйте снова.");
            return;
        }

        currentQuery = query;                     // Сохраняем текущий запрос
        String encodedQuery = URLEncoder.encode(query, "UTF-8");  // Кодируем запрос для URL

        // Формируем URL для запроса к API Википедии
        String apiUrl = "https://ru.wikipedia.org/w/api.php?" +
                "action=query&list=search&srsearch=" + encodedQuery +  // Параметры API //хотим выполнить запрос, хотим выполнить поиск, передача поискового запроса
                "&srlimit=10&utf8=&format=json";

        URL url = new URL(apiUrl);                // Создаём объект URL из строки
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();  // Открываем соединение
        conn.setRequestMethod("GET");             // Устанавливаем метод GET
        conn.setRequestProperty("User-Agent", "WikiSearchApp/1.0 (https://example.com/; example123@gmail.com)");  // Устанавливаем заголовок User-Agent

        // Читаем ответ от сервера
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();  // Создаём StringBuilder для накопления ответа
        String line;
        while ((line = in.readLine()) != null) {  // Читаем построчно пока не кончится ответ
            response.append(line);                // Добавляем каждую строку в response
        }
        in.close();

        String json = response.toString();       // Преобразуем StringBuilder в строку
        JsonObject jsonResponse = JsonParser.parseString(json).getAsJsonObject();  // Парсим JSON
        JsonObject queryObj = jsonResponse.getAsJsonObject("query");  // Получаем объект "query"
        currentSearchResults = queryObj.getAsJsonArray("search");  // Получаем массив результатов поиска


        if (currentSearchResults.size() == 0) {
            System.out.println("Ничего не найдено. Попробуйте другой запрос.");
            currentSearchResults = null;
            return;
        }

        displaySearchResults();
    }

    private static void displaySearchResults() {
        System.out.println("\n=== Результаты поиска: '" + currentQuery + "' ===");
        for (int i = 0; i < currentSearchResults.size(); i++) {
            JsonObject article = currentSearchResults.get(i).getAsJsonObject();  // Получаем статью
            String title = article.get("title").getAsString();
            System.out.println((i + 1) + ". " + title);
        }
    }

    private static void displayMainMenu(Scanner scanner) throws IOException {
        System.out.println("\n=== Главное меню ===");
        System.out.println("1. Выбрать статью из текущего списка");
        System.out.println("2. Новый поиск");
        System.out.println("3. Завершить работу");
        System.out.print("Выберите действие (1-3): ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                selectArticleFromCurrentList(scanner);        // Выбор статьи из списка
                break;
            case "2":
                currentSearchResults = null;                  // Сбрасываем результаты
                currentQuery = "";                            // Сбрасываем запрос
                break;
            case "3":
                System.out.println("Выход из программы. До свидания!");
                scanner.close();
                System.exit(0);
                break;
            default:
                System.out.println("Неверный выбор. Пожалуйста, введите 1, 2 или 3.");
        }
    }

    private static void selectArticleFromCurrentList(Scanner scanner) throws IOException {
        if (currentSearchResults == null) return;

        System.out.println("\nСписок статей:");
        for (int i = 0; i < currentSearchResults.size(); i++) {  // Выводим все статьи
            JsonObject article = currentSearchResults.get(i).getAsJsonObject();
            String title = article.get("title").getAsString();
            System.out.println((i + 1) + ". " + title);
        }

        System.out.print("\nВведите номер статьи для открытия (1-" + currentSearchResults.size() + "): ");
        String input = scanner.nextLine().trim();

        try {
            int choice = Integer.parseInt(input);             // Пробуем преобразовать в число
            if (choice >= 1 && choice <= currentSearchResults.size()) {  // Проверяем диапазон
                JsonObject article = currentSearchResults.get(choice - 1).getAsJsonObject();  // Получаем выбранную статью
                String title = article.get("title").getAsString();      // Извлекаем заголовок
                int pageId = article.get("pageid").getAsInt();          // Извлекаем ID страницы

                // Формируем URL статьи используя pageId
                String articleUrl = "https://ru.wikipedia.org/w/index.php?curid=" + pageId;
                System.out.println("Открываю статью: " + title);        // Сообщаем какую статью открываем

                if (Desktop.isDesktopSupported()) {           // Проверяем поддерживается ли Desktop
                    Desktop.getDesktop().browse(new URI(articleUrl));  // Открываем браузер со статьёй
                    System.out.println("Статья открыта в браузере!");
                } else {
                    System.out.println("Открытие браузера не поддерживается.");
                }

                System.out.println("\nНажмите Enter для возврата в меню...");
                scanner.nextLine();
            } else {
                // Сообщение если номер вне диапазона
                System.out.println("Неверный номер статьи. Пожалуйста, введите число от 1 до " + currentSearchResults.size());
            }
        } catch (NumberFormatException e) {
            System.out.println("Пожалуйста, введите корректный номер.");
        } catch (Exception e) {
            System.out.println("Ошибка при открытии статьи: " + e.getMessage());
        }
    }
}