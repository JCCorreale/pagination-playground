package pagination.playground;

import lombok.experimental.UtilityClass;

import java.util.List;

import static pagination.playground.Constants.SA_PREFIX;

@UtilityClass
public class Utils {

    public static long countSA(List<String> tail) {
        return tail.stream()
                .filter(Utils::isSA)
                .count();
    }

    public static boolean isSA(String u) {
        return u.startsWith(SA_PREFIX);
    }
}
