package at.ac.tuwien.inso.ctrl;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class CtrlApplicationTests {

    @Test
    void contextLoads() {
        // emtpy on purpose! try loading application context (checks for missing dependencies etc.)
    }

}