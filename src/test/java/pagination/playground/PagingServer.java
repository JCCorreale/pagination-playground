package pagination.playground;

import java.util.List;

public interface PagingServer {

    int countServiceAccounts();

    List<String> fetchPage(int offset, int size);
}
