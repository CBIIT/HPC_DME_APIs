(function(angular) {
  var HpcDataRegistrationFactory = function($resource) {
    return $resource('/register/:id', {
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