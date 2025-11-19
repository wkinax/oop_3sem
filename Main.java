import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.net.URI;
import java.awt.Desktop;
import java.util.Scanner;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {
    private static WikiSearchResult currentSearch = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Поиск в Википедии ===");

        while (true) {
            try {
                if (currentSearch == null) {
                    performNewSearch(scanner);
                } else {
                    displayMainMenu(scanner);
                }
            } catch (Exception e) {
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

        Query searchQuery = new Query(query);
        currentSearch = searchQuery.execute();

        if (currentSearch.getResults().size() == 0) {
            System.out.println("Ничего не найдено. Попробуйте другой запрос.");
            currentSearch = null;
            return;
        }

        currentSearch.display();
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
                selectArticleFromCurrentList(scanner);
                break;
            case "2":
                currentSearch = null;
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
        if (currentSearch == null) return;

        System.out.println("\nСписок статей:");
        currentSearch.display();

        System.out.print("\nВведите номер статьи для открытия (1-" + currentSearch.getResults().size() + "): ");
        String input = scanner.nextLine().trim();

        try {
            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= currentSearch.getResults().size()) {
                SearchItems article = currentSearch.getResults().get(choice - 1);
                article.openInBrowser();

                System.out.println("\nНажмите Enter для возврата в меню...");
                scanner.nextLine();
            } else {
                System.out.println("Неверный номер статьи. Пожалуйста, введите число от 1 до " + currentSearch.getResults().size());
            }
        } catch (NumberFormatException e) {
            System.out.println("Пожалуйста, введите корректный номер.");
        } catch (Exception e) {
            System.out.println("Ошибка при открытии статьи: " + e.getMessage());
        }
    }
}

class Query {
    private String queryText;

    public Query(String queryText) {
        this.queryText = queryText;
    }

    public WikiSearchResult execute() throws IOException {
        String encodedQuery = URLEncoder.encode(queryText, "UTF-8");
        String apiUrl = "https://ru.wikipedia.org/w/api.php?" +
                "action=query&list=search&srsearch=" + encodedQuery +
                "&srlimit=10&utf8=&format=json";

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "WikiSearchApp/1.0");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        String json = response.toString();
        JsonObject jsonResponse = JsonParser.parseString(json).getAsJsonObject();
        JsonObject queryObj = jsonResponse.getAsJsonObject("query");
        JsonArray searchResults = queryObj.getAsJsonArray("search");

        WikiSearchResult result = new WikiSearchResult(queryText);
        for (int i = 0; i < searchResults.size(); i++) {
            JsonObject article = searchResults.get(i).getAsJsonObject();
            String title = article.get("title").getAsString();
            int pageId = article.get("pageid").getAsInt();
            result.addResult(new SearchItems(title, pageId));
        }

        return result;
    }
}

class WikiSearchResult {
    private String query;
    private java.util.ArrayList<SearchItems> results;

    public WikiSearchResult(String query) {
        this.query = query;
        this.results = new java.util.ArrayList<>();
    }

    public void addResult(SearchItems item) {
        results.add(item);
    }

    public java.util.ArrayList<SearchItems> getResults() {
        return results;
    }

    public void display() {
        System.out.println("\n=== Результаты поиска: '" + query + "' ===");
        for (int i = 0; i < results.size(); i++) {
            System.out.println((i + 1) + ". " + results.get(i).getTitle());
        }
    }
}

class SearchItems {
    private String title;
    private int pageId;

    public SearchItems(String title, int pageId) {
        this.title = title;
        this.pageId = pageId;
    }

    public String getTitle() {
        return title;
    }

    public void openInBrowser() throws Exception {
        String articleUrl = "https://ru.wikipedia.org/w/index.php?curid=" + pageId;
        System.out.println("Открываю статью: " + title);

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(articleUrl));
            System.out.println("Статья открыта в браузере!");
        } else {
            System.out.println("Открытие браузера не поддерживается.");
        }
    }
}