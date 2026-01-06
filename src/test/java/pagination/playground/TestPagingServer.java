package pagination.playground;

import java.util.List;

public interface TestPagingServer extends PagingServer {

    List<String> getAllUsers();

    List<String> getExpectedClean();

    int getFetchPageCount();

    int getCountSACount();

    int getTotalRecordsFetched();
}
