# 08: Final verification and cleanup

**What to build:** End-to-end verification that everything works together. Run `mvn test` and confirm all tests pass. Start the demo app and verify the Facade endpoints work. Review `BAO_CAO.md` against the grading rubric (0.5 + 1.5 + 2.5 + 1.5 = 6.0 points). Check all drawio files exist and are referenced correctly. Ensure no broken links in documentation.

**Blocked by:** 07-report-chapters-5-7-demo (all content must be complete).

**Status:** completed

- [ ] `mvn test` passes (all tests including new Facade + TC03)
- [ ] Demo app starts with `mvn spring-boot:run -Dspring-boot.run.profiles=demo`
- [ ] Facade booking endpoint works via API call
- [ ] All 15 drawio files exist under `docs/diagrams/`
- [ ] `BAO_CAO.md` has all 7 chapters with no placeholder content
- [ ] `KICH_BAN_DEMO.md` is complete
- [ ] All diagram references in the report resolve to existing files
