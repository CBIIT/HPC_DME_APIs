var scope = [ 'https://www.googleapis.com/auth/drive.file' ];
var pickerApiLoaded = false;

function loadDownloadPicker() {
	gapi.load('picker', {
		'callback' : onDownloadPickerApiLoad
	});
	gapi.load('client', start);
}

function onDownloadPickerApiLoad() {
	pickerApiLoaded = true;
	createDownloadPicker();
}

function start() {
	gapi.client.setToken({
		access_token : oauthToken
	})
}

function createDownloadPicker() {
	if (pickerApiLoaded && oauthToken) {
		var view = new google.picker.DocsView(google.picker.ViewId.FOLDERS);
		view.setIncludeFolders(true);
		view.setSelectFolderEnabled(true);
		view.setParent("root");
		var picker = new google.picker.PickerBuilder()
		.setOAuthToken(oauthToken).addView(view)
		.setCallback(downloadPickerCallback).setTitle(
				"Select Folder").build();
		picker.setVisible(true);
	}
}

// Callback implementation for download picker.
function downloadPickerCallback(data) {
	if (data.action == google.picker.Action.PICKED) {
		var fileId = data.docs[0].id;
		constructPath(fileId, "");
	}
}

/**
 * Construct full path using file's parents.
 *
 * @param {String} fileId ID of the file/folder.
 * @param {String} path Child path to prepend to.
 */
function constructPath(fileId, path) {
	gapi.client.load('drive', 'v2', function() {
		var request = gapi.client.drive.files.get({
			'fileId' : fileId
		});
		request.execute(function(resp) {
			console.log('Folder: ' + resp.title);
			path = resp.title + '/' + path
			if (resp.parents[0]) {
				if (resp.parents[0].isRoot) {
					console.log('The user selected: ' + path);
					if(downloadType == "datafile")
						path = path + downloadFileName;
					$("#drivePath").val(path);
				} else {
					constructPath(resp.parents[0].id, path)
				}
			}
		});
	});
}