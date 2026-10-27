package pt.psoft.g1.psoftg1.lendingmanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardLendingPolicy;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;

/**
 * Default {@link LendingPolicyProvider}: a stub that returns the Standard policy for every reader.
 * Real per-reader selection is #12; this keeps the lending flow wired to the seam until then.
 */
@Service
@RequiredArgsConstructor
public class DefaultLendingPolicyProvider implements LendingPolicyProvider {

  private final LendingPolicyFactory policyFactory;

  @Override
  public LendingPolicy policyFor(ReaderDetails reader) {
    return policyFactory.create(StandardLendingPolicy.NAME);
  }
}
