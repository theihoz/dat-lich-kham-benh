Auth.checkAuth(['ADMIN']);

    let chartInstance = null;
    let analyticsData = null;
    let selectedMonth = 0; // 0 = Tất cả
    let currentChartType = 'bar';
    let txCurrentPage = 1;
    const txPageSize = 15;

    function formatVND(val) {
      const n = Number(val) || 0;
      return n.toLocaleString('vi-VN') + ' ₫';
    }

    function getMethodBadge(method) {
      switch(method) {
        case 'VietQR Banking': return '<span class="badge badge-method method-vietqr"><i class="bi bi-qr-code-scan me-1"></i>VietQR</span>';
        case 'Ví MoMo': return '<span class="badge badge-method method-momo"><i class="bi bi-wallet2 me-1"></i>MoMo</span>';
        case 'ZaloPay': return '<span class="badge badge-method method-zalopay"><i class="bi bi-wallet-fill me-1"></i>ZaloPay</span>';
        case 'Thẻ ATM/Visa': return '<span class="badge badge-method method-card"><i class="bi bi-credit-card me-1"></i>Thẻ ATM</span>';
        default: return '<span class="badge badge-method method-cash"><i class="bi bi-cash me-1"></i>Tiền mặt</span>';
      }
    }

    async function loadDashboardData() {
      const feedback = document.getElementById('dashboard-feedback');
      feedback.textContent = 'Đang tải tổng quan...';
      try {
        // Load basic stats
        const resStats = await Api.admin.stats();
        const stats = resStats.data || {};
        document.getElementById('stat-doctors').innerText = (stats.totalDoctors || 0).toLocaleString('vi-VN');
        document.getElementById('stat-patients').innerText = (stats.totalPatients || 0).toLocaleString('vi-VN');
        document.getElementById('stat-appointments').innerText = (stats.totalAppointments || 0).toLocaleString('vi-VN');

        // Load analytics & transaction detailed data
        const year = new Date().getFullYear();
        const resAnalytics = await Api.admin.analytics(year);
        analyticsData = resAnalytics.data || {};

        document.getElementById('stat-revenue').innerText = formatVND(analyticsData.yearRevenue || stats.totalRevenue || 0);

        // Render Month Pills
        renderMonthPills();

        // Render Chart
        renderRevenueChart();

        // Render Transactions Table
        renderTxTable();

        feedback.textContent = '';
      } catch (err) {
        feedback.innerHTML = `${Utils.escapeHtml(err.message)} <button type="button" class="btn btn-outline-primary" data-ui-click="action74">Thử lại</button>`;
      }
    }

    function renderMonthPills() {
      const container = document.getElementById('month-pills-container');
      let html = `<button class="month-pill ${selectedMonth === 0 ? 'active' : ''}" data-ui-click="action21">Tất Cả 12 Tháng</button>`;
      for (let m = 1; m <= 12; m++) {
        html += `<button class="month-pill ${selectedMonth === m ? 'active' : ''}" data-ui-click="action22" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(m))}">Tháng ${m}</button>`;
      }
      container.innerHTML = html;
    }

    function selectMonth(m) {
      selectedMonth = m;
      txCurrentPage = 1;
      renderMonthPills();
      renderTxTable();

      const subtitle = document.getElementById('tx-subtitle');
      if (m === 0) {
        subtitle.innerText = 'Hiển thị toàn bộ các lịch hẹn hoàn thành trong năm';
      } else {
        subtitle.innerText = `Hiển thị danh sách bệnh nhân có lịch hẹn hoàn thành trong Tháng ${m}`;
      }
    }

    function switchChartType(type) {
      currentChartType = type;
      document.getElementById('btn-chart-bar').className = type === 'bar' ? 'btn btn-primary rounded-pill px-3 active' : 'btn btn-outline-primary rounded-pill px-3 border-0';
      document.getElementById('btn-chart-line').className = type === 'line' ? 'btn btn-primary rounded-pill px-3 active' : 'btn btn-outline-primary rounded-pill px-3 border-0';
      renderRevenueChart();
    }

    function renderRevenueChart() {
      const months = (analyticsData && analyticsData.months) ? analyticsData.months : [];
      const loader = document.getElementById('chart-loading');
      const canvas = document.getElementById('revenueChart');

      if (!months.length) {
        if (loader) loader.innerHTML = '<span class="text-muted">Không có dữ liệu biểu đồ</span>';
        return;
      }

      if (loader) loader.style.display = 'none';
      if (canvas) canvas.style.display = 'block';

      const labels = months.map(m => 'Tháng ' + m.month);
      const revenues = months.map(m => Number(m.revenue) || 0);
      const appCounts = months.map(m => Number(m.appointments) || 0);

      if (chartInstance) {
        chartInstance.destroy();
        chartInstance = null;
      }

      chartInstance = new Chart(canvas, {
        type: currentChartType,
        data: {
          labels: labels,
          datasets: [
            {
              label: 'Phí Khám Hoàn Thành (VNĐ)',
              data: revenues,
              backgroundColor: currentChartType === 'bar' ? 'rgba(2, 132, 199, 0.85)' : 'rgba(2, 132, 199, 0.15)',
              borderColor: '#0284c7',
              borderWidth: 2,
              borderRadius: currentChartType === 'bar' ? 6 : 0,
              fill: currentChartType === 'line',
              tension: 0.35,
              yAxisID: 'y'
            },
            {
              label: 'Số Lượt Khám',
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
              selectMonth(index + 1);
            }
          },
          plugins: {
            legend: { position: 'top' },
            tooltip: {
              callbacks: {
                label: function(context) {
                  if (context.dataset.yAxisID === 'y') {
                    return 'Phí khám hoàn thành: ' + formatVND(context.raw);
                  }
                  return 'Lượt khám: ' + context.raw + ' ca';
                }
              }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: {
                callback: value => (value / 1000000).toFixed(0) + ' Tr ₫'
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

    function onSearchTx() {
      txCurrentPage = 1;
      renderTxTable();
    }

    function renderTxTable() {
      if (!analyticsData || !analyticsData.transactions) return;
      const searchKey = (document.getElementById('searchTxInput').value || '').toLowerCase().trim();
      const tbody = document.getElementById('transactionRows');

      let list = analyticsData.transactions || [];
      if (selectedMonth > 0) {
        list = list.filter(t => t.month === selectedMonth);
      }
      if (searchKey) {
        list = list.filter(t =>
          (t.patientName || '').toLowerCase().includes(searchKey) ||
          (t.transactionId || '').toLowerCase().includes(searchKey) ||
          (t.patientPhone || '').includes(searchKey) ||
          (t.bookingCode || '').toLowerCase().includes(searchKey)
        );
      }

      const totalItems = list.length;
      const totalPages = Math.ceil(totalItems / txPageSize) || 1;
      if (txCurrentPage > totalPages) txCurrentPage = totalPages;

      const startIndex = (txCurrentPage - 1) * txPageSize;
      const endIndex = Math.min(startIndex + txPageSize, totalItems);
      const pageItems = list.slice(startIndex, endIndex);

      document.getElementById('tx-pagination-info').innerText = `Hiển thị ${totalItems > 0 ? startIndex + 1 : 0} - ${endIndex} trong tổng số ${totalItems.toLocaleString('vi-VN')} lịch hẹn hoàn thành`;

      if (pageItems.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Không tìm thấy lịch hẹn hoàn thành phù hợp</td></tr>`;
        renderTxPagination(0);
        return;
      }

      tbody.innerHTML = pageItems.map(t => `
        <tr>
          <td>
            <div class="font-monospace fw-bold text-primary small">${Utils.escapeHtml(t.appointmentId)}</div>
            <small class="text-muted font-monospace">${Utils.escapeHtml(t.bookingCode)}</small>
          </td>
          <td>
            <div class="fw-bold text-dark">${Utils.escapeHtml(t.patientName)}</div>
            <small class="text-muted"><i class="bi bi-telephone me-1"></i>${Utils.escapeHtml(t.patientPhone)}</small>
          </td>
          <td>
            <div class="fw-semibold text-dark small">${Utils.escapeHtml(t.doctorName)}</div>
            <span class="badge bg-light text-secondary border small">${Utils.escapeHtml(t.specialty)}</span>
          </td>
          <td>
            <div class="small fw-semibold text-dark"><i class="bi bi-clock me-1 text-primary"></i>${Utils.formatDate(t.workDate)} ${Utils.formatTime(t.startTime)}</div>
          </td>
          <td><span class="text-muted small">Chưa xác nhận thanh toán</span></td>
          <td class="text-end">
            <strong class="text-success fs-6">${formatVND(t.amount)}</strong>
          </td>
          <td class="text-center">
            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-3 py-1">
              <i class="bi bi-check-circle-fill me-1"></i>Đã hoàn thành
            </span>
          </td>
        </tr>
      `).join('');

      renderTxPagination(totalPages);
    }

    function renderTxPagination(totalPages) {
      const controls = document.getElementById('tx-pagination-controls');
      if (totalPages <= 1) {
        controls.innerHTML = '';
        return;
      }

      let html = `
        <li class="page-item ${txCurrentPage === 1 ? 'disabled' : ''}">
          <button class="page-link" data-ui-click="action23"><i class="bi bi-chevron-double-left"></i></button>
        </li>
        <li class="page-item ${txCurrentPage === 1 ? 'disabled' : ''}">
          <button class="page-link" data-ui-click="action24" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(txCurrentPage - 1))}"><i class="bi bi-chevron-left"></i></button>
        </li>
      `;

      let startPage = Math.max(1, txCurrentPage - 2);
      let endPage = Math.min(totalPages, txCurrentPage + 2);

      for (let p = startPage; p <= endPage; p++) {
        html += `
          <li class="page-item ${p === txCurrentPage ? 'active' : ''}">
            <button class="page-link" data-ui-click="action24" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(p))}">${p}</button>
          </li>
        `;
      }

      html += `
        <li class="page-item ${txCurrentPage === totalPages ? 'disabled' : ''}">
          <button class="page-link" data-ui-click="action24" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(txCurrentPage + 1))}"><i class="bi bi-chevron-right"></i></button>
        </li>
        <li class="page-item ${txCurrentPage === totalPages ? 'disabled' : ''}">
          <button class="page-link" data-ui-click="action24" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(totalPages))}"><i class="bi bi-chevron-double-right"></i></button>
        </li>
      `;

      controls.innerHTML = html;
    }

    function changeTxPage(page) {
      txCurrentPage = page;
      renderTxTable();
    }

    loadDashboardData();
