package pagination.playground;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagingClientReproducibleTest {

    private static final String SA_PREFIX = "service-account-";
    private static final Path DATA_DIR = Path.of("build/test-data");

    @ParameterizedTest
    @CsvSource({
             "100,0.3",
             "200,0.5",
             "300,0.1",
             "500,0.8",
             "500,0.3",
            "1000,0.3"
    })
    void testPagingWithGeneratedDataAndPersist(int size, double saRatio) {
        long seed = System.currentTimeMillis();
        System.out.println("Using seed: " + seed);
        List<String> generated = generateRandomData(size, saRatio, seed);
        Collections.sort(generated);
//        persistDataset(generated);
        var server = new PagingServerImpl(generated);
        runPagingAssertions(server, new PagingClient(server));
    }

    @Test
    void testPaging() throws IOException {

        long seed = 1767700602857L;
        int size = 100;
        double saRatio = 0.3;
        int page = 1;
        int pageSize = 41;

        List<String> generated = generateRandomData(size, saRatio, seed);
        Collections.sort(generated);
//        persistDataset(generated);
        var server = new PagingServerImpl(generated);
        runPagingAssertion(server, new PagingClient(server), page, pageSize);
    }

    // ---------- core test logic ----------

    private void runPagingAssertions(TestPagingServer server, PagingClient client) {

        var OVER_PAGE_SIZE = 20;

        for (int pageSize = 1; pageSize <= server.getAllUsers().size() + OVER_PAGE_SIZE; pageSize++) {
            int totalPages = (server.getExpectedClean().size() + pageSize - 1) / pageSize;

            for (int page = 0; page < totalPages; page++) {
                runPagingAssertion(server, client, page, pageSize);
            }
        }
    }

    private void runPagingAssertion(TestPagingServer server, PagingClient service, int page, int pageSize) {
        List<String> expected = slice(server.getExpectedClean(), page * pageSize, pageSize);
        List<String> actual = service.fetchCleanPage(page, pageSize);

        assertEquals(
                expected,
                actual,
                "Mismatch at page %d page size %d".formatted(page, pageSize)
        );
    }

    // ---------- dataset generation ----------

    private List<String> generateRandomData(int size, double saRatio, long seed) {
        Random random = new Random(seed);
        List<String> result = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            if (random.nextDouble() < saRatio) {
                result.add(SA_PREFIX + randomAlpha(random, 6));
            } else {
                result.add(randomAlpha(random, 8));
            }
        }
        return result;
    }

    private String randomAlpha(Random r, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + r.nextInt(26)));
        }
        return sb.toString();
    }

    // ---------- persistence ----------

    private Path persistDataset(List<String> data) throws IOException {
        Files.createDirectories(DATA_DIR);

        String ts = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"));

        Path file = DATA_DIR.resolve("dataset-" + ts + ".txt");
        Files.write(file, data);

        return file;
    }

    private List<String> loadDataset(Path file) throws IOException {
        return Files.readAllLines(file);
    }

    // ---------- utils ----------

    private List<String> slice(List<String> list, int from, int size) {
        if (from >= list.size()) {
            return List.of();
        }
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }
}
