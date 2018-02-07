import globus_sdk
import json

####################################################################################################
####################################################################################################
#
# Script: process_join_group.py
#
#
# Description: 
#     Processes an input JSON file and uses that data to attempt to add specified Globus 
#     application accounts to specified Globus groups.  Input file is expected to be named
#     inputGroupAdds.json and is expected to be in same directory as this script.
# 
# Assumptions:
#     1.  Input JSON file is named inputGroupAdds.json and is in same directory as this script
#
#     2.  Globus group IDs, Globus client IDs, and Globus client secrets in input JSON file are accurate.
#
#     3.  Globus groups are properly configured to allow appropriate Globus application accounts to join
#         without any human-in-the-loop intervention.
#
#     4.  Contents of input JSON file are expected to be in the following format (example) with placeholders
#         for Globus group UUIDs, Globus application accounts' client IDs, and Globus application accounts' 
#         client secrets:
# 
#         {
#           "addGroupMemberBundles": [
#             {
#               "groupId": "<UUID of some Globus group>",
#               "clientsToAdd": [
#                 {
#                   "clientId": "<client ID of some Globus application account>",
#                   "clientSecret": "<a client secret>"
#                 },
#                 {
#                   "clientId": "<client ID of some Globus application account>",
#                   "clientSecret": "<a client secret>"
#                 },
#                 {
#                   "clientId": "<client ID of some Globus application account>",
#                   "clientSecret": "<a client secret>"
#                 }
#               ]
#             },
#             {
#               "groupId": "<UUID of some Globus group>",
#               "clientsToAdd": [
#                 {
#                   "clientId": "<client ID of some Globus application account>",
#                   "clientSecret": "<a client secret>"
#                 },
#                 {
#                   "clientId": "<client ID of some Globus application account>",
#                   "clientSecret": "<a client secret>"
#                 },
#                 {
#                   "clientId": "<client ID of some Globus application account>",
#                   "clientSecret": "<a client secret>"
#                 }
#               ]
#             }
#           ]
#         }
#
# Invocation:  Call the script without any arguments.
#
# Original Author: William Liu
#
# Original Date: January 9, 2018
# 
# Revisions:
#
#   Author                Date                         Comments
#--------------------------------------------------------------------------------------------------
#   William Liu           February 7, 2018             Modified add_client_to_group function to
#                                                      fix errors that were experienced with prior
#                                                      revision's implementation of the function.
#                                                      The modified code was sent by Globus Support
#                                                      in response to support request.
#                                                      
#                                                      Changed sample JSON in this comment block to
#                                                      remove actual Globus group UUIDs, Globus 
#                                                      application account client IDs, and Globus
#                                                      application account client secrets.
#
####################################################################################################
####################################################################################################

# Consider the following variables to be constants
CLIENT_IDENTITY_USERNAME_TEMPLATE = '{}@clients.auth.globus.org'
GROUPS_API_URL = 'https://nexus.api.globusonline.org'
GROUPS_SCOPE = 'urn:globus:auth:scope:nexus.api.globus.org:groups'
JOIN_GROUP_CALL = '/groups/{group_id}/members'
JSON_INPUT_FILE = 'inputGroupAdds.json'

def join_group_payload(username):
    return {
        'users': [username]
    }


def gen_groups_client(client_id, client_secret):
    auth_client = globus_sdk.ConfidentialAppAuthClient(client_id, client_secret)

    token_response = auth_client.oauth2_client_credentials_tokens(
      requested_scopes=GROUPS_SCOPE).by_resource_server['nexus.api.globus.org']

    authorizer = globus_sdk.AccessTokenAuthorizer(token_response['access_token'])

    groups_client = globus_sdk.base.BaseClient(
      'groups', base_url=GROUPS_API_URL, authorizer=authorizer)

    return groups_client


def add_client_to_group(target_group_id, target_client_id, target_client_secret):
    groups_client = gen_groups_client(target_client_id, target_client_secret)
    try:
        client_username = CLIENT_IDENTITY_USERNAME_TEMPLATE.format(target_client_id)
        res = groups_client.post(
                JOIN_GROUP_CALL.format(group_id=target_group_id),
                join_group_payload(client_username))
    except globus_sdk.GlobusAPIError:
        try:
            res = groups_client.get('{}/{}'.format(
                    JOIN_GROUP_CALL.format(group_id=target_group_id),
                    client_username))
        except globus_sdk.GlobusAPIError as e:
            print('Error joining group:\n{}'.format(e))
            raise

    try:
        status = res.data['members'][0]['status']
    except KeyError:
        status = res.data['status']

    return status == 'active'


def main():
    # Assume file to read is in same directory as this script
    with open(JSON_INPUT_FILE, 'r') as myInFile:
        groupAddData = myInFile.read()

    # Load the file data as JSON
    groupAddJson = json.loads(groupAddData)

    # Add client to group for each received bundle of a group with additional clients to add as members
    for groupAddItem in groupAddJson['addGroupMemberBundles']:
        for clientCred in groupAddItem['clientsToAdd']:
            signal = add_client_to_group(groupAddItem['groupId'], clientCred['clientId'], clientCred['clientSecret'])
            if signal == True:
                print('{} has ***successfully*** joined group {}'.format(clientCred['clientId'], groupAddItem['groupId']))
            else:
                print('{} has FAILED to joined group {}'.format(clientCred['clientId'], groupAddItem['groupId']))

main()