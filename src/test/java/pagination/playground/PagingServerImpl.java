package pagination.playground;

import java.util.List;

import static java.util.function.Predicate.not;
import static pagination.playground.Utils.countSA;

public class PagingServerImpl implements TestPagingServer {


    private final List<String> allUsers; // lista ordinata completa con SA

    public PagingServerImpl(List<String> allUsers) {
        this.allUsers = allUsers;
    }

    @Override
    public List<String> getAllUsers() {
        return allUsers;
    }

    @Override
    public List<String> getExpectedClean() {
        return allUsers.stream()
                .filter(not(Utils::isSA))
                .toList();
    }

    @Override
    public int countServiceAccounts() {
        return (int) countSA(allUsers);
    }

    @Override
    public List<String> fetchPage(int offset, int size) {
        int end = Math.min(offset + size, allUsers.size());
        if (offset >= allUsers.size()) return List.of();
        return allUsers.subList(offset, end);
    }
}
