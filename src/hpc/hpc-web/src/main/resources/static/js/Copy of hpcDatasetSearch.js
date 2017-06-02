var app = angular.module('searchDatasetApp', ['ngGrid']);
var linkCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="dataset?id={{row.getProperty(\'id\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';
app.controller('DatasetSearchCtrl', function($scope, $http, $window, $attrs) {
    var collection = $window.results;
    console.log('collection', collection);
   // var datasets = $window.results["gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO"];
	 var datasets = collection["hpcDatasetDTO"];
	 console.log('datasets', datasets);

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
});