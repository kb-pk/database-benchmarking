package bench.model.common;

import java.util.Date;

public record Book(
    BookShop bookShop,

    String author,
    String title,
    String publisher,
    Date publishDate,
    int pages,
    boolean isInReadingRoom
) {}