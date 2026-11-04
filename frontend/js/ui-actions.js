// Explicit action allowlist. Values in data attributes are parsed as JSON, never executed.
const UiActions = Object.freeze({
  action79(event) { return onSearchChange(); },
  action80(event) { return filterTransactions(); },
  action77(event) { return changeDoctorPage(-1); },
  action78(event) { return changeDoctorPage(1); },
  action71(event) { return loadRecords(); },
  action72(event) { return loadAppointments(); },
  action73(event) { return loadDoctors(document.getElementById('specialty-select')?.value || ''); },
  action74(event) { return loadDashboardData(); },
  action75(event) { return loadSpecialties(); },
  action76(event) { return loadPendingDoctors(); },
  action68(event) { return changeUsersPage(-1); },
  action69(event) { return changeUsersPage(1); },
  action70(event) { return load(); },
  action65(event) { return changeSchedulePage(-1); },
  action66(event) { return changeSchedulePage(1); },
  action67(event) { return loadAdminSchedule(); },
  action1(event) { return Auth.redirectByRole(JSON.parse(this.dataset.uiArg0)); },
  action2(event) { return Auth.logout(); },
  action3(event) { return loadProfile(); },
  action4(event) { return toggleSlot(JSON.parse(this.dataset.uiArg0), JSON.parse(this.dataset.uiArg1)); },
  action5(event) { return enterEditMode(JSON.parse(this.dataset.uiArg3), JSON.parse(this.dataset.uiArg0), JSON.parse(this.dataset.uiArg1), JSON.parse(this.dataset.uiArg2)); },
  action6(event) { return deleteSlot(JSON.parse(this.dataset.uiArg0)); },
  action7(event) { return cancelEditMode(); },
  action8(event) { return loadSlots(); },
  action9(event) { return loadDoctorAppointments(); },
  action10(event) { return openPatientDetail(JSON.parse(this.dataset.uiArg0)); },
  action11(event) { return event.stopPropagation(); },
  action12(event) { return openRecordModal(JSON.parse(this.dataset.uiArg0)); },
  action13(event) { return changePage(1); },
  action14(event) { return changePage(JSON.parse(this.dataset.uiArg0)); },
  action15(event) { return filterByCard('ALL'); },
  action16(event) { return filterByCard('PENDING'); },
  action17(event) { return filterByCard('COMPLETED'); },
  action18(event) { return openDiagnoseFromDetail(); },
  action19(event) { return openEdit(JSON.parse(this.dataset.uiArg2), JSON.parse(this.dataset.uiArg0), JSON.parse(this.dataset.uiArg1)); },
  action20(event) { return deleteSpecialty(JSON.parse(this.dataset.uiArg1), JSON.parse(this.dataset.uiArg0)); },
  action21(event) { return selectMonth(0); },
  action22(event) { return selectMonth(JSON.parse(this.dataset.uiArg0)); },
  action23(event) { return changeTxPage(1); },
  action24(event) { return changeTxPage(JSON.parse(this.dataset.uiArg0)); },
  action25(event) { return switchChartType('bar'); },
  action26(event) { return switchChartType('line'); },
  action27(event) { return onSearchTx(); },
  action28(event) { return showDoctorSchedule(JSON.parse(this.dataset.uiArg1), JSON.parse(this.dataset.uiArg0)); },
  action29(event) { return openEdit(JSON.parse(this.dataset.uiArg0)); },
  action30(event) { return toggleStatus(JSON.parse(this.dataset.uiArg0), JSON.parse(this.dataset.uiArg1)); },
  action31(event) { return removeUser(JSON.parse(this.dataset.uiArg0)); },
  action32(event) { return openCreate(); },
  action33(event) { return selectType('DOCTOR'); },
  action34(event) { return selectType('PATIENT'); },
  action35(event) { return approve(JSON.parse(this.dataset.uiArg0)); },
  action36(event) { return reject(JSON.parse(this.dataset.uiArg0)); },
  action37(event) { return loadAnalyticsData(); },
  action38(event) { return selectMonthFilter(0); },
  action39(event) { return selectMonthFilter(JSON.parse(this.dataset.uiArg0)); },
  action40(event) { return toggleChartType('bar'); },
  action41(event) { return toggleChartType('line'); },
  action42(event) { return edit(JSON.parse(this.dataset.uiArg0)); },
  action43(event) { return disable(JSON.parse(this.dataset.uiArg0)); },
  action44(event) { return edit(); },
  action45(event) { return debouncedFilter(); },
  action46(event) { return onSpecialtyChange(this.value); },
  action47(event) { return loadDoctors(document.getElementById('specialty-select').value); },
  action48(event) { return resetFilters(); },
  action49(event) { return onPillClick('', this); },
  action50(event) { return editBooking(); },
  action51(event) { return reviewBooking(); },
  action52(event) { return openDetail(JSON.parse(this.dataset.uiArg0)); },
  action53(event) { return filterStatus('ALL'); },
  action54(event) { return filterStatus('CONFIRMED'); },
  action55(event) { return filterStatus('COMPLETED'); },
  action56(event) { return filterStatus('CANCELLED'); },
  action57(event) { return promptCancelFromDetail(); },
  action58(event) { return executeCancel(); },
  action59(event) { return openRecordDetail(JSON.parse(this.dataset.uiArg0)); },
  action60(event) { return onPillClick(JSON.parse(this.dataset.uiArg0), this); },
  action61(event) { return viewDoctorDetail(JSON.parse(this.dataset.uiArg0)); },
  action62(event) { return bookDoctor(JSON.parse(this.dataset.uiArg0)); },
  action63(event) { return selectSlot(this, JSON.parse(this.dataset.uiArg0)); },
  action64(event) { return this.parentElement.parentElement.remove(); }
});
for (const type of ['click', 'change', 'input', 'submit', 'keydown']) {
  document.addEventListener(type, event => {
    for (let element = event.target; element && element !== document; element = element.parentElement) {
      const action = element.getAttribute?.(`data-ui-${type}`);
      if (action && Object.hasOwn(UiActions, action)) {
        if (element.disabled || element.getAttribute('aria-disabled') === 'true') continue;
        try {
          const result = UiActions[action].call(element, event);
          if (result === false) event.preventDefault();
          if (result?.catch) result.catch(error => Utils.showToast(error.message || 'Thao tác chưa thành công. Vui lòng thử lại.', 'error'));
        } catch (error) { Utils.showToast(error.message || 'Thao tác chưa thành công.', 'error'); }
      }
      if (event.cancelBubble) break;
    }
  });
}
