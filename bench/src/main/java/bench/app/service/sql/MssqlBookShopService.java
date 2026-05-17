package bench.app.service.sql;

import bench.app.repository.sql.mssql.MssqlBookShopRepository;
import org.springframework.stereotype.Service;

@Service
public class MssqlBookShopService extends SQLBookShopService<MssqlBookShopRepository> {
    private final MssqlBookShopRepository bookShopRepository;

    public MssqlBookShopService(MssqlBookShopRepository bookShopRepository) {
        this.bookShopRepository = bookShopRepository;
    }

    @Override
    public MssqlBookShopRepository getRepository() {
        return this.bookShopRepository;
    }
}
