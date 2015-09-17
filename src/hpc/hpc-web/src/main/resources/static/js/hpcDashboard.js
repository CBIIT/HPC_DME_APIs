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
	        if(!datasets instanceof Array)
	        {
	        	console.log('not instance of array');
	        	$scope.hpcDataset = new Array(datasets);
	        }
	        else
	        {
	        	if(datasets.length == undefined)
	        		$scope.hpcDataset = new Array(datasets);
	        	else
	        		$scope.hpcDataset = datasets;
	        	console.log('instance of array', projects.length);
	        }
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
	        if(!projects instanceof Array)
	        {
	        	console.log('not instance of array');
	        	$scope.hpcProject = new Array(projects);
	        }
	        else
	        {
	        	if(projects.length == undefined)
	        		$scope.hpcProject = new Array(projects);
	        	else
	        		$scope.hpcProject = projects;
	        	console.log('instance of array', projects.length);
	        }
	        console.log('projects', $scope.hpcProject);
				
				deferred.resolve($scope.hpcData);
	  }).
	  error(function(data, status, headers, config) {
		console.log('Failure', status);
	  });	
	});

	$scope.pagingOptions = {
		    pageSizes: [10, 20, 30, 500, 1000, 5000], //page Sizes
		    pageSize: 10, //Size of Paging data
		    currentPage: 1 //what page they are currently on
		};
	
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
$scope.gridOptions1.pagingOptions = $scope.pagingOptions;
$scope.gridOptions1.showFooter = true;
$scope.gridOptions1.enablePaging = true;

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
            field: 'metadata.principalInvestigatorNciUserId',
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

$scope.gridOptions2.pagingOptions = $scope.pagingOptions;
$scope.gridOptions2.showFooter = true;
$scope.gridOptions2.enablePaging = true;

});


