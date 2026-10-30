package pt.psoft.g1.psoftg1.notificationmanagement.services;

import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * The single collaborator the after-commit listener calls to deliver a notification (issue #22,
 * ADR-0016). Its implementation ({@link CompositeNotifier}) applies the setup-time mechanism
 * selection, fans the notification out to the active channels, and audits each attempt. The listener
 * depends on this abstraction, not on the channels, so the fan-out and audit policy lives in one
 * place.
 */
public interface Notifier {

  /** Delivers the notification to every active mechanism, auditing each attempt. */
  void notify(Notification notification);
}
