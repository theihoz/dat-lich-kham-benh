# 03: Create Class + Sequence diagrams for 5 existing patterns

**What to build:** 10 draw.io diagrams (1 Class + 1 Sequence per pattern) for Builder, Factory Method, Adapter, Strategy, and Observer. Each Class Diagram shows interfaces, abstract classes, concrete classes, and their relationships as implemented in the codebase. Each Sequence Diagram shows the runtime interaction flow (e.g., Observer: BookingServiceImpl → EventPublisher → Listener → Consumer → Creator → Sender).

**Blocked by:** None (can start immediately).

**Status:** completed

- [ ] `builder-class.drawio` + `builder-sequence.drawio` — Appointment.AppointmentBuilder
- [ ] `factory-method-class.drawio` + `factory-method-sequence.drawio` — NotificationCreator hierarchy
- [ ] `adapter-class.drawio` + `adapter-sequence.drawio` — NotificationSender with SMTP/Demo adapters
- [ ] `strategy-class.drawio` + `strategy-sequence.drawio` — DoctorSortStrategy with 4 strategies + registry
- [ ] `observer-class.drawio` + `observer-sequence.drawio` — EmailNotificationRequestedEvent pipeline
