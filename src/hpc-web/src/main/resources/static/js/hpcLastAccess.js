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
    var latestPieEntries = [];
    var latestBarEntries = [];

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

    // Format bytes to human-readable format (B, KB, MB, GB, TB, PB)
    function formatBytes(bytes) {
        if (bytes === 0) return '0 B';
        var k = 1024;
        var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
        var i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1);
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
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
        if (currentBasePath && currentBasePath !== 'ALL') {
            add('basepath', currentBasePath);
        }

        // Date criteria aliases
        add('fromDate', range.from);
        add('toDate', range.to);

        // includeAWSBucket Flag
        add('includeAWSBucket', getIncludeAWSBucketFlag())

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

        toggleExportButtons();

        $('#basePathSelect').on('change', function () {
            var selected = $(this).val();
            if (!selected) {
                currentBasePath = '';
                currentPath = '';
                $('#chartsRow').hide();
                $('#breadcrumbContainer').hide();
                toggleExportButtons();
                return;
            }
            currentBasePath = selected;
            currentPath = (selected === 'ALL') ? '' : selected;
            toggleExportButtons();
            loadCharts(currentBasePath, currentPath);
        });

        $('#includeAWSBucket').on('change', function () {
            if (currentBasePath) {
                loadCharts(currentBasePath, currentPath);
            }
        });

        $('#exportPdfBtn').on('click', function (e) {
            e.preventDefault();
            exportChartsToPdf();
        });

        $('#exportExcelBtn').on('click', function (e) {
            e.preventDefault();
            exportDataToExcel();
        });
    });

    function getIncludeAWSBucketFlag() {
        return $('#includeAWSBucket').is(':checked');
    }

    function toggleExportButtons() {
        var hasBasePath = !!$('#basePathSelect').val();
        $('#exportPdfBtn, #exportExcelBtn').prop('disabled', !hasBasePath);
    }

    // -------------------------------------------------------------------------
    // Reset (destroy + clear) both charts
    // -------------------------------------------------------------------------
    function resetCharts() {
        if (pieChart) {
            pieChart.destroy();
            pieChart = null;
        }
        if (barChart) {
            barChart.destroy();
            barChart = null;
        }
        latestPieEntries = [];
        latestBarEntries = [];

        // Clear pie canvas
        var pieCanvas = document.getElementById('stalePieChart');
        if (pieCanvas) {
            var pieCtx = pieCanvas.getContext('2d');
            pieCtx.clearRect(0, 0, pieCanvas.width, pieCanvas.height);
        }

        // Clear bar canvas
        var barCanvas = document.getElementById('staleBarChart');
        if (barCanvas) {
            var barCtx = barCanvas.getContext('2d');
            barCtx.clearRect(0, 0, barCanvas.width, barCanvas.height);
            barCanvas.height = 0;
        }

        $('#leafMessage').hide();
    }

    // -------------------------------------------------------------------------
    // Load both charts for the given basePath and currentPath
    // -------------------------------------------------------------------------
    function loadCharts(basePath, path) {
        resetCharts();
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
        var params = {
            basePath: basePath || '',
            currentPath: path || '',
            includeAWSBucket: getIncludeAWSBucketFlag()
        };
        $.ajax({
            url: '/lastAccess/pieChartData',
            method: 'GET',
            data: params,
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
        var params = {
            basePath: basePath || 'ALL',
            currentPath: path || '',
            includeAWSBucket: getIncludeAWSBucketFlag()
        };
        $.ajax({
            url: '/lastAccess/barChartData',
            method: 'GET',
            data: params,
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
        latestPieEntries = entries.slice();

        // Use dataSize and dataSizePercentage for chart ratios
        var labels = entries.map(function (e) {
            return e.bucketLabel + ' (' + e.dataSizePercentage + '%)';
        });
        var dataSizes = entries.map(function (e) { return e.dataSize; });
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
                    data: dataSizes,
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
                                return entry.bucketLabel + ': ' + formatBytes(entry.dataSize) +
                                    ' (' + entry.dataSizePercentage + '%)' +
                                    '\n' + entry.fileCount + ' files';
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
        latestBarEntries = entries.slice();

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
            if (e.subfolder && e.subfolder !== 'h' && !seenSubs[e.subfolder]) {
                seenSubs[e.subfolder] = true;
                subfolderSet.push(e.subfolder);
            }
        });
        subfolderSet.sort();

        // Build a lookup: subfolder -> bucketOrder -> { fileCount, dataSize, label }
        var lookup = {};
        entries.forEach(function (e) {
            if (!lookup[e.subfolder]) { lookup[e.subfolder] = {}; }
            lookup[e.subfolder][e.bucketOrder] = {
                fileCount: e.fileCount,
                dataSize: e.dataSize,
                label: e.bucketLabel
            };
        });

        // Build one dataset per bucket order (1-4) using dataSize for chart height
        var datasets = BUCKET_ORDER.map(function (order) {
            return {
                label: BUCKET_LABELS[order],
                backgroundColor: BUCKET_COLORS[order],
                data: subfolderSet.map(function (sub) {
                    return (lookup[sub] && lookup[sub][order]) ? lookup[sub][order].dataSize : 0;
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
                        title: { display: true, text: 'Data Size' }
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
                                var subfolder = subfolderSet[context.dataIndex];
                                var bucketOrder = BUCKET_ORDER[context.datasetIndex];
                                var info = lookup[subfolder][bucketOrder];
                                return context.dataset.label + ': ' + formatBytes(context.raw) +
                                    ' (' + info.fileCount + ' files)';
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
        var newPath = currentPath + '/' + subfolder;

        if (currentBasePath === 'ALL') {
            // First drill-down from ALL: the clicked top-level path becomes the new basePath.
            currentBasePath = newPath;
            currentPath = newPath;
            // Sync the dropdown display without re-firing our change handler.
            $('#basePathSelect').val(currentBasePath).trigger('change.select2');
            toggleExportButtons();
        } else {
            currentPath = newPath;
        }

        loadCharts(currentBasePath, currentPath);
    }

    // -------------------------------------------------------------------------
    // Navigate to an ancestor path via breadcrumb click
    // -------------------------------------------------------------------------
    function breadcrumbNavigate(path) {
        if (!currentBasePath) {
            return;
        }
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

    function exportChartsToPdf() {
        if (!window.jspdf || !window.jspdf.jsPDF) {
            console.error('jsPDF library is not available.');
            return;
        }

        var pieCanvas = document.getElementById('stalePieChart');
        var barCanvas = document.getElementById('staleBarChart');
        if (!pieCanvas && !barCanvas) {
            console.warn('No charts found to export.');
            return;
        }

        var jsPDF = window.jspdf.jsPDF;
        var doc = new jsPDF('p', 'pt', 'a4');
        var pageWidth = doc.internal.pageSize.getWidth();
        var pageHeight = doc.internal.pageSize.getHeight();
        var margin = 30;
        var y = margin;

        var now = new Date();
        doc.setFontSize(14);
        doc.text('Last Accessed Collection Report', margin, y);
        y += 18;
        doc.setFontSize(10);
        doc.text('Path: ' + (currentPath || currentBasePath || ''), margin, y);
        y += 14;
        doc.text('Generated: ' + now.toLocaleString(), margin, y);
        y += 16;

        function addChart(title, canvas, preferredHeight) {
            if (!canvas) {
                return;
            }
            var availableWidth = pageWidth - (margin * 2);
            var height = preferredHeight;
            if (!height) {
                var ratio = canvas.width ? (canvas.height / canvas.width) : 0.6;
                height = availableWidth * ratio;
            }

            if (y + 20 + height > pageHeight - margin) {
                doc.addPage();
                y = margin;
            }

            doc.setFontSize(11);
            doc.text(title, margin, y);
            y += 8;
            doc.addImage(canvas.toDataURL('image/png', 1.0), 'PNG', margin, y, availableWidth, height);
            y += height + 16;
        }

        function addBarPivotTable() {
            if (!latestBarEntries || latestBarEntries.length === 0) {
                return;
            }

            // Keep bucket columns in fixed report order.
            var bucketLabels = BUCKET_ORDER
                .map(function (order) { return BUCKET_LABELS[order]; })
                .filter(function (label) {
                    return latestBarEntries.some(function (e) { return e && e.bucketLabel === label; });
                });

            if (bucketLabels.length === 0) {
                return;
            }

            var subfolderMap = {};
            latestBarEntries.forEach(function (e) {
                if (!e || !e.subfolder) {
                    return;
                }
                if (!subfolderMap[e.subfolder]) {
                    subfolderMap[e.subfolder] = {};
                }
                subfolderMap[e.subfolder][e.bucketLabel] = formatBytes(e.dataSize || 0);
            });

            var subfolders = Object.keys(subfolderMap).sort();
            if (subfolders.length === 0) {
                return;
            }

            var columns = ['Subfolder'].concat(bucketLabels);
            var tableWidth = pageWidth - (margin * 2);
            var colWidth = tableWidth / columns.length;
            var lineHeight = 11;
            var cellPadX = 2;
            var cellPadY = 2;

            var ensureSpace = function (neededHeight) {
                if (y + neededHeight > pageHeight - margin) {
                    doc.addPage();
                    y = margin;
                }
            };

            var drawRow = function (rowValues, isHeader) {
                var wrappedByCol = rowValues.map(function (value, idx) {
                    var maxWidth = Math.max(colWidth - (cellPadX * 2), 10);
                    return doc.splitTextToSize(String(value || ''), maxWidth);
                });

                var maxLines = 1;
                wrappedByCol.forEach(function (lines) {
                    if (lines.length > maxLines) {
                        maxLines = lines.length;
                    }
                });

                var rowHeight = (maxLines * lineHeight) + (cellPadY * 2);
                ensureSpace(rowHeight);

                doc.setFontSize(isHeader ? 9 : 8);
                wrappedByCol.forEach(function (lines, idx) {
                    var x = margin + (idx * colWidth);
                    var textY = y + cellPadY + lineHeight - 2;
                    doc.text(lines, x + cellPadX, textY);
                });

                y += rowHeight;
            };

            ensureSpace(26);
            doc.setFontSize(11);
            doc.text('Bar Data by Subfolder', margin, y);
            y += 12;

            drawRow(columns, true);

            subfolders.forEach(function (subfolder) {
                var row = columns.map(function (col, idx) {
                    if (idx === 0) {
                        return subfolder;
                    }
                    return subfolderMap[subfolder][col] || '0 B';
                });
                drawRow(row, false);
            });

            y += 8;
        }

        addChart('Pie Chart', pieCanvas, 220);
        addChart('Bar Chart', barCanvas, 320);
        addBarPivotTable();
        doc.save('last-access-report-' + formatDateYYYYMMDD(now) + '.pdf');
    }

    function exportDataToExcel() {
        if (!window.XLSX) {
            console.error('SheetJS XLSX library is not available.');
            return;
        }

        var wb = window.XLSX.utils.book_new();

        var bucketLabels = BUCKET_ORDER
            .map(function (order) { return BUCKET_LABELS[order]; })
            .filter(function (label) {
                return latestBarEntries.some(function (e) { return e && e.bucketLabel === label; });
            });

        var subfolderMap = {};
        latestBarEntries.forEach(function (e) {
            if (!e || !e.subfolder || e.subfolder === 'h') {
                return;
            }
            if (!subfolderMap[e.subfolder]) {
                subfolderMap[e.subfolder] = {};
            }
            subfolderMap[e.subfolder][e.bucketLabel] = formatBytes(e.dataSize || 0);
        });

        var subfolders = Object.keys(subfolderMap).sort();
        var rows = subfolders.map(function (subfolder) {
            var row = { Subfolder: subfolder };
            bucketLabels.forEach(function (label) {
                row[label] = subfolderMap[subfolder][label] || '0 B';
            });
            return row;
        });

        if (rows.length === 0) {
            var emptyRow = { Subfolder: '' };
            bucketLabels.forEach(function (label) {
                emptyRow[label] = '';
            });
            rows.push(emptyRow);
        }

        window.XLSX.utils.book_append_sheet(wb, window.XLSX.utils.json_to_sheet(rows), 'Subfolder Summary');

        var now = new Date();
        window.XLSX.writeFile(wb, 'last-access-report-' + formatDateYYYYMMDD(now) + '.xlsx');
    }

}());