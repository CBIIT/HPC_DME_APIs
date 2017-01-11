var app = angular.module('DashBoard', ['ngTouch', 'ui.grid', 'ui.grid.pagination']);
var linkSearchNameCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="search?queryName={{row.getProperty(\'message\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';

app.controller('DashBoardCtrl', ['$scope', '$http', function ($scope, $http) {
	$http.get('/query').
	  success(function(data, status, headers, config) {
//		    console.log('success0' + data);
//		    var str = JSON.stringify(data, null, 2);
//		    console.log('success1' + str);
        	$scope.hpcQueries = data;
	  }).
	  error(function(data, status, headers, config) {
		console.log('Failure', status);
	  });
	
  $scope.gridOptions1 = {
    paginationPageSizes: [25, 50, 75],
    data: 'hpcQueries',
    paginationPageSize: 25,
    columnDefs: [
      { 
    	field: "message", 
    	displayName: "Name",
        enableCellEdit: false,
        cellTemplate: '<div class="ui-grid-cell-contents" title="TOOLTIP"><a href="search?queryName={{COL_FIELD CUSTOM_FILTERS}}">{{COL_FIELD CUSTOM_FILTERS}}</a></div>' 
      }
    ]
  };
 
  $scope.gridOptions2 = {
    enablePaginationControls: false,
    paginationPageSize: 25,
    columnDefs: [
      { name: 'name' }
    ]
  };
 
	$scope.myData = [{name: "Moroni", age: 50},
	                 {name: "Tiancum", age: 43},
	                 {name: "Jacob", age: 27},
	                 {name: "Nephi", age: 29},
	                 {name: "Enos", age: 34}];
 
	
  $scope.gridOptions2.onRegisterApi = function (gridApi) {
    $scope.gridApi2 = gridApi;
  }
 
    $scope.gridOptions2.data = $scope.myData;
}]);