package bench.app.model.common;

import java.util.Date;

public record BookListItem(
        String author,
        String title,
        String publisher,
        Date publishDate,
        int pages,
        boolean isInReadingRoom
) {
}