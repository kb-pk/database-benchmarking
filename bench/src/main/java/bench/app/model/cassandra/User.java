package bench.app.model.cassandra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    @PrimaryKey("user_id")
    private UUID userId;

    private String name;
    private String surname;
    @Column("phone_number")
    private String phoneNumber;
    private String email;
    @Column("main_book_shop_id")
    private UUID mainBookShopId;
    @Column("card_id_number")
    private String cardIdNumber;
    private String status;
    private String login;
    private Set<String> permissions;
}