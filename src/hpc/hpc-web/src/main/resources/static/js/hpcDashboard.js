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

	$http.get($scope.queryURL).
	  success(function(data, status, headers, config) {
		  console.log('success');
	        var namedqueriesDTO = data["gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryListDTO"];
	        console.log('namedqueriesDTO', namedqueriesDTO);
	        var namedqueries = queries["gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery"];
	        if(!namedqueries instanceof Array)
	        {
	        	console.log('not instance of array');
	        	$scope.hpcQueries = new Array(namedqueries);
	        }
	        else
	        {
	        	if(namedqueries.length == undefined)
	        		$scope.hpcQueries = new Array(namedqueries);
	        	else
	        		$scope.hpcQueries = namedqueries;
	        }
			deferred.resolve($scope.hpcQueries);
	  }).
	  error(function(data, status, headers, config) {
		console.log('Failure', status);
	  });
	


	$scope.pagingOptions = {
		    pageSizes: [10, 20, 30, 500, 1000, 5000], //page Sizes
		    pageSize: 10, //Size of Paging data
		    currentPage: 1 //what page they are currently on
		};
	
$scope.gridOptions1 = {
        data: 'hpcNotifications',
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
             displayName: 'Type',
             enableCellEdit: false
         },{
             field: 'created',
             displayName: 'Created',
             enableCellEdit: false,
             cellFilter: 'date:\'yyyy-MM-dd\''
           }],
         sortInfo: {
   	      fields: ['created'],
   	      directions: ['asc']
   	    }    
       };
$scope.gridOptions1.pagingOptions = $scope.pagingOptions;
$scope.gridOptions1.showFooter = true;
$scope.gridOptions1.enablePaging = true;

$scope.gridOptions2 = {
        enableRowSelection: false,
        data: 'hpcQueries',
        enableCellEditOnFocus: false,
        enableColumnResize: true,
        showSelectionCheckbox: false,
        selectedItems:$scope.selectedRows,
        columnDefs: [{
        	 field: 'searchname',
             displayName: 'Name',
             enableCellEdit: false
         }, {
             field: 'description',
             displayName: 'Description',
             enableCellEdit: false
           },{
              field: 'created',
              displayName: 'Created',
              enableCellEdit: false,
              cellFilter: 'date:\'yyyy-MM-dd\''
           }],
           sortInfo: {
        	      fields: ['searchname'],
        	      directions: ['asc']
        	    }           
       };

$scope.gridOptions2.pagingOptions = $scope.pagingOptions;
$scope.gridOptions2.showFooter = false;
$scope.gridOptions2.enablePaging = false;
})});


