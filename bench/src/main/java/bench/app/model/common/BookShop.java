package bench.app.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BookShop {
    private Employee manager;
    private final OpeningHours openingHours;
    private final List<Book> bookOfferings;

    private final String shopName;
    private final String address;
    private final String email;
}
