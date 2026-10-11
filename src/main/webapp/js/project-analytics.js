/**
 * =========================================================================
 * TEAMWORK HUB — PROJECT ANALYTICS & BURNDOWN ENGINE (CHART.JS 4.4+)
 * =========================================================================
 * Điều phối biểu đồ Burndown tiến độ, phân tích chất lượng nghiệm thu (FTPR),
 * vận tốc duyệt của Trưởng Dự Án và làm mới dữ liệu AJAX thời gian thực.
 */

(function () {
    'use strict';

    let burndownChartInstance = null;
    let qualityChartInstance = null;

    function isDarkTheme() {
        const theme = document.documentElement.getAttribute('data-bs-theme') ||
                      document.documentElement.getAttribute('data-theme');
        return theme === 'dark';
    }

    function getThemeColors() {
        const dark = isDarkTheme();
        return {
            textColor: dark ? '#cbd5e1' : '#475569',
            gridColor: dark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.06)',
            tooltipBg: dark ? '#1e293b' : '#0f172a',
            tooltipText: '#ffffff',
            idealBorder: dark ? 'rgba(148, 163, 184, 0.7)' : 'rgba(100, 116, 139, 0.7)',
            actualBorder: '#2563eb',
            actualBg: dark ? 'rgba(37, 99, 235, 0.2)' : 'rgba(37, 99, 235, 0.08)',
            passGreen: '#10b981',
            reviseAmber: '#f59e0b',
            pendingSlate: '#94a3b8'
        };
    }

    /**
     * 1. Khởi tạo Biểu đồ Burndown
     */
    function initBurndownChart(burndownData) {
        const canvas = document.getElementById('burndownChart');
        if (!canvas || !window.Chart) return;

        if (burndownChartInstance) {
            burndownChartInstance.destroy();
            burndownChartInstance = null;
        }

        if (!burndownData || !burndownData.labels || burndownData.labels.length === 0) {
            return;
        }

        const colors = getThemeColors();
        const ctx = canvas.getContext('2d');

        burndownChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: burndownData.labels,
                datasets: [
                    {
                        label: 'Lý tưởng (Sprint Goal)',
                        data: burndownData.idealRemaining,
                        borderColor: colors.idealBorder,
                        borderWidth: 2,
                        borderDash: [6, 6],
                        pointRadius: 1,
                        pointHoverRadius: 4,
                        fill: false,
                        tension: 0
                    },
                    {
                        label: 'Thực tế còn lại',
                        data: burndownData.actualRemaining,
                        borderColor: colors.actualBorder,
                        backgroundColor: colors.actualBg,
                        borderWidth: 3,
                        pointRadius: 3.5,
                        pointHoverRadius: 6,
                        pointBackgroundColor: colors.actualBorder,
                        fill: true,
                        tension: 0.25,
                        spanGaps: false // Dừng vẽ khi gặp null (ngày trong tương lai)
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: {
                        position: 'top',
                        align: 'end',
                        labels: {
                            color: colors.textColor,
                            font: {
                                family: "'Inter', sans-serif",
                                size: 12,
                                weight: '500'
                            },
                            usePointStyle: true,
                            boxWidth: 8,
                            padding: 16
                        }
                    },
                    tooltip: {
                        backgroundColor: colors.tooltipBg,
                        titleColor: colors.tooltipText,
                        bodyColor: colors.tooltipText,
                        titleFont: { family: "'Inter', sans-serif", size: 12, weight: '600' },
                        bodyFont: { family: "'Inter', sans-serif", size: 12 },
                        padding: 10,
                        cornerRadius: 8,
                        callbacks: {
                            title: function (items) {
                                return items.length ? 'Ngày: ' + items[0].label : '';
                            },
                            label: function (context) {
                                const val = context.parsed.y;
                                if (val === null || val === undefined) return null;
                                return ' ' + context.dataset.label + ': ' + val + ' công việc';
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: {
                            color: colors.gridColor,
                            drawBorder: false
                        },
                        ticks: {
                            color: colors.textColor,
                            font: { family: "'Inter', sans-serif", size: 11 },
                            maxRotation: 0,
                            autoSkip: true,
                            maxTicksLimit: 14
                        }
                    },
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: colors.gridColor,
                            drawBorder: false
                        },
                        ticks: {
                            color: colors.textColor,
                            font: { family: "'Inter', sans-serif", size: 11 },
                            precision: 0
                        },
                        title: {
                            display: true,
                            text: 'Công việc còn lại',
                            color: colors.textColor,
                            font: { family: "'Inter', sans-serif", size: 11, weight: '500' }
                        }
                    }
                }
            }
        });
    }

    /**
     * 2. Khởi tạo Biểu đồ Donut Chất lượng (First-time Pass vs Revised)
     */
    function initQualityDonutChart(qualityData) {
        const canvas = document.getElementById('qualityDonutChart');
        if (!canvas || !window.Chart) return;

        if (qualityChartInstance) {
            qualityChartInstance.destroy();
            qualityChartInstance = null;
        }

        if (!qualityData) return;

        const pass = qualityData.firstTimePassedCount || 0;
        const revised = qualityData.revisionRequiredCount || 0;
        const pending = qualityData.pendingReviewCount || 0;

        // Nếu chưa có lượt thẩm định nào
        if (pass === 0 && revised === 0 && pending === 0) {
            const ctx = canvas.getContext('2d');
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.font = "13px 'Inter', sans-serif";
            ctx.fillStyle = "#94a3b8";
            ctx.textAlign = "center";
            ctx.fillText("Chưa có lượt thẩm định nhiệm vụ nào", canvas.width / 2, canvas.height / 2);
            return;
        }

        const colors = getThemeColors();
        const ctx = canvas.getContext('2d');

        qualityChartInstance = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Đạt ngay lần đầu (Rev 1)', 'Cần chỉnh sửa (Rev 2+)', 'Chờ PM duyệt'],
                datasets: [
                    {
                        data: [pass, revised, pending],
                        backgroundColor: [colors.passGreen, colors.reviseAmber, colors.pendingSlate],
                        borderWidth: 2,
                        borderColor: isDarkTheme() ? '#1e293b' : '#ffffff',
                        hoverOffset: 4
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '72%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            color: colors.textColor,
                            font: { family: "'Inter', sans-serif", size: 11 },
                            usePointStyle: true,
                            boxWidth: 8,
                            padding: 12
                        }
                    },
                    tooltip: {
                        backgroundColor: colors.tooltipBg,
                        titleColor: colors.tooltipText,
                        bodyColor: colors.tooltipText,
                        cornerRadius: 8,
                        padding: 8,
                        callbacks: {
                            label: function (context) {
                                const total = pass + revised + pending;
                                const val = context.parsed;
                                const pct = total > 0 ? Math.round((val / total) * 100) : 0;
                                return ' ' + context.label + ': ' + val + ' việc (' + pct + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * 3. Hàm Làm mới dữ liệu qua AJAX (Live Refresh)
     */
    window.refreshAnalyticsData = function () {
        const config = window.ANALYTICS_CONFIG;
        if (!config || !config.projectId) return;

        const btn = document.getElementById('btnRefreshAnalytics');
        const spinner = document.getElementById('refreshSpinnerIcon');

        if (btn) btn.disabled = true;
        if (spinner) {
            spinner.classList.add('spinner-border', 'spinner-border-sm');
            spinner.classList.remove('bi-arrow-clockwise');
        }

        const url = (config.contextPath || '') + '/project-analytics?action=data&projectId=' + config.projectId;

        fetch(url, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(function (res) {
            if (!res.ok) throw new Error('Mã phản hồi HTTP: ' + res.status);
            return res.json();
        })
        .then(function (data) {
            if (!data || !data.success) throw new Error('Dữ liệu API không hợp lệ');

            // Cập nhật lại các đối tượng biểu đồ
            if (data.burndown) {
                config.burndownData = data.burndown;
                initBurndownChart(data.burndown);
                // Cập nhật KPI Burndown
                const elCompRate = document.getElementById('kpiCompletionRate');
                if (elCompRate) elCompRate.textContent = data.burndown.completionRate + '%';
            }

            if (data.quality) {
                config.qualityData = data.quality;
                initQualityDonutChart(data.quality);
                // Cập nhật KPI Health & Pass Rate
                const elHealth = document.getElementById('kpiHealthScore');
                if (elHealth) elHealth.textContent = data.quality.healthScore;
                const elPass = document.getElementById('kpiPassRate');
                if (elPass) elPass.textContent = data.quality.firstTimePassRate + '%';
                const elAvgHours = document.getElementById('kpiAvgHours');
                if (elAvgHours && data.quality.avgResponseHours > 0) {
                    elAvgHours.textContent = data.quality.avgResponseHours;
                }
            }

            if (window.showToast) {
                window.showToast('✅ Đã cập nhật chỉ số phân tích mới nhất!', 'success');
            }
        })
        .catch(function (err) {
            console.error('Lỗi khi làm mới phân tích qua AJAX:', err);
            if (window.showToast) {
                window.showToast('⚠️ Không thể tải dữ liệu mới nhất: ' + err.message, 'danger');
            }
        })
        .finally(function () {
            if (btn) btn.disabled = false;
            if (spinner) {
                spinner.classList.remove('spinner-border', 'spinner-border-sm');
                spinner.classList.add('bi-arrow-clockwise');
            }
        });
    };

    /**
     * 4. Lắng nghe thay đổi Theme (Light/Dark) để đổi màu biểu đồ đồng bộ
     */
    function attachThemeObserver() {
        const observer = new MutationObserver(function (mutations) {
            mutations.forEach(function (mutation) {
                if (mutation.attributeName === 'data-bs-theme' || mutation.attributeName === 'data-theme') {
                    const config = window.ANALYTICS_CONFIG || {};
                    if (config.burndownData) initBurndownChart(config.burndownData);
                    if (config.qualityData) initQualityDonutChart(config.qualityData);
                }
            });
        });
        observer.observe(document.documentElement, { attributes: true });
    }

    // Khởi chạy khi DOM sẵn sàng
    document.addEventListener('DOMContentLoaded', function () {
        const config = window.ANALYTICS_CONFIG || {};
        if (config.burndownData) {
            initBurndownChart(config.burndownData);
        }
        if (config.qualityData) {
            initQualityDonutChart(config.qualityData);
        }
        attachThemeObserver();
    });

})();
