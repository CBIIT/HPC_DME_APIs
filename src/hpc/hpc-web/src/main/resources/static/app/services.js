(function(angular) {
  var HpcDataRegistrationFactory = function($resource) {
    return $resource('/dataset/:id', {
      id: '@id'
    }, {
      update: {
        method: "PUT"
      },
      remove: {
        method: "DELETE"
      }
    });
  };

  HpcDataRegistrationFactory.$inject = ['$resource'];
  angular.module("hpcWeb.services").factory("HpcRegistration", HpcDataRegistrationFactory);
}(angular));