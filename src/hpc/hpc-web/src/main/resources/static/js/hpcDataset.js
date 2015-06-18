var app = angular.module('myApp', ['ngGrid']);
var linkCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="dataset?id={{row.getProperty(\'name\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';
app.controller('MyCtrl', function($scope, $http) {
	$http.get('/js/hpcDatasets.json').
	  success(function(data, status, headers, config) {
				console.log('Success', data["gov.nih.nci.hpc.dto.datasetregistration.HpcDatasetDTO"]);
				$scope.hpcData = data["gov.nih.nci.hpc.dto.datasetregistration.HpcDatasetDTO"];
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
           field: '10/12/2014',
           displayName: 'Created Date',
           enableCellEdit: false
        }]
      };
});