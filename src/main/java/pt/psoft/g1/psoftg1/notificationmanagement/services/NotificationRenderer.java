package pt.psoft.g1.psoftg1.notificationmanagement.services;

import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * Turns a {@link LendingEvent} into a {@link Notification}, producing the subject and body once per
 * event from hardcoded per-event-type wording with the lending and reader details interpolated
 * (issue #18, ADR-0014). Rendering lives here, run before any channel is called, so the wording is
 * consistent across mechanisms and the adapters do not each re-render.
 *
 * <p>This is a framework-free, application-layer class: it is the event-to-notification mapping
 * that the transparent-box domain tests (C3.2) exercise directly over plain objects.
 */
public class NotificationRenderer {

  public Notification render(LendingEvent event) {
    final String subject;
    final String body;
    final String reader = event.getReaderNumber();
    final String lending = event.getLendingNumber();

    switch (event.getType()) {
      case CREATED:
        subject = "New lending " + lending;
        body =
            "Dear reader "
                + reader
                + ", your lending "
                + lending
                + " was registered on "
                + event.getStartDate()
                + " and is due on "
                + event.getLimitDate()
                + ".";
        break;
      case RENEWED:
        subject = "Lending " + lending + " renewed";
        body =
            "Dear reader "
                + reader
                + ", your lending "
                + lending
                + " was renewed and is now due on "
                + event.getLimitDate()
                + ".";
        break;
      case RETURNED:
        subject = "Lending " + lending + " returned";
        body =
            "Dear reader "
                + reader
                + ", your return of lending "
                + lending
                + " was registered on "
                + event.getReturnedDate()
                + ".";
        break;
      case OVERDUE:
        subject = "Lending " + lending + " overdue";
        body =
            "Dear reader "
                + reader
                + ", your lending "
                + lending
                + " was due on "
                + event.getLimitDate()
                + " and is now overdue. Please return or renew it.";
        break;
      default:
        throw new IllegalArgumentException("Unsupported lending event type: " + event.getType());
    }

    return new Notification(
        reader,
        event.getEmail(),
        event.getPhone(),
        event.getType(),
        lending,
        event.getLimitDate(),
        subject,
        body);
  }
}
