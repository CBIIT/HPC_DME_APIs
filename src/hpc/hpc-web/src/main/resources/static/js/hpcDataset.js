var app = angular.module('myApp', ['ngGrid']);
var linkCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="dataset?id={{row.getProperty(\'id\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';
app.controller('MyCtrl', function($scope, $http, $q, $attrs) {
	var deferred = $q.defer();
	$scope.$watch('userId', function () {
	console.log('userId', $scope.userId);
	$http.get($scope.baseURL + '/dataset/?creatorId=' + $scope.userId).
	//$http.get('/js/hpcDatasets.json').
	  success(function(data, status, headers, config) {
		        var datasets = data["gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO"];
				console.log('Success', datasets["datasets"]);
				$scope.hpcData = datasets["datasets"];
				deferred.resolve($scope.hpcData);
	  }).
	  error(function(data, status, headers, config) {
		console.log('Failure', status);
	  });
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
            displayName: 'Creator By',
            enableCellEdit: false
         },
         {
            field: 'registratorId',
            displayName: 'Registered by',
            enableCellEdit: false
         },        {
            field: 'created',
            displayName: 'Created Date',
            enableCellEdit: false
         }]
       };
});