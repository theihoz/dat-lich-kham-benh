# 01: Implement Facade pattern with tests

**What to build:** Add `AppointmentBookingFacade` as the 6th design pattern. The Facade provides a simplified interface (`bookAppointment`, `cancelAppointment`) that orchestrates the booking subsystem. The controller calls the Facade for create/cancel; all other BookingService methods remain direct. Include a unit test proving the Facade delegates to subsystem services in order, and a test for input validation (TC03). After this ticket, `mvn test` passes with the new Facade wired in and all existing tests still green.

**Blocked by:** None (can start immediately).

**Status:** completed

- [ ] `AppointmentBookingFacade` interface created in `pattern/facade/` with `bookAppointment` and `cancelAppointment`
- [ ] `AppointmentBookingFacadeImpl` implementation delegates to `BookingService` and other services
- [ ] `BookingController` create/cancel endpoints use the Facade instead of BookingService directly
- [ ] `FacadePatternTests` unit test verifies delegation order with mocks
- [ ] Input validation test (TC03) added — booking with null scheduleId returns validation error
- [ ] `mvn test` passes (all existing + new tests)
