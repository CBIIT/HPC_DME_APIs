var app = angular.module('myApp', ['ngGrid']);
var linkDatasetCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="dataset?id={{row.getProperty(\'id\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';
var linkProjectCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="project?id={{row.getProperty(\'id\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';
app.controller('MyCtrl', function($scope, $http, $q, $attrs) {
	var deferred = $q.defer();
	$scope.$watch('userId', function () {
	console.log('$scope.userId', $scope.userId);

	$http.get($scope.datasetURL + '/' + $scope.userId).
	//$http.get('/js/hpcDatasets.json').
	  success(function(data, status, headers, config) {
		        var collection = data["gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO"];
		    	console.log('dataset collection', collection);
	        var datasets = collection["gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO"];
	    	console.log('datasets', datasets);
				$scope.hpcDataset = datasets;
				deferred.resolve($scope.hpcData);
	  }).
	  error(function(data, status, headers, config) {
		console.log('Failure', status);
	  });
	
	$http.get($scope.projectURL + '/' + $scope.userId).
	//$http.get('/js/hpcDatasets.json').
	  success(function(data, status, headers, config) {
		        var collection = data["gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO"];
		    	console.log('project collection', collection);
	        var projects = collection["gov.nih.nci.hpc.dto.project.HpcProjectDTO"];
	        console.log('projects', projects);
				$scope.hpcProject = projects;
				deferred.resolve($scope.hpcData);
	  }).
	  error(function(data, status, headers, config) {
		console.log('Failure', status);
	  });	
	});

$scope.gridOptions1 = {
        data: 'hpcDataset',
        enableRowSelection: false,
        enableColumnResize: true,
        enableCellEditOnFocus: false,
        showSelectionCheckbox: false,
        selectedItems:$scope.selectedRows,
        columnDefs: [{
            field: 'id',
            displayName: 'Id',
            enableCellEdit: false,
            cellTemplate: linkDatasetCellTemplate
         }, {
             field: 'fileSet.name',
             displayName: 'Dataset Name',
             enableCellEdit: false
         }, {
            field: 'fileSet.description',
            displayName: 'Description',
            enableCellEdit: false
         },{
             field: 'created',
             displayName: 'Created',
             enableCellEdit: false,
             cellFilter: 'date:\'yyyy-MM-dd\''
          },{
              field: 'lastUpdated',
              displayName: 'Last Updated',
              enableCellEdit: false,
              cellFilter: 'date:\'yyyy-MM-dd\''
           }],
         sortInfo: {
   	      fields: ['fileSet.name'],
   	      directions: ['asc']
   	    }    
       };

$scope.gridOptions2 = {
        data: 'hpcProject',
        enableRowSelection: false,
        enableCellEditOnFocus: false,
        enableColumnResize: true,
        showSelectionCheckbox: false,
        selectedItems:$scope.selectedRows,
        columnDefs: [{
            field: 'id',
            displayName: 'Id',
            enableCellEdit: false,
            cellTemplate: linkProjectCellTemplate
         }, {
             field: 'metadata.name',
             displayName: 'Project Name',
             enableCellEdit: false
         }, {
             field: 'metadata.internalProjectId',
             displayName: 'Internal Project Id',
             enableCellEdit: false
         },{
            field: 'metadata.principalInvestigatorNihUserId',
            displayName: 'Investigator',
            enableCellEdit: false
         }, {
             field: 'metadata.description',
             displayName: 'Description',
             enableCellEdit: false
          }, {
              field: 'metadata.labBranch',
              displayName: 'Lab/Branch',
              enableCellEdit: false
           },{
              field: 'metadata.created',
              displayName: 'Created',
              enableCellEdit: false,
              cellFilter: 'date:\'yyyy-MM-dd\''
           }],
           sortInfo: {
        	      fields: ['metadata.name'],
        	      directions: ['asc']
        	    }           
       };
});

