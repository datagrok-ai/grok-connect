package serialization;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// Turns on the level-2 encoders (off by default: Datagrok 1.27.x and older cannot read them)
// for each test of a class annotated with @ExtendWith(Level2Encoders.class).
public class Level2Encoders implements BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(Level2Encoders.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(NS).put("writerLevel", IntColumn.WRITER_LEVEL);
        IntColumn.WRITER_LEVEL = 2;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        IntColumn.WRITER_LEVEL = context.getStore(NS).get("writerLevel", Integer.class);
    }
}
