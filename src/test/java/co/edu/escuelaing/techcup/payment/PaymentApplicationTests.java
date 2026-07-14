package co.edu.escuelaing.techcup.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MongoTestcontainersConfiguration.class)
class PaymentApplicationTests {

	@Test
	void contextLoads() {
	}

}
