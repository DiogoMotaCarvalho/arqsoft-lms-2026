package pt.psoft.g1.psoftg1.lendingmanagement.model.events;

/**
 * The four relevant occurrences in a lending's lifecycle that may notify the reader (issue #18,
 * ADR-0014). The first three follow a reader action on {@code LendingServiceImpl}; the fourth has
 * no user action and is detected by a scheduled sweep.
 */
public enum LendingEventType {
  CREATED,
  RENEWED,
  RETURNED,
  OVERDUE
}
