'use strict';

angular.module('angularGruntSeed')

.config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/', {
        templateUrl: '/test/templates/home.html',
        controller: 'HomeController'
    })
    .otherwise({ redirectTo: '/' });
}]);