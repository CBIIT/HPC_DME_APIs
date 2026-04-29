const fs = require('fs');
const path = require('path');

const base = process.cwd();
const colBase = path.join(base, 'postman/collections/HPC Server API');
const envBase = path.join(base, 'postman/environments');

// Create directories
const dirs = [
  colBase,
  path.join(colBase, '.resources'),
  path.join(colBase, 'Security'),
  path.join(colBase, 'Security', '.resources'),
  path.join(colBase, 'Data Management'),
  path.join(colBase, 'Data Management', '.resources'),
  path.join(colBase, 'Data Search'),
  path.join(colBase, 'Data Search', '.resources'),
  path.join(colBase, 'Data Browse'),
  path.join(colBase, 'Data Browse', '.resources'),
  path.join(colBase, 'Notifications'),
  path.join(colBase, 'Notifications', '.resources'),
  path.join(colBase, 'Reports'),
  path.join(colBase, 'Reports', '.resources'),
  path.join(colBase, 'Data Migration'),
  path.join(colBase, 'Data Migration', '.resources'),
  path.join(colBase, 'Data Tiering'),
  path.join(colBase, 'Data Tiering', '.resources'),
  path.join(colBase, 'Review'),
  path.join(colBase, 'Review', '.resources'),
  envBase,
];
dirs.forEach(d => fs.mkdirSync(d, { recursive: true }));

function write(filePath, content) {
  fs.writeFileSync(filePath, content, 'utf8');
  console.log('Created:', filePath.replace(base + '/', ''));
}

// ============================================================
// COLLECTION DEFINITION
// ============================================================
write(path.join(colBase, '.resources/definition.yaml'), `$kind: collection
name: HPC Server API
description: |-
  REST API for the HPC Data Management (HPC-DME) server.
  Base URL: {{baseUrl}}{{basePath}}
  Authentication: Basic Auth (NCI username/password).
auth:
  type: basic
  credentials:
    - key: username
      value: '{{username}}'
    - key: password
      value: '{{password}}'
variables:
  - key: baseUrl
    value: 'https://localhost:7738'
    description: Base URL of the HPC server
  - key: basePath
    value: '/hpc-server'
    description: Base path for all API endpoints
  - key: username
    value: ''
    description: NCI username for Basic Auth
  - key: password
    value: ''
    description: NCI password for Basic Auth
  - key: token
    value: ''
    description: Bearer token (obtained from /authenticate)
`);

// ============================================================
// SECURITY FOLDER
// ============================================================
write(path.join(colBase, 'Security/.resources/definition.yaml'), `$kind: collection
name: Security
description: User management, group management, and authentication endpoints.
order: 1000
`);

write(path.join(colBase, 'Security/authenticate.request.yaml'), `$kind: http-request
name: Authenticate
method: GET
url: '{{baseUrl}}{{basePath}}/authenticate'
order: 1000
headers:
  - key: Accept
    value: 'application/json'
`);

write(path.join(colBase, 'Security/get invoker.request.yaml'), `$kind: http-request
name: Get Invoker
method: GET
url: '{{baseUrl}}{{basePath}}/user'
order: 2000
headers:
  - key: Accept
    value: 'application/json'
`);

write(path.join(colBase, 'Security/register user.request.yaml'), `$kind: http-request
name: Register User
method: PUT
url: '{{baseUrl}}{{basePath}}/user/{{nciUserId}}'
order: 3000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: nciUserId
    value: 'userId'
body:
  type: json
  content: |-
    {
      "firstName": "John",
      "lastName": "Doe",
      "doc": "NCI",
      "defaultBasePath": "/NCI",
      "userRole": "USER",
      "active": true
    }
`);

write(path.join(colBase, 'Security/update user.request.yaml'), `$kind: http-request
name: Update User
method: POST
url: '{{baseUrl}}{{basePath}}/user/{{nciUserId}}'
order: 4000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: nciUserId
    value: 'userId'
body:
  type: json
  content: |-
    {
      "firstName": "John",
      "lastName": "Doe",
      "doc": "NCI",
      "defaultBasePath": "/NCI",
      "userRole": "USER",
      "active": true
    }
`);

write(path.join(colBase, 'Security/get user.request.yaml'), `$kind: http-request
name: Get User
method: GET
url: '{{baseUrl}}{{basePath}}/user/{{nciUserId}}'
order: 5000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: nciUserId
    value: 'userId'
`);

write(path.join(colBase, 'Security/delete user.request.yaml'), `$kind: http-request
name: Delete User
method: DELETE
url: '{{baseUrl}}{{basePath}}/user/{{nciUserId}}'
order: 6000
pathVariables:
  - key: nciUserId
    value: 'userId'
`);

write(path.join(colBase, 'Security/get active users.request.yaml'), `$kind: http-request
name: Get Active Users
method: GET
url: '{{baseUrl}}{{basePath}}/user/active'
order: 7000
headers:
  - key: Accept
    value: 'application/json'
queryParams:
  - key: nciUserId
    value: ''
    disabled: true
  - key: firstNamePattern
    value: ''
    disabled: true
  - key: lastNamePattern
    value: ''
    disabled: true
  - key: doc
    value: ''
    disabled: true
  - key: defaultBasePath
    value: ''
    disabled: true
`);

write(path.join(colBase, 'Security/query users.request.yaml'), `$kind: http-request
name: Query Users
method: GET
url: '{{baseUrl}}{{basePath}}/user/query'
order: 8000
headers:
  - key: Accept
    value: 'application/json'
queryParams:
  - key: nciUserId
    value: ''
    disabled: true
  - key: firstNamePattern
    value: ''
    disabled: true
  - key: lastNamePattern
    value: ''
    disabled: true
  - key: doc
    value: ''
    disabled: true
  - key: defaultBasePath
    value: ''
    disabled: true
`);

write(path.join(colBase, 'Security/get users by role.request.yaml'), `$kind: http-request
name: Get Users By Role
method: GET
url: '{{baseUrl}}{{basePath}}/user/role/{{roleName}}'
order: 9000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: roleName
    value: 'USER'
queryParams:
  - key: doc
    value: ''
    disabled: true
  - key: defaultBasePath
    value: ''
    disabled: true
`);

write(path.join(colBase, 'Security/get user groups.request.yaml'), `$kind: http-request
name: Get User Groups
method: GET
url: '{{baseUrl}}{{basePath}}/user/group/{{nciUserId}}'
order: 10000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: nciUserId
    value: 'userId'
`);

write(path.join(colBase, 'Security/register group.request.yaml'), `$kind: http-request
name: Register Group
method: PUT
url: '{{baseUrl}}{{basePath}}/group/{{groupName}}'
order: 11000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: groupName
    value: 'myGroup'
body:
  type: json
  content: |-
    {
      "addUserRequests": [{ "userId": "userId1" }]
    }
`);

write(path.join(colBase, 'Security/update group.request.yaml'), `$kind: http-request
name: Update Group
method: POST
url: '{{baseUrl}}{{basePath}}/group/{{groupName}}'
order: 12000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: groupName
    value: 'myGroup'
body:
  type: json
  content: |-
    {
      "addUserRequests": [{ "userId": "userId1" }],
      "deleteUserRequests": []
    }
`);

write(path.join(colBase, 'Security/get group.request.yaml'), `$kind: http-request
name: Get Group
method: GET
url: '{{baseUrl}}{{basePath}}/group/{{groupName}}'
order: 13000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: groupName
    value: 'myGroup'
`);

write(path.join(colBase, 'Security/get groups.request.yaml'), `$kind: http-request
name: Get Groups
method: GET
url: '{{baseUrl}}{{basePath}}/group'
order: 14000
headers:
  - key: Accept
    value: 'application/json'
queryParams:
  - key: groupPattern
    value: '%'
    disabled: true
`);

write(path.join(colBase, 'Security/delete group.request.yaml'), `$kind: http-request
name: Delete Group
method: DELETE
url: '{{baseUrl}}{{basePath}}/group/{{groupName}}'
order: 15000
pathVariables:
  - key: groupName
    value: 'myGroup'
`);

write(path.join(colBase, 'Security/refresh data management configurations.request.yaml'), `$kind: http-request
name: Refresh Data Management Configurations
method: POST
url: '{{baseUrl}}{{basePath}}/refreshDataManagementConfigurations'
order: 16000
`);

write(path.join(colBase, 'Security/get query configuration.request.yaml'), `$kind: http-request
name: Get Query Configuration
method: GET
url: '{{baseUrl}}{{basePath}}/queryConfig/{{path}}'
order: 17000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI'
`);

// ============================================================
// DATA MANAGEMENT FOLDER
// ============================================================
write(path.join(colBase, 'Data Management/.resources/definition.yaml'), `$kind: collection
name: Data Management
description: Collection and data object CRUD, permissions, downloads, and bulk operations.
order: 2000
`);

write(path.join(colBase, 'Data Management/interrogate path type.request.yaml'), `$kind: http-request
name: Interrogate Path Type
method: GET
url: '{{baseUrl}}{{basePath}}/pathType/{{path}}'
order: 1000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
`);

write(path.join(colBase, 'Data Management/register collection.request.yaml'), `$kind: http-request
name: Register Collection
method: PUT
url: '{{baseUrl}}{{basePath}}/collection/{{path}}'
order: 2000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
body:
  type: json
  content: |-
    {
      "metadataEntries": [
        { "attribute": "collection_type", "value": "Project" },
        { "attribute": "pi_name", "value": "John Doe" }
      ]
    }
`);

write(path.join(colBase, 'Data Management/get collection.request.yaml'), `$kind: http-request
name: Get Collection
method: GET
url: '{{baseUrl}}{{basePath}}/collection/{{path}}'
order: 3000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
queryParams:
  - key: list
    value: 'true'
    disabled: true
  - key: includeAcl
    value: 'false'
    disabled: true
`);

write(path.join(colBase, 'Data Management/get collection children.request.yaml'), `$kind: http-request
name: Get Collection Children
method: GET
url: '{{baseUrl}}{{basePath}}/collection/{{path}}/children'
order: 4000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
`);

write(path.join(colBase, 'Data Management/delete collection.request.yaml'), `$kind: http-request
name: Delete Collection
method: DELETE
url: '{{baseUrl}}{{basePath}}/collection/{{path}}'
order: 5000
pathVariables:
  - key: path
    value: 'NCI/myCollection'
queryParams:
  - key: recursive
    value: 'false'
    disabled: true
  - key: force
    value: 'false'
    disabled: true
`);

write(path.join(colBase, 'Data Management/move collection.request.yaml'), `$kind: http-request
name: Move Collection
method: POST
url: '{{baseUrl}}{{basePath}}/collection/{{path}}/move/{{destinationPath}}'
order: 6000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
  - key: destinationPath
    value: 'NCI/newLocation'
queryParams:
  - key: alignArchivePath
    value: 'false'
    disabled: true
`);

write(path.join(colBase, 'Data Management/download collection.request.yaml'), `$kind: http-request
name: Download Collection (v2)
method: POST
url: '{{baseUrl}}{{basePath}}/v2/collection/{{path}}/download'
order: 7000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
body:
  type: json
  content: |-
    {
      "destinationType": "GOOGLE_DRIVE",
      "googleDriveDestination": {
        "accessToken": "{{googleAccessToken}}",
        "folderName": "HPC-Download"
      }
    }
`);

write(path.join(colBase, 'Data Management/get collection download status.request.yaml'), `$kind: http-request
name: Get Collection Download Status
method: GET
url: '{{baseUrl}}{{basePath}}/collection/download/{{taskId}}'
order: 8000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: taskId
    value: 'task-id-here'
`);

write(path.join(colBase, 'Data Management/set collection permissions.request.yaml'), `$kind: http-request
name: Set Collection Permissions
method: POST
url: '{{baseUrl}}{{basePath}}/collection/{{path}}/acl'
order: 9000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
body:
  type: json
  content: |-
    {
      "userPermissions": [{ "userId": "userId1", "permission": "READ" }],
      "groupPermissions": []
    }
`);

write(path.join(colBase, 'Data Management/get collection permissions.request.yaml'), `$kind: http-request
name: Get Collection Permissions
method: GET
url: '{{baseUrl}}{{basePath}}/collection/{{path}}/acl'
order: 10000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
`);

write(path.join(colBase, 'Data Management/register data object.request.yaml'), `$kind: http-request
name: Register Data Object (v2)
method: PUT
url: '{{baseUrl}}{{basePath}}/v2/dataObject/{{path}}'
order: 11000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection/myFile.txt'
body:
  type: formdata
  content:
    - key: dataObjectRegistration
      type: text
      value: |-
        {
          "metadataEntries": [{ "attribute": "file_type", "value": "txt" }],
          "source": { "fileContainerId": "s3-bucket", "fileId": "path/to/file.txt" }
        }
      contentType: 'application/json'
`);

write(path.join(colBase, 'Data Management/get data object.request.yaml'), `$kind: http-request
name: Get Data Object
method: GET
url: '{{baseUrl}}{{basePath}}/dataObject/{{path}}'
order: 12000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection/myFile.txt'
queryParams:
  - key: includeAcl
    value: 'false'
    disabled: true
`);

write(path.join(colBase, 'Data Management/delete data object.request.yaml'), `$kind: http-request
name: Delete Data Object
method: DELETE
url: '{{baseUrl}}{{basePath}}/dataObject/{{path}}'
order: 13000
pathVariables:
  - key: path
    value: 'NCI/myCollection/myFile.txt'
`);

write(path.join(colBase, 'Data Management/download data object.request.yaml'), `$kind: http-request
name: Download Data Object (v2)
method: POST
url: '{{baseUrl}}{{basePath}}/v2/dataObject/{{path}}/download'
order: 14000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection/myFile.txt'
body:
  type: json
  content: |-
    {
      "destinationType": "GOOGLE_DRIVE",
      "googleDriveDestination": {
        "accessToken": "{{googleAccessToken}}",
        "folderName": "HPC-Download"
      }
    }
`);

write(path.join(colBase, 'Data Management/get data object download status.request.yaml'), `$kind: http-request
name: Get Data Object Download Status
method: GET
url: '{{baseUrl}}{{basePath}}/dataObject/download/{{taskId}}'
order: 15000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: taskId
    value: 'task-id-here'
`);

write(path.join(colBase, 'Data Management/set data object permissions.request.yaml'), `$kind: http-request
name: Set Data Object Permissions
method: POST
url: '{{baseUrl}}{{basePath}}/dataObject/{{path}}/acl'
order: 16000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection/myFile.txt'
body:
  type: json
  content: |-
    {
      "userPermissions": [{ "userId": "userId1", "permission": "READ" }],
      "groupPermissions": []
    }
`);

write(path.join(colBase, 'Data Management/bulk register data objects.request.yaml'), `$kind: http-request
name: Bulk Register Data Objects (v2)
method: PUT
url: '{{baseUrl}}{{basePath}}/v2/registration'
order: 17000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "dataObjectRegistrationItems": [
        {
          "path": "/NCI/myCollection/file1.txt",
          "metadataEntries": [{ "attribute": "file_type", "value": "txt" }],
          "source": { "fileContainerId": "s3-bucket", "fileId": "path/to/file1.txt" }
        }
      ]
    }
`);

write(path.join(colBase, 'Data Management/get bulk registration status.request.yaml'), `$kind: http-request
name: Get Bulk Registration Status (v2)
method: GET
url: '{{baseUrl}}{{basePath}}/v2/registration/{{taskId}}'
order: 18000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: taskId
    value: 'task-id-here'
`);

write(path.join(colBase, 'Data Management/bulk download data objects.request.yaml'), `$kind: http-request
name: Bulk Download Data Objects (v2)
method: POST
url: '{{baseUrl}}{{basePath}}/v2/download'
order: 19000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "dataObjectPaths": ["/NCI/myCollection/file1.txt"],
      "destinationType": "GOOGLE_DRIVE",
      "googleDriveDestination": {
        "accessToken": "{{googleAccessToken}}",
        "folderName": "HPC-Download"
      }
    }
`);

write(path.join(colBase, 'Data Management/move paths.request.yaml'), `$kind: http-request
name: Move Paths (Bulk)
method: POST
url: '{{baseUrl}}{{basePath}}/move'
order: 20000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "moveRequests": [
        {
          "sourcePath": "/NCI/myCollection/file1.txt",
          "destinationPath": "/NCI/newCollection/file1.txt"
        }
      ]
    }
`);

// ============================================================
// DATA SEARCH FOLDER
// ============================================================
write(path.join(colBase, 'Data Search/.resources/definition.yaml'), `$kind: collection
name: Data Search
description: Metadata queries, named queries, catalog search, and metadata attributes.
order: 3000
`);

write(path.join(colBase, 'Data Search/query collections.request.yaml'), `$kind: http-request
name: Query Collections
method: POST
url: '{{baseUrl}}{{basePath}}/collection/query'
order: 1000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "compoundQuery": {
        "operator": "AND",
        "queries": [
          { "attribute": "collection_type", "value": "Project", "operator": "EQUAL" }
        ]
      },
      "detailedResponse": false,
      "page": 1,
      "pageSize": 100,
      "totalCount": true
    }
`);

write(path.join(colBase, 'Data Search/query collections by named query.request.yaml'), `$kind: http-request
name: Query Collections By Named Query
method: GET
url: '{{baseUrl}}{{basePath}}/collection/query/{{queryName}}'
order: 2000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: queryName
    value: 'myQuery'
queryParams:
  - key: detailedResponse
    value: 'false'
    disabled: true
  - key: page
    value: '1'
    disabled: true
  - key: pageSize
    value: '100'
    disabled: true
  - key: totalCount
    value: 'true'
    disabled: true
`);

write(path.join(colBase, 'Data Search/query data objects.request.yaml'), `$kind: http-request
name: Query Data Objects
method: POST
url: '{{baseUrl}}{{basePath}}/dataObject/query'
order: 3000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
queryParams:
  - key: returnParent
    value: 'false'
    disabled: true
body:
  type: json
  content: |-
    {
      "compoundQuery": {
        "operator": "AND",
        "queries": [
          { "attribute": "file_type", "value": "txt", "operator": "EQUAL" }
        ]
      },
      "detailedResponse": false,
      "page": 1,
      "pageSize": 100,
      "totalCount": true
    }
`);

write(path.join(colBase, 'Data Search/query data objects by named query.request.yaml'), `$kind: http-request
name: Query Data Objects By Named Query
method: GET
url: '{{baseUrl}}{{basePath}}/dataObject/query/{{queryName}}'
order: 4000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: queryName
    value: 'myQuery'
queryParams:
  - key: detailedResponse
    value: 'false'
    disabled: true
  - key: page
    value: '1'
    disabled: true
  - key: pageSize
    value: '100'
    disabled: true
  - key: totalCount
    value: 'true'
    disabled: true
  - key: returnParent
    value: 'false'
    disabled: true
`);

write(path.join(colBase, 'Data Search/add named query.request.yaml'), `$kind: http-request
name: Add Named Query
method: PUT
url: '{{baseUrl}}{{basePath}}/query/{{queryName}}'
order: 5000
headers:
  - key: Content-Type
    value: 'application/json'
pathVariables:
  - key: queryName
    value: 'myQuery'
body:
  type: json
  content: |-
    {
      "compoundQuery": {
        "operator": "AND",
        "queries": [
          { "attribute": "collection_type", "value": "Project", "operator": "EQUAL" }
        ]
      },
      "detailedResponse": false,
      "page": 1,
      "pageSize": 100,
      "totalCount": true
    }
`);

write(path.join(colBase, 'Data Search/update named query.request.yaml'), `$kind: http-request
name: Update Named Query
method: POST
url: '{{baseUrl}}{{basePath}}/query/{{queryName}}'
order: 6000
headers:
  - key: Content-Type
    value: 'application/json'
pathVariables:
  - key: queryName
    value: 'myQuery'
body:
  type: json
  content: |-
    {
      "compoundQuery": {
        "operator": "AND",
        "queries": [
          { "attribute": "collection_type", "value": "Project", "operator": "EQUAL" }
        ]
      },
      "detailedResponse": false,
      "page": 1,
      "pageSize": 100,
      "totalCount": true
    }
`);

write(path.join(colBase, 'Data Search/get named query.request.yaml'), `$kind: http-request
name: Get Named Query
method: GET
url: '{{baseUrl}}{{basePath}}/query/{{queryName}}'
order: 7000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: queryName
    value: 'myQuery'
`);

write(path.join(colBase, 'Data Search/get all named queries.request.yaml'), `$kind: http-request
name: Get All Named Queries
method: GET
url: '{{baseUrl}}{{basePath}}/query'
order: 8000
headers:
  - key: Accept
    value: 'application/json'
`);

write(path.join(colBase, 'Data Search/delete named query.request.yaml'), `$kind: http-request
name: Delete Named Query
method: DELETE
url: '{{baseUrl}}{{basePath}}/query/{{queryName}}'
order: 9000
pathVariables:
  - key: queryName
    value: 'myQuery'
`);

write(path.join(colBase, 'Data Search/get metadata attributes.request.yaml'), `$kind: http-request
name: Get Metadata Attributes
method: GET
url: '{{baseUrl}}{{basePath}}/metadataAttributes'
order: 10000
headers:
  - key: Accept
    value: 'application/json'
queryParams:
  - key: levelLabel
    value: ''
    disabled: true
`);

write(path.join(colBase, 'Data Search/query catalog.request.yaml'), `$kind: http-request
name: Query Catalog
method: POST
url: '{{baseUrl}}{{basePath}}/catalog/query'
order: 11000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "path": "/NCI",
      "page": 1,
      "pageSize": 100,
      "totalCount": true
    }
`);

write(path.join(colBase, 'Data Search/email export query.request.yaml'), `$kind: http-request
name: Email Export Query
method: POST
url: '{{baseUrl}}{{basePath}}/emailExport/query'
order: 12000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "compoundQuery": {
        "operator": "AND",
        "queries": [
          { "attribute": "collection_type", "value": "Project", "operator": "EQUAL" }
        ]
      },
      "detailedResponse": true,
      "page": 1,
      "pageSize": 1000,
      "totalCount": true
    }
`);

// ============================================================
// DATA BROWSE FOLDER
// ============================================================
write(path.join(colBase, 'Data Browse/.resources/definition.yaml'), `$kind: collection
name: Data Browse
description: Bookmark management for quick access to frequently used paths.
order: 4000
`);

write(path.join(colBase, 'Data Browse/add bookmark.request.yaml'), `$kind: http-request
name: Add Bookmark
method: PUT
url: '{{baseUrl}}{{basePath}}/bookmark/{{bookmarkName}}'
order: 1000
headers:
  - key: Content-Type
    value: 'application/json'
pathVariables:
  - key: bookmarkName
    value: 'myBookmark'
body:
  type: json
  content: |-
    {
      "path": "/NCI/myCollection"
    }
`);

write(path.join(colBase, 'Data Browse/update bookmark.request.yaml'), `$kind: http-request
name: Update Bookmark
method: POST
url: '{{baseUrl}}{{basePath}}/bookmark/{{bookmarkName}}'
order: 2000
headers:
  - key: Content-Type
    value: 'application/json'
pathVariables:
  - key: bookmarkName
    value: 'myBookmark'
body:
  type: json
  content: |-
    {
      "path": "/NCI/newCollection"
    }
`);

write(path.join(colBase, 'Data Browse/get bookmark.request.yaml'), `$kind: http-request
name: Get Bookmark
method: GET
url: '{{baseUrl}}{{basePath}}/bookmark/{{bookmarkName}}'
order: 3000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: bookmarkName
    value: 'myBookmark'
`);

write(path.join(colBase, 'Data Browse/get all bookmarks.request.yaml'), `$kind: http-request
name: Get All Bookmarks
method: GET
url: '{{baseUrl}}{{basePath}}/bookmark'
order: 4000
headers:
  - key: Accept
    value: 'application/json'
`);

write(path.join(colBase, 'Data Browse/delete bookmark.request.yaml'), `$kind: http-request
name: Delete Bookmark
method: DELETE
url: '{{baseUrl}}{{basePath}}/bookmark/{{bookmarkName}}'
order: 5000
pathVariables:
  - key: bookmarkName
    value: 'myBookmark'
`);

// ============================================================
// NOTIFICATIONS FOLDER
// ============================================================
write(path.join(colBase, 'Notifications/.resources/definition.yaml'), `$kind: collection
name: Notifications
description: Notification subscription management and delivery receipt retrieval.
order: 5000
`);

write(path.join(colBase, 'Notifications/subscribe notifications.request.yaml'), `$kind: http-request
name: Subscribe Notifications
method: POST
url: '{{baseUrl}}{{basePath}}/notification'
order: 1000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
queryParams:
  - key: nciUserId
    value: ''
    disabled: true
body:
  type: json
  content: |-
    {
      "addSubscriptions": [
        {
          "eventType": "COLLECTION_UPDATED",
          "payloadEntries": [
            { "attribute": "collection_path", "value": "/NCI/myCollection" }
          ]
        }
      ],
      "deleteSubscriptions": []
    }
`);

write(path.join(colBase, 'Notifications/get notification subscriptions.request.yaml'), `$kind: http-request
name: Get Notification Subscriptions
method: GET
url: '{{baseUrl}}{{basePath}}/notification'
order: 2000
headers:
  - key: Accept
    value: 'application/json'
queryParams:
  - key: nciUserId
    value: ''
    disabled: true
`);

write(path.join(colBase, 'Notifications/get delivery receipts.request.yaml'), `$kind: http-request
name: Get Delivery Receipts
method: GET
url: '{{baseUrl}}{{basePath}}/notification/deliveryReceipts'
order: 3000
headers:
  - key: Accept
    value: 'application/json'
queryParams:
  - key: page
    value: '1'
    disabled: true
  - key: totalCount
    value: 'true'
    disabled: true
`);

write(path.join(colBase, 'Notifications/get delivery receipt.request.yaml'), `$kind: http-request
name: Get Delivery Receipt
method: GET
url: '{{baseUrl}}{{basePath}}/notification/deliveryReceipt'
order: 4000
headers:
  - key: Accept
    value: 'application/json'
queryParams:
  - key: eventId
    value: '1'
`);

// ============================================================
// REPORTS FOLDER
// ============================================================
write(path.join(colBase, 'Reports/.resources/definition.yaml'), `$kind: collection
name: Reports
description: Generate usage and activity reports.
order: 6000
`);

write(path.join(colBase, 'Reports/generate report.request.yaml'), `$kind: http-request
name: Generate Report
method: POST
url: '{{baseUrl}}{{basePath}}/report'
order: 1000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "type": "HPC_USAGE",
      "fromDate": "2024-01-01",
      "toDate": "2024-12-31",
      "doc": "NCI",
      "path": "/NCI"
    }
`);

// ============================================================
// DATA MIGRATION FOLDER
// ============================================================
write(path.join(colBase, 'Data Migration/.resources/definition.yaml'), `$kind: collection
name: Data Migration
description: Migrate data objects and collections between archive storage systems.
order: 7000
`);

write(path.join(colBase, 'Data Migration/migrate data object.request.yaml'), `$kind: http-request
name: Migrate Data Object
method: POST
url: '{{baseUrl}}{{basePath}}/dataObject/{{path}}/migrate'
order: 1000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection/myFile.txt'
body:
  type: json
  content: |-
    {
      "configurationId": "target-config-id"
    }
`);

write(path.join(colBase, 'Data Migration/migrate collection.request.yaml'), `$kind: http-request
name: Migrate Collection
method: POST
url: '{{baseUrl}}{{basePath}}/collection/{{path}}/migrate'
order: 2000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
body:
  type: json
  content: |-
    {
      "configurationId": "target-config-id"
    }
`);

write(path.join(colBase, 'Data Migration/bulk migrate.request.yaml'), `$kind: http-request
name: Bulk Migrate Data Objects or Collections
method: POST
url: '{{baseUrl}}{{basePath}}/migrate'
order: 3000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "dataObjectPaths": ["/NCI/myCollection/file1.txt"],
      "collectionPaths": [],
      "configurationId": "target-config-id"
    }
`);

write(path.join(colBase, 'Data Migration/retry data object migration.request.yaml'), `$kind: http-request
name: Retry Data Object Migration
method: POST
url: '{{baseUrl}}{{basePath}}/dataObject/migrate/{{taskId}}/retry'
order: 4000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: taskId
    value: 'task-id-here'
`);

write(path.join(colBase, 'Data Migration/retry collection migration.request.yaml'), `$kind: http-request
name: Retry Collection Migration
method: POST
url: '{{baseUrl}}{{basePath}}/collection/migrate/{{taskId}}/retry'
order: 5000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: taskId
    value: 'task-id-here'
queryParams:
  - key: failedItemsOnly
    value: 'true'
    disabled: true
`);

write(path.join(colBase, 'Data Migration/migrate metadata.request.yaml'), `$kind: http-request
name: Migrate Metadata
method: POST
url: '{{baseUrl}}{{basePath}}/migrateMetadata'
order: 6000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "fromConfigurationId": "source-config-id",
      "toConfigurationId": "target-config-id"
    }
`);

// ============================================================
// DATA TIERING FOLDER
// ============================================================
write(path.join(colBase, 'Data Tiering/.resources/definition.yaml'), `$kind: collection
name: Data Tiering
description: Tier data objects and collections to Glacier cold storage.
order: 8000
`);

write(path.join(colBase, 'Data Tiering/tier data object.request.yaml'), `$kind: http-request
name: Tier Data Object
method: POST
url: '{{baseUrl}}{{basePath}}/dataObject/{{path}}/tier'
order: 1000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection/myFile.txt'
`);

write(path.join(colBase, 'Data Tiering/tier collection.request.yaml'), `$kind: http-request
name: Tier Collection
method: POST
url: '{{baseUrl}}{{basePath}}/collection/{{path}}/tier'
order: 2000
headers:
  - key: Accept
    value: 'application/json'
pathVariables:
  - key: path
    value: 'NCI/myCollection'
`);

write(path.join(colBase, 'Data Tiering/bulk tier.request.yaml'), `$kind: http-request
name: Bulk Tier Data Objects or Collections
method: POST
url: '{{baseUrl}}{{basePath}}/tier'
order: 3000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
body:
  type: json
  content: |-
    {
      "dataObjectPaths": ["/NCI/myCollection/file1.txt"],
      "collectionPaths": []
    }
`);

// ============================================================
// REVIEW FOLDER
// ============================================================
write(path.join(colBase, 'Review/.resources/definition.yaml'), `$kind: collection
name: Review
description: Review workflow management for data curation.
order: 9000
`);

write(path.join(colBase, 'Review/query review.request.yaml'), `$kind: http-request
name: Query Review
method: POST
url: '{{baseUrl}}{{basePath}}/review/query'
order: 1000
headers:
  - key: Content-Type
    value: 'application/json'
  - key: Accept
    value: 'application/json'
queryParams:
  - key: projectStatus
    value: 'ACTIVE'
    disabled: true
  - key: dataCurator
    value: ''
    disabled: true
`);

write(path.join(colBase, 'Review/send review reminder.request.yaml'), `$kind: http-request
name: Send Review Reminder
method: POST
url: '{{baseUrl}}{{basePath}}/review/sendReminder'
order: 2000
headers:
  - key: Accept
    value: 'application/json'
body:
  type: text
  content: 'userId'
`);

// ============================================================
// ENVIRONMENTS
// ============================================================
write(path.join(envBase, 'Local.yaml'), `name: Local
values:
  - key: baseUrl
    value: 'https://localhost:7738'
    enabled: true
    type: default
  - key: basePath
    value: '/hpc-server'
    enabled: true
    type: default
  - key: username
    value: ''
    enabled: true
    type: default
  - key: password
    value: ''
    enabled: true
    type: default
  - key: token
    value: ''
    enabled: true
    type: default
  - key: googleAccessToken
    value: ''
    enabled: true
    type: default
`);

write(path.join(envBase, 'Dev.yaml'), `name: Dev
values:
  - key: baseUrl
    value: 'https://hpc-dev.nci.nih.gov'
    enabled: true
    type: default
  - key: basePath
    value: '/hpc-server'
    enabled: true
    type: default
  - key: username
    value: ''
    enabled: true
    type: default
  - key: password
    value: ''
    enabled: true
    type: default
  - key: token
    value: ''
    enabled: true
    type: default
  - key: googleAccessToken
    value: ''
    enabled: true
    type: default
`);

console.log('\n✅ Done! All files created successfully.');
console.log('\nTo run this script: node create-hpc-collection.js');
