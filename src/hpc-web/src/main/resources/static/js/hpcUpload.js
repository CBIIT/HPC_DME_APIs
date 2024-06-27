var scope = [ 'https://www.googleapis.com/auth/drive.file' ];
var pickerApiLoaded = false;

function loadUploadPicker() {
	gapi.load('picker', {
		'callback' : onUploadPickerApiLoad
	});
	gapi.load('client', start);
}

function onUploadPickerApiLoad() {
	pickerApiLoaded = true;
	createUploadPicker();
}

function start() {
	gapi.client.setToken({
		access_token : oauthToken
	})
}

function createUploadPicker() {
	if (pickerApiLoaded && oauthToken) {
		var view = new google.picker.DocsView(google.picker.ViewId.DOCS);
		view.setIncludeFolders(true);
		view.setSelectFolderEnabled(true);
		view.setParent("root");
		var picker = new google.picker.PickerBuilder()
		.setOAuthToken(oauthToken).addView(view)
		.enableFeature(google.picker.Feature.MULTISELECT_ENABLED)
		.setCallback(uploadPickerCallback).setTitle(
				"Select Files or Folder").build();
		picker.setVisible(true);
	}
}

// Callback implementation for download picker.
function uploadPickerCallback(data) {
	if (data.action == google.picker.Action.PICKED) {
		// get all selected files
        var files = data[google.picker.Response.DOCUMENTS];
        
        // loop over selected files 
        for (var i = 0; i < files.length; i++) {
            populateSelection(files[i][google.picker.Document.ID])
        }
	}
}

/**
 * Populate selected file/folder.
 *
 * @param {String} fileId ID of the file/folder.
 */
function populateSelection(fileId) {
	gapi.client.load('drive', 'v3', function() {
		var request = gapi.client.drive.files.get({
			'fileId' : fileId
		});
		$("#fileNamesDiv").html('<label for="fileName"><b>Selected Files:</b></label>');
		$("#folderNamesDiv").html('<br /><label for="folderName"><b>Selected Folders:</b></label>');
		request.execute(function(resp) {
			if (resp.mimeType == 'application/vnd.google-apps.folder') {
				console.log('Folder: ' + resp.name);
				var folderHtml = '<div><label id="' + resp.id + '_label"></label><input type="hidden" id="' + resp.id + '" name="folderNames"></div>'
				+ '<input type="hidden" name="folderIds" value="' + resp.id + '">';
				$("#folderNamesDiv").append(folderHtml);
				$("#folderNamesDiv").show();
			} else {
				console.log('File: ' + resp.name);
				var fileHtml = '<div><label id="' + resp.id + '_label"></label><input type="hidden" id="' + resp.id + '" name="fileNames"></div>'
				+ '<input type="hidden" name="fileIds" value="' + resp.id + '">';
				$("#fileNamesDiv").append(fileHtml);
				$("#fileNamesDiv").show();
			}
			constructPath(resp.id, "", resp.id);
		});
	});
}

/**
 * Construct full path using file's parents.
 *
 * @param {String} fileId ID of the file/folder.
 * @param {String} path Child path to prepend to.
 */
function constructPath(fileId, path, origFileId) {
	gapi.client.load('drive', 'v3', function() {
		var request = gapi.client.drive.files.get({
			'fileId' : fileId
		});
		request.execute(function(resp) {
			console.log('Folder: ' + resp.name);
			var folder = resp.name;
			request = gapi.client.drive.files.get({
				'fileId' : fileId,
				'fields' : 'parents'
			});
			request.execute(function(resp) {
			if (resp) {
				if (resp.parents) {
					if (fileId == origFileId)
						path = folder;
					else
						path = folder + '/' + path;
					constructPath(resp.parents[0], path, origFileId)
				} else {
					console.log('The user selected: ' + path);
					$("#" + origFileId + "_label").html(path);
					$("#" + origFileId).val(path);
				}
			}
			});
		});
	});
}