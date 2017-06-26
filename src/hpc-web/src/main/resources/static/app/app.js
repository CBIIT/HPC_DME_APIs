(function(angular) {
  angular.module("hpcWeb.controllers", []);
  angular.module("hpcWeb.services", []);
  angular.module("hpcWeb", ["ngResource", "hpcWeb.controllers", "hpcWeb.services"]);
}(angular));