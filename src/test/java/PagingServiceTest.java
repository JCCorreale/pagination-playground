import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


public class PagingServiceTest {

    private static final String SA_PREFIX = "service-account-";

    @Test
    void testPagingAgainstFilteredList() {
        Random random = new Random(42);

        List<String> all = new ArrayList<>();
        int total = 10;

        for (int i = 0; i < total; i++) {
            if (random.nextDouble() < 0.30) {
                all.add(SA_PREFIX + randomAlphabetic(random, 6));
            } else {
                all.add(randomAlphabetic(random, 8));
            }
        }

        // requisito ESSENZIALE
        Collections.sort(all);

        PagingService service = new PagingService(all);

        // lista di riferimento (senza SA)
        List<String> expectedClean =
                all.stream()
                        .filter(u -> !u.startsWith(SA_PREFIX))
                        .toList();

        int pageSize = 17;
        int totalPages = (expectedClean.size() + pageSize - 1) / pageSize;

        for (int page = 0; page < totalPages; page++) {
            List<String> expected = subList(expectedClean, page * pageSize, pageSize);
            List<String> actual = service.fetchCleanPage(page, pageSize);

            System.out.println("expected size " +  expected.size() + ", actual size " + actual.size());

            assertEquals(
                    expected,
                    actual,
                    "Mismatch at page " + page
            );
        }
    }

    private static String randomAlphabetic(Random r, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + r.nextInt(26)));
        }
        return sb.toString();
    }

    private static List<String> subList(List<String> list, int from, int size) {
        if (from >= list.size()) {
            return List.of();
        }
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }

    @Test
    public void testAllSABefore() {
        List<String> data = List.of(
                SA_PREFIX + "1",
                SA_PREFIX + "2",
                "utente_1",
                "utente_2",
                "utente_3",
                "utente_4"
        );
        PagingService service = new PagingService(data);

        List<String> page0 = service.fetchCleanPage(1, 2);
        assertEquals(List.of("utente_3", "utente_4"), page0);
    }

    @Test
    public void testAllSAAfter() {
        List<String> data = List.of(
                "a_utente_1",
                "a_utente_2",
                "a_utente_3",
                "a_utente_4",
                SA_PREFIX + "1",
                SA_PREFIX + "2",
                SA_PREFIX + "3"
        );
        PagingService service = new PagingService(data);

//        // pagina 0 size 2 = utente_1, utente_2 (offset fisico = offset logico)
//        List<String> page0 = service.fetchCleanPage(0, 2);
//        assertEquals(List.of("a_utente_1", "a_utente_2"), page0);
//
//        // pagina 1 size 2 = utente_3, utente_4
//        List<String> page1 = service.fetchCleanPage(1, 2);
//        assertEquals(List.of("a_utente_3", "a_utente_4"), page1);

        // pagina 2 size 2 = vuoto
        List<String> page2 = service.fetchCleanPage(2, 2);
        assertEquals(List.of(), page2);
    }

    @Test
    public void testHybridCase() {
        List<String> data = List.of(
                "a_utente_1",
                SA_PREFIX + "1",
                SA_PREFIX + "2",
                "utente_2",
                "utente_3",
                "utente_4"
        );
        PagingService service = new PagingService(data);

        List<String> page0 = service.fetchCleanPage(0, 3);
        assertEquals(List.of("a_utente_1", "utente_2", "utente_3"), page0);

        List<String> page1 = service.fetchCleanPage(1, 3);
        assertEquals(List.of("utente_4"), page1);
    }
}
