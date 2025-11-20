import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

class Address {
    private String city;
    private String street;
    private String house;
    private int floor;

    public Address(String city, String street, String house, int floor) {
        this.city = city;
        this.street = street;
        this.house = house;
        this.floor = floor;
    }

    public String getCity() { return city; }
    public String getStreet() { return street; }
    public String getHouse() { return house; }
    public int getFloor() { return floor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return floor == address.floor &&
                Objects.equals(city, address.city) &&
                Objects.equals(street, address.street) &&
                Objects.equals(house, address.house);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, street, house, floor);
    }

    @Override
    public String toString() {
        return String.format("Город: %s, Улица: %s, Дом: %s, Этажей: %d",
                city, street, house, floor);
    }
}

abstract class FileParser {
    public abstract List<Address> parse(String filePath);
}

class CsvParser extends FileParser {
    @Override
    public List<Address> parse(String filePath) {
        List<Address> addresses = new ArrayList<>(1000);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath), 8192 * 4)) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = fastSplit(line);
                if (parts.length >= 4) {
                    try {
                        String city = parts[0].trim().replace("\"", "");
                        String street = parts[1].trim().replace("\"", "");
                        String house = parts[2].trim();
                        int floor = Integer.parseInt(parts[3].trim());

                        addresses.add(new Address(city, street, house, floor));
                    } catch (NumberFormatException e) {
                        System.out.println("Пропущена некорректная строка: " + line);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден: " + filePath);
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + e.getMessage());
        }

        return addresses;
    }

    private String[] fastSplit(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if ((c == ',' || c == ';') && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());

        return parts.toArray(new String[0]);
    }
}

class XmlParser extends FileParser {
    @Override
    public List<Address> parse(String filePath) {
        List<Address> addresses = new ArrayList<>(1000);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(filePath));
            NodeList itemNodes = document.getElementsByTagName("item");

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node itemNode = itemNodes.item(i);

                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element itemElement = (Element) itemNode;

                    try {
                        String city = itemElement.getAttribute("city");
                        String street = itemElement.getAttribute("street");
                        String house = itemElement.getAttribute("house");
                        int floor = Integer.parseInt(itemElement.getAttribute("floor"));

                        addresses.add(new Address(city, street, house, floor));
                    } catch (NumberFormatException e) {
                        System.out.println("Пропущена некорректная XML запись");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден: " + filePath);
        } catch (IOException e) {
            System.out.println("Ошибка чтения XML файла: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Ошибка парсинга XML: " + e.getMessage());
        }

        return addresses;
    }
}

class StatisticsCalculator {

    public Map<Address, Integer> findDuplicates(List<Address> addresses) {
        Map<Address, Integer> addressCount = new HashMap<>(addresses.size());

        for (Address address : addresses) {
            addressCount.merge(address, 1, Integer::sum);
        }

        Map<Address, Integer> duplicates = new HashMap<>();
        for (Map.Entry<Address, Integer> entry : addressCount.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.put(entry.getKey(), entry.getValue());
            }
        }

        return duplicates;
    }

    public Map<String, int[]> calculateFloorStatistics(List<Address> addresses) {
        Map<String, int[]> cityFloorStats = new HashMap<>();

        for (Address address : addresses) {
            String city = address.getCity();
            int floor = address.getFloor();

            int[] floors = cityFloorStats.computeIfAbsent(city, k -> new int[5]);

            if (floor >= 1 && floor <= 5) {
                floors[floor - 1]++;
            }
        }

        return cityFloorStats;
    }
}

public class AddressAnalyzer {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        StatisticsCalculator statsCalculator = new StatisticsCalculator();

        System.out.println("=== Анализатор адресов ===");
        System.out.println("Введите путь к файлу (CSV или XML)");
        System.out.println("Для выхода нажмите Ctrl+D или введите 'exit'");

        while (true) {
            System.out.print("\n> ");

            try {
                if (!scanner.hasNextLine()) {
                    System.out.println("Завершение работы...");
                    break;
                }

                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Завершение работы...");
                    break;
                }

                if (input.isEmpty()) {
                    continue;
                }

                long startTime = System.currentTimeMillis();

                List<Address> addresses = parseFile(input);

                if (addresses.isEmpty()) {
                    System.out.println("Файл не содержит данных или произошла ошибка при чтении");
                    continue;
                }

                Map<Address, Integer> duplicates = statsCalculator.findDuplicates(addresses);
                Map<String, int[]> floorStats = statsCalculator.calculateFloorStatistics(addresses);

                long endTime = System.currentTimeMillis();
                long processingTime = endTime - startTime;

                printStatistics(duplicates, floorStats, processingTime);

            } catch (NoSuchElementException e) {
                System.out.println("\nЗавершение работы...");
                break;
            }
        }

        scanner.close();
    }

    private static List<Address> parseFile(String filePath) {
        FileParser parser;

        if (filePath.toLowerCase().endsWith(".csv")) {
            parser = new CsvParser();
        } else if (filePath.toLowerCase().endsWith(".xml")) {
            parser = new XmlParser();
        } else {
            System.out.println("Неподдерживаемый формат файла. Используйте .csv или .xml");
            return new ArrayList<>();
        }

        return parser.parse(filePath);
    }

    private static void printStatistics(Map<Address, Integer> duplicates,
                                        Map<String, int[]> floorStats,
                                        long processingTime) {

        System.out.println("\n=== РЕЗУЛЬТАТЫ СТАТИСТИКИ ===");
        System.out.println("Время обработки: " + processingTime + " мс");

        System.out.println("\n--- ДУБЛИРУЮЩИЕСЯ ЗАПИСИ ---");
        if (duplicates.isEmpty()) {
            System.out.println("Дубликаты не найдены");
        } else {
            for (Map.Entry<Address, Integer> entry : duplicates.entrySet()) {
                System.out.println(entry.getKey() + " - повторений: " + entry.getValue());
            }
        }

        System.out.println("\n--- СТАТИСТИКА ПО ЭТАЖАМ ---");
        for (Map.Entry<String, int[]> entry : floorStats.entrySet()) {
            String city = entry.getKey();
            int[] floors = entry.getValue();

            System.out.printf("%s: 1-этажных: %d, 2-этажных: %d, 3-этажных: %d, 4-этажных: %d, 5-этажных: %d%n",
                    city, floors[0], floors[1], floors[2], floors[3], floors[4]);
        }
    }
}