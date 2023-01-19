var app = angular.module('DashBoard', ['ngTouch', 'ui.grid', 'ui.grid.pagination', 'ui.grid.resizeColumns']);
var linkSearchNameCellTemplate = '<div class="ngCellText" ng-class="col.colIndex()">' +
'  <a href="search?queryName={{row.getProperty(\'message\')}}">{{row.getProperty(col.field)}}</a>' +
'</div>';
var selectedColumns = null;

app.filter('percentEncoding', function () {
  return function (argStr) {
    return encodeURIComponent(argStr);
  };
});

app.controller('DashBoardCtrl', ['$scope', '$http', function ($scope, $http) {
	$scope.searchesloading = true;
	$http.get('/savedSearchList').
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
      {
        field : 'searchName',
        width: '45%',
        displayName : 'Search Name',
        cellFilter: 'percentEncoding',
        cellTemplate : '<div class="ui-grid-cell-contents" title="TOOLTIP"><a title="Run Search" href="search?queryName={{COL_FIELD CUSTOM_FILTERS}}&amp;page=1">{{COL_FIELD CUSTOM_FILTERS}}</a>&nbsp;&nbsp;&nbsp;<a href="#" id="{{COL_FIELD CUSTOM_FILTERS}}_edit" onclick="editSearch(this)"><i class="fa fa-edit" title="Edit Search Criteria"></i></a></div>'
      },
      {
        field : 'searchType',
        width: '15%',
        displayName : 'Search Type'
      },
      {
        field : 'createdOn',
        width: '15%',
        displayName : 'Created On',
        type:'date'
      },
      {
        field : 'frequency',
        width: '15%',
        displayName : 'Auto Run Frequency',
        cellFilter: 'percentEncoding',
        cellTemplate : '<div class="ui-grid-cell-contents" title="TOOLTIP">{{COL_FIELD CUSTOM_FILTERS}}&nbsp;&nbsp;&nbsp;<a href="/savesearch?queryName={{row.entity.searchName}}" data-toggle="modal" data-target="#saveModal" id="{{COL_FIELD CUSTOM_FILTERS}}_frequency"><i class="fa fa-edit" title="Modify Settings"></a></div>'
       },
      {
        field : 'delete',
        width: '10%',
        displayName : 'Delete',
        cellFilter: 'percentEncoding',
        cellTemplate : '<div class="ui-grid-cell-contents" title="TOOLTIP"><a href="#" id="{{COL_FIELD CUSTOM_FILTERS}}" onclick="deleteSearch(this)"><i class="fa fa-trash" title="Delete"></i></a></div>'
      }
    ]
  };

  $("#updatedField").on("change", function(){
	  location.reload() 
  });
	     
$scope.notificationsloading = true;
$http.get('/notificationList').
  success(function(data, status, headers, config) {
  	$scope.hpcNotifications = data;
  	$scope.notificationsloading = false;
  }).
  error(function(data, status, headers, config) {
	console.log('Failure', status);
  });

  $scope.gridOptions2 = {
	    data: 'hpcNotifications',
	    paginationPageSize: 100,
	    enableFiltering: true,
	    enableRowHeaderSelection: false, 
	    multiSelect: false,
	    enableGridMenu: true,
	    enableSorting: true,
	    enableColumnResizing: true,    
	    columnDefs: [
	      { 
	    	field: "eventId", 
	    	displayName: "ID",
	        enableCellEdit: false,
	        cellTemplate: '<div class="ui-grid-cell-contents" title="TOOLTIP"><a href="event?id={{COL_FIELD CUSTOM_FILTERS}}">{{COL_FIELD CUSTOM_FILTERS}}</a></div>' 
	      },
	      { 
		    	field: "eventType", 
		    	displayName: "Event",
		        enableCellEdit: false ,
		        width: "40%"
		  },
	      { 
		    	field: "eventCreated", 
		    	displayName: "Created On",
		        enableCellEdit: false 
		  },
	      { 
		    	field: "notificationDeliveryMethod", 
		    	displayName: "Delivery Type",
		        enableCellEdit: false 
		  },
	      { 
		    	field: "delivered", 
		    	displayName: "Delivered On",
		        enableCellEdit: false 
		  }
	    ]
	  };
}]);