package pagination.playground;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static pagination.playground.Constants.SA_PREFIX;
import static pagination.playground.Utils.countSA;

@RequiredArgsConstructor
public class PagingClient {

    @Getter
    private final PagingServer server;

    /**
     * Fetch "clean" page: pagina logica P (zero-based), dimensione size,
     * senza service-account in output.
     */
    public List<String> fetchCleanPage(int page, int pageSize) {
        int offsetLogical = page * pageSize;

        // 1) fetch pagina con offset logico
        List<String> pageData = server.fetchPage(offsetLogical, pageSize);

        if (pageData.isEmpty()) {
            return pageData;
        }

        String first = pageData.get(0);
        String last = pageData.get(pageData.size() - 1);

        int pageSA = (int) countSA(pageData);

        // confronto lessicografico per capire se pagina è prima o dopo SA
        if (last.compareTo(SA_PREFIX) < 0) {
            // caso: pagina prima di SA, offset fisico = offset logico
            return pageData;
        }

        if (first.compareTo(SA_PREFIX) >= 0 && pageSA == 0) {
            // caso: pagina dopo SA, shift offset
            int countSA = server.countServiceAccounts();
            // serve solo per fetchare pageSize anzichè pageSize + countSA - pageSA
            return server.fetchPage(offsetLogical + countSA, pageSize);
        } else {

            // caso ibrido: la pagina contiene SA (o è tutta SA)


            // Bound sicuro:
            // fetch = pageSize + countSA - pageSA
            // IPOTESI: posso paginare per un valore qualsiasi o comunque "abbastanza grande" (countSA "piccolo")

            int fetchSize;
            int countSA;

            if (isSAStrictlyContained(pageData)) {
                // ottimizzazione: SA strettamente contenuti nella pagina
                countSA = pageSA;
                fetchSize = pageSize;
            }
            else {
                // se no, mi serve contare gli SA
                countSA = server.countServiceAccounts();
                fetchSize = pageSize + countSA - pageSA;
            }

            List<String> tail = server.fetchPage(offsetLogical + pageData.size(), fetchSize);

            var tailSA = countSA(tail);

            var offset = countSA - pageSA - tailSA;

            return Stream.concat(pageData.stream(), tail.stream())
                    .filter(not(Utils::isSA))
                    .skip(offset)
                    .limit(pageSize)
                    .toList();
        }
    }

    private boolean isSAStrictlyContained(List<String> pageData) {
        List<Integer> saIndices = IntStream.range(0, pageData.size())
                .filter(i -> Utils.isSA(pageData.get(i)))
                .boxed()
                .toList();

        if (saIndices.isEmpty()) return false;

        int minSA = Collections.min(saIndices);
        int maxSA = Collections.max(saIndices);

        return minSA > 0 && maxSA < pageData.size() - 1;
    }
}
