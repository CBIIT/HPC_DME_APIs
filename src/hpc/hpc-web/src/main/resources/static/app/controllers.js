(function(angular) {
  var AppController = function($scope, HpcRegistration) {
    HpcRegistration.query(function(response) {
      $scope.datasets = response ? response : [];
    });

    $scope.register = function(hpcRegistration) {
      new HpcRegistration({
        projectName: hpcRegistration.projectName,
        investigatorName: hpcRegistration.investigatorName,
        originDataendpoint: hpcRegistration.originDataendpoint,
        originDataLocation: hpcRegistration.originDataLocation
      }).$save(function(dataset) {
        $scope.datasets.push(hpcRegistration);
      });
      $scope.hpcRegistration = "";
    };

    $scope.updateDataset = function(hpcRegistration) {
      dataset.$update();
    };
  };

  AppController.$inject = ['$scope', 'HpcRegistration'];
  angular.module("hpcWeb.controllers").controller("AppController", AppController);
}(angular));