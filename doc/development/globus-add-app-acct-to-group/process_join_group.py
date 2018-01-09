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
#     4.  Contents of input JSON file are expected to be in the following format (example):
# 
#         {
#           "addGroupMemberBundles": [
#             {
#               "groupId": "a6da7308-eca5-11e7-a8a1-0ea08e639e9e",
#               "clientsToAdd": [
#                 {
#                   "clientId": "50d777e0-5ee2-4af6-b6d6-741093187267",
#                   "clientSecret": "UqS4tNZI/Vy42XM+6AmC2MzBQO39spxT4jWcxH/3vm0="
#                 },
#                 {
#                   "clientId": "554a5aed-e93f-436a-af63-cb818d2de823",
#                   "clientSecret": "DPWfOKKaG6LjndCsF6ywSFReRq0W+JFF9+Xm+gMnLAs="
#                 },
#                 {
#                   "clientId": "3fa453a9-202b-471f-a972-55e25a31a1fd",
#                   "clientSecret": "WpNCLuwmC7Reaf/27GRyyG2rIWUuFJEIqbd49JdnT+0="
#                 }
#               ]
#             },
#             {
#               "groupId": "5f8eae9c-f55b-11e7-a756-0af7ae577cc6",
#               "clientsToAdd": [
#                 {
#                   "clientId": "d25efd0e-1e05-4f04-b246-329ab54d2eea",
#                   "clientSecret": "qp2GAacLbmBqS/Zdv86Vg/I4lgxPdxnv+B29lKhUONQ="
#                 },
#                 {
#                   "clientId": "399b1e76-b57f-491a-ae28-3f8b8d3c4a58",
#                   "clientSecret": "bbjcLR2F2/N4Nmr8AOT85bAa/JRmi8kY+dfQrEKKZVM="
#                 },
#                 {
#                   "clientId": "417b7f4d-6e4b-447e-aeca-462cbbe4b07d",
#                   "clientSecret": "Rla57tCdUKmRX01WA0Wm6UVIxwT8Qzhi0yMbsz4iRes="
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
    ret_signal = False
    groups_client = gen_groups_client(target_client_id, target_client_secret)
    try:
        client_username = CLIENT_IDENTITY_USERNAME_TEMPLATE.format(target_client_id)
        res = groups_client.post(
                JOIN_GROUP_CALL.format(group_id=target_group_id),
                join_group_payload(client_username))
        ret_signal = True
    except globus_sdk.GlobusAPIError as e:
        print('Error joining group:\n{}'.format(e))
        raise

    return ret_signal


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