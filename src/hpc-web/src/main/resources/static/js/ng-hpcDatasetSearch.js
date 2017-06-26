var app = angular.module('searchDatasetApp', ['ngGrid']);
var linkCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="dataset?id={{row.getProperty(\'id\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';

app.controller('DatasetSearchCtrl', ['$scope', function($scope) {
	 console.log('************************************', "test1"); 
	$scope.datasetSearch = {};

	 $scope.searchDatasetName = function() {
		 console.log('************************************', "test2");
			$http.get("/dataset/query/name/" + $scope.datasetName).
			  success(function(data, status, headers, config) {
				        var collection = data["gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO"];
				    	console.log('collection', collection);
			        var datasets = collection["gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO"];
			    	console.log('datasets', datasets);
						$scope.hpcData = datasets;
						deferred.resolve($scope.hpcData);
			  }).
			  error(function(data, status, headers, config) {
				console.log('Failure', status);
			  });
			};



$scope.gridOptions = {
		data: $window.results,
        enableRowSelection: false,
        enableCellEditOnFocus: false,
        showSelectionCheckbox: false,
        selectedItems:$scope.selectedRows,
        columnDefs: [{
            field: 'id',
            displayName: 'Id',
            enableCellEdit: false,
            cellTemplate: linkCellTemplate
         }, {
             field: 'fileSet.name',
             displayName: 'Dataset Name',
             enableCellEdit: false
         }, {
            field: 'fileSet.created',
            displayName: 'Created Date',
            enableCellEdit: false
         }]
       };
}]);