import org.junit.jupiter.api.Test;

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

class PagingServiceReproducibleTest {

    private static final String SA_PREFIX = "service-account-";
    private static final Path DATA_DIR = Path.of("build/test-data");

    @Test
    void testPagingWithGeneratedDataAndPersist() throws IOException {
        List<String> generated = generateRandomData(10, 0.3, 42);
        Collections.sort(generated);
        Path file = persistDataset(generated);
        runPagingAssertions(new PagingService(loadDataset(file)));
    }


    @Test
    void testPagingWithExistingDataset() throws IOException {
        Path file = Path.of("build/test-data/dataset-2026-01-05T20-43-54.txt");
        var service = new PagingService(loadDataset(file));
//        runPagingAssertions(service);
        runPagingAssertion(service, 1, 4);
    }

    // ---------- core test logic ----------

    private void runPagingAssertions(PagingService service) {

        var OVER_PAGE_SIZE = 20;

        for (int pageSize = 1; pageSize <= service.getAllUsers().size() + OVER_PAGE_SIZE; pageSize++) {
            int totalPages = (service.getExpectedClean().size() + pageSize - 1) / pageSize;

            for (int page = 0; page < totalPages; page++) {
                runPagingAssertion(service, page, pageSize);
            }
        }
    }

    private void runPagingAssertion(PagingService service, int page, int pageSize) {
        List<String> expected = slice(service.getExpectedClean(), page * pageSize, pageSize);
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
