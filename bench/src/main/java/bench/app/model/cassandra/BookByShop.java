package bench.app.model.cassandra;

import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Table("books_by_shop")
public class BookByShop {
    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    @Column("shop_id")
    private UUID shopId;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED)
    @Column("book_id")
    private UUID bookId;

    private String author;
    private String title;
    private String publisher;
    @Column("publish_date")
    private LocalDate publishDate;
    private int pages;
    @Column("is_in_reading_room")
    private boolean isInReadingRoom;
}
