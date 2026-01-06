import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PagingService {
    private final List<String> allUsers; // lista ordinata completa con SA
    private final String saPrefix = "service-account-";

    public PagingService(List<String> allUsers) {
        this.allUsers = allUsers;
    }

    public List<String> getAllUsers() {
        return allUsers;
    }

    public List<String> getExpectedClean() {
        return allUsers.stream()
                .filter(u -> !u.startsWith(saPrefix))
                .toList();
    }

    public int countServiceAccounts() {
        return (int) allUsers.stream().filter(u -> u.startsWith(saPrefix)).count();
    }

    public List<String> fetchPage(int offset, int size) {
        int end = Math.min(offset + size, allUsers.size());
        if (offset >= allUsers.size()) return List.of();
        return allUsers.subList(offset, end);
    }

    /**
     * Fetch "clean" page: pagina logica P (zero-based), dimensione size,
     * senza service-account in output.
     */
    public List<String> fetchCleanPage(int page, int size) {
        int offsetLogical = page * size;

        // 1) fetch pagina con offset logico
        List<String> pageData = fetchPage(offsetLogical, size);

        if (pageData.isEmpty()) {
            return pageData;
        }

        String first = pageData.get(0);
        String last = pageData.get(pageData.size() - 1);

        int pageSA = (int)pageData.stream().filter(u -> u.startsWith(saPrefix)).count();

        // confronto lessicografico per capire se pagina è prima o dopo SA
        if (last.compareTo(saPrefix) < 0) {
            // caso: pagina prima di SA, offset fisico = offset logico
            return pageData;
        }

        int countSA = countServiceAccounts();

        if (first.compareTo(saPrefix) >= 0 && pageSA == 0) {
            // caso: pagina dopo SA, shift offset
            // serve solo per fetchare pageSize anzichè pageSize + countSA - pageSA
            return fetchPage(offsetLogical + countSA, size);
        } else {

            // caso ibrido: la pagina contiene SA (o è tutta SA)

            // Bound sicuro:
            // fetch = pageSize + countSA - pageSA
            int fetchSize = size + countSA - pageSA;

            List<String> tail = fetchPage(offsetLogical + pageData.size(), fetchSize);

            return Stream.concat(pageData.stream(), tail.stream())
                    .filter(u -> !u.startsWith(saPrefix))
                    .limit(size)
                    .toList();
        }
    }
}
