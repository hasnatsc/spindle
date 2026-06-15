/* Fabric Pre-Costing Control Tower v3 — Scripts */
/* ── THEME & SIDEBAR ── */
    function getTheme() { return document.documentElement.getAttribute('data-theme'); }
    function toggleTheme() {
      const dark = getTheme() === 'dark';
      document.documentElement.setAttribute('data-theme', dark ? 'light' : 'dark');
      document.getElementById('themeKnob').textContent = dark ? '☀️' : '🌙';
      buildCharts();
    }
    function openSidebar() {
      document.getElementById('sidebar').classList.add('open');
      const ov = document.getElementById('overlay');
      ov.style.display = 'block';
      requestAnimationFrame(() => ov.classList.add('show'));
      document.body.style.overflow = 'hidden';
    }
    function closeSidebar() {
      document.getElementById('sidebar').classList.remove('open');
      const ov = document.getElementById('overlay');
      ov.classList.remove('show');
      setTimeout(() => { ov.style.display = 'none'; }, 240);
      document.body.style.overflow = '';
    }
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeSidebar(); });
    document.querySelectorAll('.sb-item').forEach(item => { item.addEventListener('click', () => { if (window.innerWidth < 992) closeSidebar(); }); });

    /* ── REFRESH ── */
    document.getElementById('refreshBtn').addEventListener('click', function () {
      const orig = this.textContent;
      this.textContent = '↻ Refreshing...'; this.style.pointerEvents = 'none';
      setTimeout(() => {
        this.textContent = orig; this.style.pointerEvents = '';
        const n = new Date();
        document.getElementById('lastRefreshed').innerHTML =
          '🕐 Last Refreshed: ' + n.toLocaleString('en-US', { day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' }) + ' &nbsp;·&nbsp; Data source: HASSML ERP Live Feed';
      }, 1200);
    });

    /* ── CHARTS (GMD pattern: destroy+rebuild) ── */
    let charts = {};
    const GN = '#059669', RD = '#e11d48', YL = '#d97706', OR = '#ea580c', BL = '#2563eb', PU = '#7c3aed', AC = '#00c8ff', TE = '#0e7490';

    function buildCharts() {
      Object.values(charts).forEach(c => { try { c.destroy(); } catch (e) { } });
      charts = {};
      const dark = getTheme() === 'dark';
      const gridC = dark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.06)';
      const textC = dark ? '#7a90aa' : '#5a6b80';
      Chart.defaults.color = textC;
      Chart.defaults.borderColor = gridC;
      Chart.defaults.font.family = "'IBM Plex Sans', sans-serif";
      Chart.defaults.font.size = 12;
      const base = { responsive: true, maintainAspectRatio: false };
      const teams = ['Las Vegas', 'Paris', 'San Francisco', 'Munich', 'Sydney', 'London', 'Barcelona', 'Tokyo', 'Morocco'];

      /* Team Target vs Projection */
      charts.tp = new Chart(document.getElementById('chartTargetProj'), {
        type: 'bar',
        data: {
          labels: teams,
          datasets: [
            { label: 'Target', data: [320000, 95000, 180000, 160000, 120000, 170000, 140000, 160000, 90000], backgroundColor: 'rgba(37,99,235,0.65)', borderColor: BL, borderWidth: 1, borderRadius: 3, barPercentage: 0.45, categoryPercentage: 0.85 },
            { label: 'Projection', data: [400000, 110000, 215000, 182000, 130000, 197000, 165000, 170000, 93000], backgroundColor: 'rgba(16,185,129,0.65)', borderColor: GN, borderWidth: 1, borderRadius: 3, barPercentage: 0.45, categoryPercentage: 0.85 }
          ]
        },
        options: {
          ...base,
          plugins: {
            legend: { display: true, position: 'top', labels: { boxWidth: 9, padding: 7, color: textC } },
            tooltip: { callbacks: { label: ctx => `${ctx.dataset.label}: ${(ctx.raw / 1000).toFixed(0)}k yds` } }
          },
          scales: {
            x: { ticks: { color: textC, font: { size: 11 }, maxRotation: 30 }, grid: { color: gridC } },
            y: { ticks: { color: textC, callback: v => `${(v / 1000).toFixed(0)}k` }, grid: { color: gridC } }
          }
        }
      });

      /* Coordinator Donut */
      charts.donut = new Chart(document.getElementById('chartDonut'), {
        type: 'doughnut',
        data: {
          labels: ['Tonoy', 'Rasel', 'Masud', 'Morocco'],
          datasets: [{ data: [1770000, 1280000, 650000, 150000], backgroundColor: [BL, GN, '#a855f7', '#f59e0b'], borderWidth: 0, hoverOffset: 4 }]
        },
        options: {
          responsive: false, maintainAspectRatio: false, cutout: '68%',
          plugins: { legend: { display: false }, tooltip: { callbacks: { label: c => ` ${c.label}: ${(c.raw / 1000).toFixed(0)}k yds` } } }
        }
      });

      /* Projection Trend */
      charts.pt = new Chart(document.getElementById('chartProjTrend'), {
        type: 'bar',
        data: {
          labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
          datasets: [
            { label: 'Target (M)', data: [3.1, 3.2, 3.3, 3.42, 3.42, 3.6], type: 'line', borderColor: YL, borderWidth: 1.5, pointRadius: 3, fill: false, tension: 0.3 },
            { label: 'Projection (M)', data: [2.8, 3.0, 3.2, 3.3, 3.24, 3.85], backgroundColor: 'rgba(0,200,255,0.55)', borderColor: AC, borderWidth: 1, borderRadius: 3 }
          ]
        },
        options: {
          ...base,
          plugins: { legend: { display: true, position: 'top', labels: { boxWidth: 9, padding: 6, color: textC } } },
          scales: {
            x: { grid: { display: false }, ticks: { color: textC } },
            y: { grid: { color: gridC }, ticks: { color: textC, callback: v => `${v}M` } }
          }
        }
      });

      /* Profit Trend */
      charts.pft = new Chart(document.getElementById('chartProfitTrend'), {
        type: 'bar',
        data: {
          labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
          datasets: [{ label: 'Profit (USD)', data: [28000, 35000, 42000, 51000, 75350, 142680], backgroundColor: 'rgba(16,185,129,0.65)', borderColor: GN, borderWidth: 1, borderRadius: 4 }]
        },
        options: {
          ...base,
          plugins: { legend: { display: false }, tooltip: { callbacks: { label: ctx => `$${ctx.raw.toLocaleString()}` } } },
          scales: {
            x: { grid: { display: false }, ticks: { color: textC } },
            y: { grid: { color: gridC }, ticks: { color: textC, callback: v => `$${(v / 1000).toFixed(0)}k` } }
          }
        }
      });

      /* Growth Overview sparkline */
      charts.go = new Chart(document.getElementById('chartGrowthOverview'), {
        type: 'line',
        data: {
          labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
          datasets: [
            { label: 'Projection', data: [2.8, 3.0, 3.2, 3.3, 3.24, 3.85], borderColor: AC, borderWidth: 2, pointRadius: 2, tension: 0.35, fill: true, backgroundColor: 'rgba(0,200,255,0.08)' },
            { label: 'Approved', data: [2.3, 2.5, 2.6, 2.7, 2.36, 2.95], borderColor: GN, borderWidth: 1.5, pointRadius: 2, tension: 0.35 }
          ]
        },
        options: {
          ...base,
          plugins: { legend: { display: true, position: 'top', labels: { boxWidth: 8, padding: 5, color: textC, font: { size: 11 } } } },
          scales: {
            x: { ticks: { color: textC, font: { size: 11 } }, grid: { color: gridC } },
            y: { ticks: { color: textC, font: { size: 11 }, callback: v => `${v}M` }, grid: { color: gridC } }
          }
        }
      });
    }

    buildCharts();