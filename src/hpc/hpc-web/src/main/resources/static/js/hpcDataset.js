var app = angular.module('myApp', ['ngGrid']);
var linkCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="dataset?id={{row.getProperty(\'id\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';
app.controller('MyCtrl', function($scope, $http, $q, $attrs) {
	var deferred = $q.defer();
	//$http({method: 'GET', url: '/users', params: {'limit': $scope.pagingOptions.pageSize, 'page': $scope.pagingOptions.currentPage}})
	$http.get('http://localhost:7737/hpc-server/dataset/?creatorId=pkonka').
	//$http.get('/js/hpcDatasets.json').
	  success(function(data, status, headers, config) {
		        var datasets = data["gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO"];
				console.log('Success', datasets["datasets"]);
				$scope.hpcData = datasets["datasets"];
				deferred.resolve($scope.hpcData);
	  }).
	  error(function(data, status, headers, config) {
		// called asynchronously if an error occurs
		// or server returns response with an error status.
		console.log('Failure', status);
	  });

$scope.gridOptions = {
        data: 'hpcData',
        enableRowSelection: false,
        enableCellEditOnFocus: false,
        showSelectionCheckbox: false,
        selectedItems:$scope.selectedRows,
        columnDefs: [{
            field: 'name',
            displayName: 'Dataset Name',
            enableCellEdit: false,
            cellTemplate: linkCellTemplate
         }, {
            field: 'primaryInvestigatorId',
            displayName: 'PI Id',
            enableCellEdit: false
         }, {
            field: 'creatorId',
            displayName: 'Creator Id',
            enableCellEdit: false
         },
         {
            field: 'files.length',
            displayName: 'Number of files',
            enableCellEdit: false
         },        {
            field: 'created',
            displayName: 'Created Date',
            enableCellEdit: false
         }]
       };
});