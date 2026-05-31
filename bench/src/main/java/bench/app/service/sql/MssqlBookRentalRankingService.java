package bench.app.service.sql;

import bench.app.model.common.BookRentalRanking;
import bench.app.repository.sql.mssql.MssqlBookRentalRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MssqlBookRentalRankingService {
    private final MssqlBookRentalRepository repository;

    public MssqlBookRentalRankingService(MssqlBookRentalRepository repository) {
        this.repository = repository;
    }

    public List<BookRentalRanking> getBookRentalRankingByShop(int shopId) {
        return repository.findBookRentalRankingByShop(shopId).stream()
                .map(row -> new BookRentalRanking(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()
                ))
                .collect(Collectors.toList());
    }
}
