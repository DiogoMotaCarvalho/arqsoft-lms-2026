package pt.psoft.g1.psoftg1.lendingmanagement.services;

import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;

/**
 * Port the lending flow calls to obtain the {@link LendingPolicy} applicable to a reader. The
 * actual selection algorithm is #12; until then the default implementation returns the Standard
 * policy for every reader.
 */
public interface LendingPolicyProvider {

  /** The policy that applies to the given reader. */
  LendingPolicy policyFor(ReaderDetails reader);
}
