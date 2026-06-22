/**
 * hpcStaleFiles.js
 *
 * JavaScript for the Stale Files Dashboard.
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

    var BUCKET_ORDER = [1, 2, 3, 4];

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------
    $(document).ready(function () {
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
            url: '/staleFiles/pieChartData',
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
            url: '/staleFiles/barChartData',
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
        entries.sort(function (a, b) { return a.staleBucketOrder - b.staleBucketOrder; });

        var labels = entries.map(function (e) {
            return e.staleBucketLabel + ' (' + e.percentage + '%)';
        });
        var counts = entries.map(function (e) { return e.fileCount; });
        var colors = entries.map(function (e) { return BUCKET_COLORS[e.staleBucketOrder] || '#999'; });

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
                                return entry.staleBucketLabel + ': ' + entry.fileCount +
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
            lookup[e.subfolder][e.staleBucketOrder] = {
                count: e.fileCount,
                label: e.staleBucketLabel
            };
        });

        // Build one dataset per bucket order (1-4)
        var bucketLabels = {
            1: 'Green: accessed within 90 days',
            2: 'Yellow: 90-180 days',
            3: 'Red: 180-365 days',
            4: 'Dark red: over 365 days'
        };

        var datasets = BUCKET_ORDER.map(function (order) {
            return {
                label: bucketLabels[order],
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
                onClick: function (event, elements) {
                    if (elements && elements.length > 0) {
                        var idx = elements[0].index;
                        var subfolder = subfolderSet[idx];
                        if (subfolder) {
                            drillDown(subfolder);
                        }
                    }
                },
                onHover: function (event, elements) {
                    var canvas = document.getElementById('staleBarChart');
                    canvas.style.cursor = (elements && elements.length > 0) ? 'pointer' : 'default';
                }
            }
        });
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
    // Expose for inline onclick in breadcrumb HTML
    window.breadcrumbNavigate = breadcrumbNavigate;

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
                var pathSnapshot = accumulated;
                html += '<a onclick="breadcrumbNavigate(\'' +
                    escapeAttr(pathSnapshot) + '\')">' + escapeHtml(part) + '</a>';
                html += '<span class="separator">/</span>';
            }
        });

        $('#breadcrumbContent').html(html);
    }

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
