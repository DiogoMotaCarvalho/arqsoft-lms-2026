package pt.psoft.g1.psoftg1.bootstrapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;

@SpringBootTest
@ActiveProfiles("bootstrap")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:bootstrapper;IGNORECASE=TRUE")
@DirtiesContext
class BootstrapperIT {

  @Autowired private ReaderRepository readerRepository;
  @Autowired private LendingRepository lendingRepository;

  @Test
  void seedsReadersNumberedInTheCurrentYear() {
    assertTrue(readerRepository.findByReaderNumber(Year.now() + "/1").isPresent());
  }

  @Test
  void seedsLendingsForTheSeededReaders() {
    assertTrue(lendingRepository.findByLendingNumber("2024/1").isPresent());
  }
}
