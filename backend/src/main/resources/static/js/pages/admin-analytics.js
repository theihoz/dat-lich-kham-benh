Auth.checkAuth(['ADMIN']);
    let chartInstance = null;
    let currentData = null;
    let selectedMonth = 0; // 0 = Tất cả
    let currentChartType = 'bar';
    let loadRequestId = 0;

    function escapeHtml(value) {
      return String(value ?? '').replace(/[&<>"']/g, char => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
      }[char]));
    }

    // Init year dropdown
    const reportYearSelect = document.getElementById('reportYear');
    const curY = new Date().getFullYear();
    for (let y = curY; y >= curY - 4; y--) {
      reportYearSelect.innerHTML += `<option value="${y}">${y}</option>`;
    }
    reportYearSelect.addEventListener('change', loadAnalyticsData);

    async function loadAnalyticsData() {
      const requestId = ++loadRequestId;
      currentData = null;
      if (chartInstance) { chartInstance.destroy(); chartInstance = null; }
      ['stat-year-revenue', 'stat-year-apps', 'stat-avg-app', 'stat-avg-growth']
        .forEach(id => document.getElementById(id).innerText = '—');
      document.getElementById('missing-price-notice').innerText = '';
      document.getElementById('transactionRows').innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Đang tải phí khám...</td></tr>';
      document.getElementById('rankingRows').innerHTML = '';
      try {
        const year = reportYearSelect.value;
        const res = await Api.admin.analytics(year);
        if (requestId !== loadRequestId) return;
        if (!res.data || !Array.isArray(res.data.months) || !Array.isArray(res.data.transactions)) {
          throw new Error('Dữ liệu báo cáo không hợp lệ');
        }
        currentData = res.data;
        let detailLimitNote = document.getElementById('analytics-detail-limit-note');
        if (!detailLimitNote) {
          detailLimitNote = document.createElement('p');
          detailLimitNote.id = 'analytics-detail-limit-note';
          detailLimitNote.className = 'alert alert-info';
          detailLimitNote.textContent = 'Bảng chi tiết hiển thị tối đa 100 lịch đã khám gần nhất trong năm. Tổng và biểu đồ vẫn tính đủ dữ liệu; bộ lọc chi tiết áp dụng trên các lịch đang hiển thị.';
          document.querySelector('main').prepend(detailLimitNote);
        }
        detailLimitNote.hidden = !currentData.transactionsLimited;

        // Stat cards
        document.getElementById('stat-year-revenue').innerText = Utils.formatCurrency(currentData.yearRevenue || 0);
        document.getElementById('stat-year-apps').innerText = (currentData.yearAppointments || 0).toLocaleString('vi-VN');
        document.getElementById('stat-avg-app').innerText = Utils.formatCurrency(currentData.avgPerAppointment ?? 0);

        const growthValues = currentData.months.map(m => m.growthPercent)
          .filter(value => value != null && Number.isFinite(Number(value))).map(Number);
        document.getElementById('stat-avg-growth').innerText = growthValues.length
          ? (growthValues.reduce((sum, value) => sum + value, 0) / growthValues.length).toFixed(1) + '%'
          : '—';
        document.getElementById('missing-price-notice').innerText = currentData.completedWithoutPrice
          ? `${currentData.completedWithoutPrice} lịch hoàn thành chưa ghi phí; tổng chỉ cộng phí đã có. Phí trung bình chia cho tất cả lịch hoàn thành.`
          : '';

        // Render Month Pills
        renderMonthPills();

        // Render Chart
        renderChart();

        // Render Transactions Table
        filterTransactions();

        // Render Doctor Rankings
        renderDoctorRankings();

      } catch (err) {
        if (requestId !== loadRequestId) return;
        currentData = null;
        document.getElementById('transactionRows').innerHTML = '<tr><td colspan="6" class="text-danger text-center py-4">Không thể tải báo cáo. <button class="btn btn-sm btn-outline-primary" data-ui-click="action37">Thử lại</button></td></tr>';
        document.getElementById('rankingRows').innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Dữ liệu chưa tải được</td></tr>';
        Utils.showToast(escapeHtml(err.message || 'Lỗi tải dữ liệu báo cáo'), 'danger');
      }
    }

    function renderMonthPills() {
      const container = document.getElementById('month-pills-container');
      let html = `<button class="month-pill ${selectedMonth === 0 ? 'active' : ''}" data-ui-click="action38">Tất Cả 12 Tháng</button>`;
      for (let m = 1; m <= 12; m++) {
        html += `<button class="month-pill ${selectedMonth === m ? 'active' : ''}" data-ui-click="action39" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(m))}">Tháng ${m}</button>`;
      }
      container.innerHTML = html;
    }

    function selectMonthFilter(m) {
      selectedMonth = m;
      renderMonthPills();
      filterTransactions();

      const subtitle = document.getElementById('tx-filter-subtitle');
      if (m === 0) {
        subtitle.innerText = 'Hiển thị toàn bộ lịch hẹn đã hoàn thành trong năm; chưa xác nhận thanh toán.';
      } else {
        subtitle.innerText = `Hiển thị lịch hẹn đã hoàn thành trong Tháng ${m}; chưa xác nhận thanh toán.`;
      }
    }

    function toggleChartType(type) {
      currentChartType = type;
      document.getElementById('btn-view-bar').className = type === 'bar' ? 'btn btn-primary rounded-pill px-3 active' : 'btn btn-outline-primary rounded-pill px-3 border-0';
      document.getElementById('btn-view-line').className = type === 'line' ? 'btn btn-primary rounded-pill px-3 active' : 'btn btn-outline-primary rounded-pill px-3 border-0';
      renderChart();
    }

    function renderChart() {
      if (!currentData || !currentData.months) return;
      const ctx = document.getElementById('monthlyChart').getContext('2d');
      if (chartInstance) chartInstance.destroy();

      const labels = currentData.months.map(m => `Tháng ${m.month}`);
      const revenues = currentData.months.map(m => m.revenue);
      const appCounts = currentData.months.map(m => m.appointments);

      chartInstance = new Chart(ctx, {
        type: currentChartType,
        data: {
          labels: labels,
          datasets: [
            {
              label: 'Phí Khám Hoàn Thành (VNĐ)',
              data: revenues,
              backgroundColor: currentChartType === 'bar' ? '#0284c7' : 'rgba(2, 132, 199, 0.15)',
              borderColor: '#0284c7',
              borderWidth: 2.5,
              borderRadius: 8,
              fill: currentChartType === 'line',
              tension: 0.35,
              yAxisID: 'y'
            },
            {
              label: 'Số Lịch Hẹn',
              data: appCounts,
              type: 'line',
              borderColor: '#10b981',
              backgroundColor: '#10b981',
              borderWidth: 2,
              pointRadius: 4,
              yAxisID: 'y1'
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          onClick: (e, elements) => {
            if (elements.length > 0) {
              const index = elements[0].index;
              selectMonthFilter(index + 1);
            }
          },
          plugins: {
            legend: { position: 'top' },
            tooltip: {
              callbacks: {
                label: function(context) {
                  if (context.dataset.yAxisID === 'y') {
                    return `Phí khám hoàn thành: ${Utils.formatCurrency(context.raw)}`;
                  }
                  return `Lịch hẹn: ${context.raw}`;
                }
              }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: {
                callback: value => (value / 1000000).toLocaleString('vi-VN') + ' Tr ₫'
              }
            },
            y1: {
              beginAtZero: true,
              position: 'right',
              grid: { display: false },
              ticks: { precision: 0 }
            }
          }
        }
      });
    }

    function filterTransactions() {
      if (!currentData || !currentData.transactions) return;
      const searchKey = (document.getElementById('searchTxInput').value || '').toLowerCase().trim();
      const tbody = document.getElementById('transactionRows');

      let list = currentData.transactions;
      if (selectedMonth > 0) {
        list = list.filter(t => t.month === selectedMonth);
      }
      if (searchKey) {
        list = list.filter(t =>
          (t.patientName || '').toLowerCase().includes(searchKey) ||
          (t.patientPhone || '').includes(searchKey) ||
          (t.bookingCode || '').toLowerCase().includes(searchKey)
        );
      }

      if (list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Chưa có lịch hẹn hoàn thành phù hợp</td></tr>`;
        return;
      }

      tbody.innerHTML = list.map(t => `
        <tr>
          <td>
            <div class="font-monospace fw-bold text-primary small">#${escapeHtml(t.appointmentId)}</div>
            <small class="text-muted font-monospace">${escapeHtml(t.bookingCode)}</small>
          </td>
          <td>
            <div class="fw-bold text-dark">${escapeHtml(t.patientName)}</div>
            <small class="text-muted"><i class="bi bi-telephone me-1"></i>${escapeHtml(t.patientPhone || "Chưa cung cấp")}</small>
          </td>
          <td>
            <div class="fw-semibold text-dark small">${escapeHtml(t.doctorName)}</div>
            <span class="badge bg-light text-secondary border small">${escapeHtml(t.specialty)}</span>
          </td>
          <td>
            <div class="small fw-semibold text-dark"><i class="bi bi-clock me-1 text-primary"></i>${Utils.formatDate(t.workDate)} ${Utils.formatTime(t.startTime)}</div>
          </td>
          <td class="text-end">
            <strong class="text-success fs-6">${t.amount == null ? "Chưa ghi phí" : Utils.formatCurrency(t.amount)}</strong>
          </td>
          <td class="text-center">
            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-3 py-1">
              <i class="bi bi-check-circle-fill me-1"></i>Đã hoàn thành
            </span>
          </td>
        </tr>
      `).join('');
    }

    function renderDoctorRankings() {
      const tbody = document.getElementById('rankingRows');
      if (!currentData || !currentData.doctorRankings || currentData.doctorRankings.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Chưa có dữ liệu bác sĩ</td></tr>`;
        return;
      }

      tbody.innerHTML = currentData.doctorRankings.map(d => `
        <tr>
          <td class="fw-bold text-dark"><i class="bi bi-person-badge text-primary me-2"></i>${escapeHtml(d.doctorName)}</td>
          <td><span class="badge bg-primary-subtle text-primary fw-semibold px-3 py-1">${escapeHtml(d.specialty)}</span></td>
          <td class="text-center fw-semibold text-primary">${d.confirmed}</td>
          <td class="text-center fw-semibold text-success">${d.completed}</td>
          <td class="text-center fw-semibold text-danger">${d.cancelled}</td>
          <td class="text-end fw-bold text-success fs-6">${Utils.formatCurrency(d.revenue)}</td>
        </tr>
      `).join('');
    }

    loadAnalyticsData();
