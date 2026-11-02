# Design Patterns Documentation & Facade Implementation — Spec

## Problem Statement

The medical appointment booking system already implements 5 of 6 required design patterns (Builder, Factory Method, Adapter, Strategy, Observer) with 28 passing tests, but has **zero documentation**: no academic report, no UML diagrams, no SOLID analysis, and no formal Facade pattern. The academic project requires a complete 7-chapter report in Vietnamese, 15 draw.io UML diagrams, proof of 5 SOLID principles with code evidence, and a working 6th pattern (Facade) — all graded on a rubric totaling 6+ points.

## Solution

Add the missing Facade pattern as `AppointmentBookingFacade` sitting above the existing `BookingService`, create all required documentation (one consolidated `BAO_CAO.md` report with embedded code, separate draw.io diagrams, and a demo script), map existing tests to the required TC01–TC12 matrix, and fill the two test gaps (TC03 input validation, TC08 Facade orchestration).

## User Stories

1. As a grader, I want to see 6 design patterns from all 3 groups (Creational, Structural, Behavioral) implemented in the codebase, so that I can verify the student understands pattern selection and application.
2. As a grader, I want a Class Diagram and Sequence Diagram for each of the 6 patterns, so that I can evaluate design comprehension.
3. As a grader, I want code snippets embedded in the report for each pattern, so that I can verify the implementation matches the design.
4. As a grader, I want analysis of pros/cons for each pattern vs. a pattern-free design, so that I can evaluate critical thinking.
5. As a grader, I want proof of all 5 SOLID principles with specific code references, so that I can verify adherence to design principles.
6. As a grader, I want a Use Case Diagram and at least 3 detailed use case specifications, so that I can evaluate requirements analysis (0.5 points).
7. As a grader, I want theory sections covering all 3 pattern groups and all 5 SOLID principles, so that I can evaluate foundational knowledge (1.5 points).
8. As a grader, I want 12 test cases with expected vs. actual results, so that I can evaluate implementation quality (1.5 points).
9. As a grader, I want a demo that runs without Docker dependencies, so that I can verify the system works.
10. As a student presenting, I want a demo script with seed accounts and step-by-step instructions, so that the demo runs smoothly.
11. As a patient actor in the system, I want the Facade to simplify the booking flow into a single call, so that the controller doesn't need to coordinate multiple services.
12. As a reader of the report, I want the architecture diagram showing 3 layers and cross-cutting concerns, so that I understand the system structure.
13. As a reader of the report, I want an ERD showing all 9 entities and their relationships, so that I understand the data model.

## Implementation Decisions

1. **Facade pattern placement**: `AppointmentBookingFacade` is a new interface + implementation that sits **above** `BookingService` in the call chain. The controller calls the Facade for `bookAppointment()` and `cancelAppointment()`; all other BookingService methods (list, get, status update) remain called directly. The Facade delegates to `BookingService`, `DoctorScheduleService`, `AsyncEmailNotificationService`, and `RedisCacheService`.

2. **Facade package**: New classes go in `pattern/facade/` to match the existing `pattern/notification/` and `pattern/sorting/` convention.

3. **No new features**: The scope is strictly patterns + SOLID + docs + tests. No new business features (fee calculation, statistics endpoints) are added.

4. **Existing patterns preserved**: All 5 existing patterns (Builder on entities, Factory Method via NotificationCreator, Adapter via NotificationSender, Strategy via DoctorSortStrategy, Observer via ApplicationEventPublisher) remain untouched.

5. **Strategy describes sorting, not fees**: The report documents Strategy as "doctor sorting strategy" since that's what the code actually does, rather than fabricating a fee-calculation strategy to match the original proposal.

6. **Documentation consolidation**: One `docs/BAO_CAO.md` file contains all 7 chapters with embedded code snippets. Diagrams are separate draw.io files. `docs/KICH_BAN_DEMO.md` is the only other doc file.

7. **Draw.io files**: 15 separate `.drawio` files under `docs/diagrams/` with English filenames. 3 system diagrams (use-case, architecture, erd) + 12 pattern diagrams (1 Class + 1 Sequence per pattern).

8. **Vietnamese language**: All documentation content is in Vietnamese. Code comments, variable names, and filenames remain in English.

## Testing Decisions

1. **Test seam**: Pattern behavior is tested at the **unit level** — each pattern's classes are instantiated directly (no Spring context) with mocks for collaborators. This matches the existing `DesignPatternTests` approach, which is the prior art.

2. **Existing tests are mapped, not duplicated**: The 28 existing test methods cover 10 of 12 required test cases. The mapping is documented in the report's testing chapter.

3. **Two new tests added**:
   - **TC03** (input validation): Tests that booking with missing patient info returns a validation error. Added as a unit test.
   - **TC08** (Facade orchestration): Tests that `AppointmentBookingFacadeImpl` calls subsystem services in the correct order. Added in a new `FacadePatternTests` class using mocks.

4. **Prior art**: `DesignPatternTests` (pure unit, no Spring context), `BookingServiceImplTests` (Mockito mocks), `BookingFlowIntegrationTests` (@SpringBootTest with H2).

## Out of Scope

- New business features (fee calculation strategy, statistics endpoints, appointment types)
- Refactoring existing pattern implementations
- Integration tests for the Facade (unit tests suffice for pattern demonstration)
- Exporting draw.io to PNG/PDF (the `.drawio` files are the deliverable)
- Docker/infrastructure setup documentation
- English translation of the report

## Further Notes

- The system runs in two modes: `demo` (H2 + in-memory notifications) and production (MySQL + RabbitMQ + SMTP). All documentation and testing targets the `demo` profile.
- The `DataInitializer` seeds 3 accounts (patient, doctor, admin) in demo mode — the demo script references these.
- Draw.io XML is generated programmatically to ensure correct UML notation; manual editing in draw.io is expected for refinement.
