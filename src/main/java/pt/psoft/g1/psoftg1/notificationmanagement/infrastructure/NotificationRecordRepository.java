package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Persistence of the delivery audit (issue #22, ADR-0016). The composite notifier writes one record
 * per channel per event through this repository. There is no read API for the audit; it is read
 * through this repository by operators and by the component and system tests, which assert the
 * persisted outcomes. The finders support those assertions.
 */
public interface NotificationRecordRepository extends CrudRepository<NotificationRecord, Long> {

  List<NotificationRecord> findByLendingNumber(String lendingNumber);

  List<NotificationRecord> findByReaderNumber(String readerNumber);
}
