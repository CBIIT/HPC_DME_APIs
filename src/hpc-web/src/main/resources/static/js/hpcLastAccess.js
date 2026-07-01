/**
 * hpcLastAccess.js
 *
 * JavaScript for the Last Accessed Collection Report.
 * Provides pie chart (summary) and bar chart (drill-down by subfolder)
 * using Chart.js, backed by AJAX calls to the hpc-web controller.
 */

(function () {
    'use strict';

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    var currentBasePath = '';
    var currentPath = '';
    var pieChart = null;
    var barChart = null;

    // -------------------------------------------------------------------------
    // Color map for stale-access buckets (order 1-4)
    // -------------------------------------------------------------------------
    var BUCKET_COLORS = {
        1: '#28a745', // Green: accessed within 90 days
        2: '#ffc107', // Yellow: 90-180 days
        3: '#dc3545', // Red: 180-365 days
        4: '#7b1010'  // Dark red: over 365 days
    };

    var BUCKET_LABELS = {
        1: 'Green: accessed within 90 days',
        2: 'Yellow: 90-180 days',
        3: 'Red: 180-365 days',
        4: 'Dark red: over 365 days'
    };

    var BUCKET_ORDER = [1, 2, 3, 4];

    function pad2(num) {
        return (num < 10 ? '0' : '') + num;
    }

    function formatDateYYYYMMDD(date) {
        return date.getFullYear() + '-' + pad2(date.getMonth() + 1) + '-' + pad2(date.getDate());
    }

    function formatDateMMDDYYYY(date) {
        return pad2(date.getMonth() + 1) + '/' + pad2(date.getDate()) + '/' + date.getFullYear();
    }

    // Returns inclusive date range based on stale bucket order.
    function getDateRangeForBucket(bucketOrder) {
        var now = new Date();
        var end = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        var start;

        if (bucketOrder === 1) {
            start = new Date(end);
            start.setDate(start.getDate() - 90);
            return { from: formatDateMMDDYYYY(start), to: formatDateMMDDYYYY(end) };
        }

        if (bucketOrder === 2) {
            start = new Date(end);
            start.setDate(start.getDate() - 180);
            var to2 = new Date(end);
            to2.setDate(to2.getDate() - 91);
            return { from: formatDateMMDDYYYY(start), to: formatDateMMDDYYYY(to2) };
        }

        if (bucketOrder === 3) {
            start = new Date(end);
            start.setDate(start.getDate() - 365);
            var to3 = new Date(end);
            to3.setDate(to3.getDate() - 181);
            return { from: formatDateMMDDYYYY(start), to: formatDateMMDDYYYY(to3) };
        }

        // bucket 4: older than 365 days
        var to4 = new Date(end);
        to4.setDate(to4.getDate() - 366);
        return { from: '', to: formatDateMMDDYYYY(to4) };
    }

    function submitLastAccessReportSearch(path, bucketOrder) {
        var range = getDateRangeForBucket(bucketOrder);
        var actionPath = '/reports';

        var form = document.createElement('form');
        form.method = 'POST';
        form.action = actionPath;
        form.style.display = 'none';

        var add = function (name, value) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = name;
            input.value = value;
            form.appendChild(input);
        };

        // Report identity
        add('report', 'LAST_ACCESS_DATA_OBJECT_REPORT');
        add('reportType', 'LAST_ACCESS_DATA_OBJECT_REPORT');

        // Path criteria (common aliases used by existing forms/controllers)
        add('path', path);
        add('basepath', currentBasePath);

        // Date criteria aliases
        add('fromDate', range.from);
        add('toDate', range.to);

        document.body.appendChild(form);
        form.submit();
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------
    $(document).ready(function () {
        // Initialize Select2 dropdown
        $('#basePathSelect').select2({
            placeholder: '-----------------Select Base Path-----------------',
            allowClear: true,
            width: '500px'
        });

        $('#basePathSelect').on('change', function () {
            var selected = $(this).val();
            if (!selected) {
                $('#chartsRow').hide();
                $('#breadcrumbContainer').hide();
                return;
            }
            currentBasePath = selected;
            currentPath = selected;
            loadCharts(currentBasePath, currentPath);
        });
    });

    // -------------------------------------------------------------------------
    // Load both charts for the given basePath and currentPath
    // -------------------------------------------------------------------------
    function loadCharts(basePath, path) {
        $('#chartsRow').show();
        updateBreadcrumb(path);
        fetchPieData(basePath, path);
        fetchBarData(basePath, path);
    }

    // -------------------------------------------------------------------------
    // Fetch pie chart data
    // -------------------------------------------------------------------------
    function fetchPieData(basePath, path) {
        $('#pieLoading').show();
        $.ajax({
            url: '/lastAccess/pieChartData',
            method: 'GET',
            data: { basePath: basePath, currentPath: path },
            dataType: 'json',
            success: function (data) {
                $('#pieLoading').hide();
                if (data && data.error) {
                    console.error('Pie chart error:', data.error);
                    return;
                }
                renderPieChart(data);
            },
            error: function (xhr, status, err) {
                $('#pieLoading').hide();
                console.error('Failed to fetch pie chart data:', status, err);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Fetch bar chart data
    // -------------------------------------------------------------------------
    function fetchBarData(basePath, path) {
        $('#barLoading').show();
        $('#leafMessage').hide();
        $.ajax({
            url: '/lastAccess/barChartData',
            method: 'GET',
            data: { basePath: basePath, currentPath: path },
            dataType: 'json',
            success: function (data) {
                $('#barLoading').hide();
                if (data && data.error) {
                    console.error('Bar chart error:', data.error);
                    return;
                }
                renderBarChart(data);
            },
            error: function (xhr, status, err) {
                $('#barLoading').hide();
                console.error('Failed to fetch bar chart data:', status, err);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Render pie chart
    // -------------------------------------------------------------------------
    function renderPieChart(data) {
        var entries = (data && data.pieChartEntries) ? data.pieChartEntries : [];

        // Sort by bucket order
        entries.sort(function (a, b) { return a.bucketOrder - b.bucketOrder; });

        var labels = entries.map(function (e) {
            return e.bucketLabel + ' (' + e.percentage + '%)';
        });
        var counts = entries.map(function (e) { return e.fileCount; });
        var colors = entries.map(function (e) { return BUCKET_COLORS[e.bucketOrder] || '#999'; });

        if (pieChart) {
            pieChart.destroy();
            pieChart = null;
        }

        var ctx = document.getElementById('stalePieChart').getContext('2d');
        pieChart = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: labels,
                datasets: [{
                    data: counts,
                    backgroundColor: colors,
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { font: { size: 11 } }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                var entry = entries[context.dataIndex];
                                return entry.bucketLabel + ': ' + entry.fileCount +
                                    ' files (' + entry.percentage + '%)';
                            }
                        }
                    }
                },
                // Pie chart is informational only — no click handler
                onClick: null
            }
        });
    }

    // -------------------------------------------------------------------------
    // Render bar chart
    // -------------------------------------------------------------------------
    function renderBarChart(data) {
        var entries = (data && data.barChartEntries) ? data.barChartEntries : [];

        if (barChart) {
            barChart.destroy();
            barChart = null;
        }

        // Show leaf message when no subfolders exist
        if (!entries || entries.length === 0) {
            $('#leafMessage').show();
            // Resize canvas to zero height so it doesn't show blank space
            var canvas = document.getElementById('staleBarChart');
            canvas.height = 0;
            return;
        }

        $('#leafMessage').hide();

        // Collect unique subfolders (preserving insertion order)
        var subfolderSet = [];
        var seenSubs = {};
        entries.forEach(function (e) {
            if (e.subfolder && !seenSubs[e.subfolder]) {
                seenSubs[e.subfolder] = true;
                subfolderSet.push(e.subfolder);
            }
        });
        subfolderSet.sort();

        // Build a lookup: subfolder -> bucketOrder -> fileCount
        var lookup = {};
        entries.forEach(function (e) {
            if (!lookup[e.subfolder]) { lookup[e.subfolder] = {}; }
            lookup[e.subfolder][e.bucketOrder] = {
                count: e.fileCount,
                label: e.bucketLabel
            };
        });

        // Build one dataset per bucket order (1-4)
        var datasets = BUCKET_ORDER.map(function (order) {
            return {
                label: BUCKET_LABELS[order],
                backgroundColor: BUCKET_COLORS[order],
                data: subfolderSet.map(function (sub) {
                    return (lookup[sub] && lookup[sub][order]) ? lookup[sub][order].count : 0;
                })
            };
        });

        // Calculate canvas height proportional to number of subfolders
        var canvasHeight = Math.max(350, subfolderSet.length * 28 + 60);
        var canvas = document.getElementById('staleBarChart');
        canvas.height = canvasHeight;

        var ctx = canvas.getContext('2d');
        barChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: subfolderSet,
                datasets: datasets
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: {
                        stacked: true,
                        title: { display: true, text: 'File Count' }
                    },
                    y: {
                        stacked: true
                    }
                },
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { font: { size: 11 } }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return context.dataset.label + ': ' + context.raw + ' files';
                            }
                        }
                    }
                },
                // Drill-down is handled only when clicking y-axis labels (see canvas handlers below)
                onClick: null,
                onHover: null
            }
        });

        // Make only y-axis labels clickable (not bars) by hit-testing the label zone.
        var getLabelIndexFromEvent = function (evt) {
            if (!barChart || !barChart.chartArea || !barChart.scales || !barChart.scales.y) {
                return -1;
            }

            var chartArea = barChart.chartArea;
            var yScale = barChart.scales.y;
            var pos = Chart.helpers.getRelativePosition(evt, barChart);

            // Label region is left of the plotting area.
            if (!pos || pos.x >= chartArea.left || pos.y < chartArea.top || pos.y > chartArea.bottom) {
                return -1;
            }

            var idx = yScale.getValueForPixel(pos.y);
            if (idx === null || idx === undefined || isNaN(idx)) {
                return -1;
            }

            idx = Math.round(idx);
            return (idx >= 0 && idx < subfolderSet.length) ? idx : -1;
        };

        var getBarHitFromEvent = function (evt) {
            if (!barChart || !barChart.chartArea) {
                return null;
            }
            var elements = barChart.getElementsAtEventForMode(evt, 'nearest', { intersect: true }, true);
            if (!elements || elements.length === 0) {
                return null;
            }

            var first = elements[0];
            if (first.index === undefined || first.datasetIndex === undefined) {
                return null;
            }

            var subfolder = subfolderSet[first.index];
            var bucketOrder = BUCKET_ORDER[first.datasetIndex];
            if (!subfolder || !bucketOrder) {
                return null;
            }

            return {
                subfolder: subfolder,
                bucketOrder: bucketOrder
            };
        };

        canvas.onclick = function (evt) {
            var labelIdx = getLabelIndexFromEvent(evt);
            if (labelIdx >= 0) {
                drillDown(subfolderSet[labelIdx]);
                return;
            }

            var barHit = getBarHitFromEvent(evt);
            if (barHit) {
                submitLastAccessReportSearch(currentPath + '/' + barHit.subfolder, barHit.bucketOrder);
            }
        };

        canvas.onmousemove = function (evt) {
            var labelIdx = getLabelIndexFromEvent(evt);
            if (labelIdx >= 0) {
                canvas.style.cursor = 'pointer';
                return;
            }
            var barHit = getBarHitFromEvent(evt);
            canvas.style.cursor = barHit ? 'pointer' : 'default';
        }
    }

    // -------------------------------------------------------------------------
    // Drill down into a subfolder
    // -------------------------------------------------------------------------
    function drillDown(subfolder) {
        currentPath = currentPath + '/' + subfolder;
        loadCharts(currentBasePath, currentPath);
    }

    // -------------------------------------------------------------------------
    // Navigate to an ancestor path via breadcrumb click
    // -------------------------------------------------------------------------
    function breadcrumbNavigate(path) {
        currentPath = path;
        loadCharts(currentBasePath, currentPath);
    }

    // -------------------------------------------------------------------------
    // Update breadcrumb display
    // -------------------------------------------------------------------------
    function updateBreadcrumb(path) {
        if (!path) {
            $('#breadcrumbContainer').hide();
            return;
        }
        $('#breadcrumbContainer').show();

        var parts = path.split('/').filter(function (p) { return p.length > 0; });
        var html = '';
        var accumulated = '';

        parts.forEach(function (part, index) {
            accumulated += '/' + part;
            var isLast = (index === parts.length - 1);
            if (isLast) {
                html += '<span class="current">' + escapeHtml(part) + '</span>';
            } else {
                html += '<a href="#" data-path="' + escapeAttr(accumulated) +
                    '">' + escapeHtml(part) + '</a>';
                html += '<span class="separator">/</span>';
            }
        });

        $('#breadcrumbContent').html(html);
    }

    // Attach breadcrumb click handler via event delegation (no inline onclick)
    $(document).on('click', '#breadcrumbContent a[data-path]', function (e) {
        e.preventDefault();
        breadcrumbNavigate($(this).data('path'));
    });

    // -------------------------------------------------------------------------
    // Utility: HTML-escape a string for display
    // -------------------------------------------------------------------------
    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    // -------------------------------------------------------------------------
    // Utility: escape for use inside a single-quoted HTML attribute
    // -------------------------------------------------------------------------
    function escapeAttr(str) {
        return String(str).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
    }

}());