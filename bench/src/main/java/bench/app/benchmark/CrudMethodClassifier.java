package bench.app.benchmark;

import org.springframework.stereotype.Component;

@Component
public class CrudMethodClassifier {
    public CrudOperationType classify(String methodName) {
        String lowerMethodName = methodName.toLowerCase();

        if (lowerMethodName.startsWith("save") || lowerMethodName.startsWith("insert")
                || lowerMethodName.startsWith("create") || lowerMethodName.startsWith("add")) {
            return CrudOperationType.CREATE;
        }
        if (lowerMethodName.startsWith("find") || lowerMethodName.startsWith("get")
                || lowerMethodName.startsWith("read") || lowerMethodName.startsWith("exists")
                || lowerMethodName.startsWith("count")) {
            return CrudOperationType.READ;
        }
        if (lowerMethodName.startsWith("update") || lowerMethodName.startsWith("set")
                || lowerMethodName.startsWith("modify")) {
            return CrudOperationType.UPDATE;
        }
        if (lowerMethodName.startsWith("delete") || lowerMethodName.startsWith("remove")) {
            return CrudOperationType.DELETE;
        }

        return CrudOperationType.OTHER;
    }
}
