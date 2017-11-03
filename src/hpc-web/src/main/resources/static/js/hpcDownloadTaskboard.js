var app = angular.module('DashBoard', ['ngTouch', 'ui.grid', 'ui.grid.pagination', 'ui.grid.resizeColumns']);
var linkSearchNameCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="search?queryName={{row.getProperty(\'message\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';

app.controller('DashBoardCtrl', ['$scope', '$http', function ($scope, $http) {
	$scope.searchesloading = true;
	$http.get('/downloadTasksList').
	  success(function(data, status, headers, config) {
        	$scope.hpcQueries = data;
        	$scope.searchesloading = false;
	  }).
	  error(function(data, status, headers, config) {
		console.log('Failure', status);
	  });

  $scope.gridOptions1 = {
    data: 'hpcQueries',
    paginationPageSize: 100,
    enableFiltering: true,
    enableRowHeaderSelection: false, 
    multiSelect: false,
    enableGridMenu: true,
    enableSorting: true,
    enableColumnResizing: true, 
    columnDefs: [
                 { field: 'taskId', width:300, displayName: 'Task Id', cellTemplate: '<div class="ui-grid-cell-contents" title="TOOLTIP"><a href="task?taskId={{row.entity.taskId}}&amp;type={{row.entity.type}}">{{COL_FIELD CUSTOM_FILTERS}}</a></div>'  },
                 { field: 'path', width:200, displayName : 'Path' },
                 { field: 'type', width:200, displayName : 'Type'},
                 { field: 'result', width:200, displayName: 'Transfer Result'},
                 
               ],
  };
}]);