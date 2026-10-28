package pt.psoft.g1.psoftg1.lendingmanagement.services;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicySelector;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.ReaderLendingHistory;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.SelectionCriteria;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardLendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;

/**
 * The runtime {@link LendingPolicyProvider} (issue #12). It gathers the reader's lending history,
 * delegates the decision to a framework-free {@link LendingPolicySelector}, and resolves the chosen
 * policy name through the {@link LendingPolicyFactory}. The history is computed on each call, so
 * the applicable policy is re-evaluated per request. This replaces the placeholder that always
 * returned the Standard policy. See ADR-0006.
 */
@Service
public class SelectingLendingPolicyProvider implements LendingPolicyProvider {

  private final LendingRepository lendingRepository;
  private final LendingPolicyFactory policyFactory;
  private final SelectionCriteria criteria;
  private final LendingPolicySelector selector;

  public SelectingLendingPolicyProvider(
      LendingRepository lendingRepository, LendingPolicyFactory policyFactory) {
    this.lendingRepository = lendingRepository;
    this.policyFactory = policyFactory;
    this.criteria = policyFactory.selectionCriteria();
    this.selector = new LendingPolicySelector(criteria);
  }

  @Override
  public LendingPolicy policyFor(ReaderDetails reader) {
    if (reader == null) {
      return policyFactory.create(StandardLendingPolicy.NAME);
    }
    final ReaderLendingHistory history = buildHistory(reader.getReaderNumber());
    return policyFactory.create(selector.selectPolicyName(history));
  }

  private ReaderLendingHistory buildHistory(String readerNumber) {
    final LocalDate since = LocalDate.now().minusMonths(criteria.getHistoryPeriodInMonths());
    final List<Lending> completed =
        lendingRepository.listCompletedByReaderNumberSince(readerNumber, since);
    int onTimeCount = 0;
    for (Lending lending : completed) {
      if (lending.getDaysDelayed() == 0) {
        onTimeCount++;
      }
    }
    boolean hasCurrentOverdue = false;
    for (Lending lending : lendingRepository.listOutstandingByReaderNumber(readerNumber)) {
      if (lending.getDaysDelayed() > 0) {
        hasCurrentOverdue = true;
        break;
      }
    }
    return new ReaderLendingHistory(completed.size(), onTimeCount, hasCurrentOverdue);
  }
}
