package pt.psoft.g1.psoftg1.lendingmanagement.services;

final class LendingPolicyFixtures {
  static final int STANDARD_MAX_ACTIVE = 3;
  static final int STANDARD_DURATION = 15;
  static final int ADAPTIVE_LONG_PERIOD = 21;
  static final int HISTORY_MONTHS = 12;
  static final int MIN_COMPLETED = 5;

  private LendingPolicyFixtures() {}

  static LendingPolicyFactory factory() {
    LendingPolicyProperties properties = new LendingPolicyProperties();

    LendingPolicyProperties.Standard standard = properties.getStandard();
    standard.setMaxActiveLendings(STANDARD_MAX_ACTIVE);
    standard.setLendingDurationInDays(STANDARD_DURATION);
    standard.setMaxRenewals(3);
    standard.setRenewalDurationInDays(15);

    LendingPolicyProperties.Adaptive adaptive = properties.getAdaptive();
    adaptive.setMaxActiveLendings(5);
    adaptive.setFirstThreshold(2);
    adaptive.setSecondThreshold(4);
    adaptive.setLongPeriod(ADAPTIVE_LONG_PERIOD);
    adaptive.setStandardPeriod(14);
    adaptive.setShortPeriod(7);

    LendingPolicyProperties.Selection selection = properties.getSelection();
    selection.setHistoryPeriodInMonths(HISTORY_MONTHS);
    selection.setMinCompletedLendings(MIN_COMPLETED);
    selection.setMinOnTimeRatio(80);

    return new LendingPolicyFactory(properties);
  }
}
