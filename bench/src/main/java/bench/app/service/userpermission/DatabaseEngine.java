package bench.app.service.userpermission;

import java.util.Arrays;

public enum DatabaseEngine {
    POSTGRESQL("postgresql"),
    MSSQL("mssql"),
    CASSANDRA("cassandra"),
    SCYLLA("scylla");

    private final String propertyValue;

    DatabaseEngine(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String propertyValue() {
        return propertyValue;
    }

    public static DatabaseEngine fromValue(String value) {
        return Arrays.stream(values())
                .filter(engine -> engine.propertyValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nieobslugiwany silnik bazy: " + value));
    }
}
