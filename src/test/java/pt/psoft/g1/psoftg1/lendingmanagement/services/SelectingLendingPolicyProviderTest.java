package pt.psoft.g1.psoftg1.lendingmanagement.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;

/**
 * Tests the runtime provider by supplying representative reader histories through a substituted
 * repository and asserting the policy it resolves. The criteria come from the real factory (minimum
 * 5 completed lendings, minimum on-time ratio 80). The decision itself is covered in depth by
 * LendingPolicySelectorTest, so these cases confirm the gathering and wiring.
 */
class SelectingLendingPolicyProviderTest {

  private static final String READER = "2024/1";

  private static LendingPolicyFactory factory() {
    LendingPolicyProperties props = new LendingPolicyProperties();
    LendingPolicyProperties.Standard s = props.getStandard();
    s.setMaxActiveLendings(3);
    s.setLendingDurationInDays(15);
    s.setMaxRenewals(3);
    s.setRenewalDurationInDays(15);
    LendingPolicyProperties.Adaptive a = props.getAdaptive();
    a.setMaxActiveLendings(5);
    a.setFirstThreshold(2);
    a.setSecondThreshold(4);
    a.setLongPeriod(21);
    a.setStandardPeriod(14);
    a.setShortPeriod(7);
    LendingPolicyProperties.Selection sel = props.getSelection();
    sel.setHistoryPeriodInMonths(12);
    sel.setMinCompletedLendings(5);
    sel.setMinOnTimeRatio(80);
    return new LendingPolicyFactory(props);
  }

  private static Lending lendingDelayedBy(int days) {
    Lending lending = mock(Lending.class);
    when(lending.getDaysDelayed()).thenReturn(days);
    return lending;
  }

  private static ReaderDetails reader() {
    ReaderDetails reader = mock(ReaderDetails.class);
    when(reader.getReaderNumber()).thenReturn(READER);
    return reader;
  }

  /** A list of mocked completed lendings, {@code onTime} of which were returned on time. */
  private static List<Lending> completed(int total, int onTime) {
    List<Lending> lendings = new ArrayList<>();
    for (int i = 0; i < total; i++) {
      lendings.add(lendingDelayedBy(i < onTime ? 0 : 3));
    }
    return lendings;
  }

  @Test
  void selectsAdaptiveForAGoodHistory() {
    List<Lending> completed = completed(5, 5);
    LendingRepository repo = mock(LendingRepository.class);
    when(repo.listCompletedByReaderNumberSince(eq(READER), any(LocalDate.class)))
        .thenReturn(completed);
    when(repo.listOutstandingByReaderNumber(READER)).thenReturn(List.of());

    var provider = new SelectingLendingPolicyProvider(repo, factory());
    assertEquals("adaptive", provider.policyFor(reader()).name());
  }

  @Test
  void selectsStandardWhenTooFewCompletedLendings() {
    List<Lending> completed = completed(2, 2);
    LendingRepository repo = mock(LendingRepository.class);
    when(repo.listCompletedByReaderNumberSince(eq(READER), any(LocalDate.class)))
        .thenReturn(completed);
    when(repo.listOutstandingByReaderNumber(READER)).thenReturn(List.of());

    var provider = new SelectingLendingPolicyProvider(repo, factory());
    assertEquals("standard", provider.policyFor(reader()).name());
  }

  @Test
  void selectsStandardWhenAnActiveLendingIsOverdue() {
    List<Lending> completed = completed(5, 5);
    List<Lending> outstanding = List.of(lendingDelayedBy(3));
    LendingRepository repo = mock(LendingRepository.class);
    when(repo.listCompletedByReaderNumberSince(eq(READER), any(LocalDate.class)))
        .thenReturn(completed);
    when(repo.listOutstandingByReaderNumber(READER)).thenReturn(outstanding);

    var provider = new SelectingLendingPolicyProvider(repo, factory());
    assertEquals("standard", provider.policyFor(reader()).name());
  }

  @Test
  void toleratesNullReaderByReturningStandard() {
    LendingRepository repo = mock(LendingRepository.class);
    var provider = new SelectingLendingPolicyProvider(repo, factory());
    assertEquals("standard", provider.policyFor(null).name());
    verifyNoInteractions(repo);
  }
}
