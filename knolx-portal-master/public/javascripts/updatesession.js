var cancel = false;
var uploading = false;
var redirect = true;
var newVideoURL = "";
var files = 0;

Dropzone.autoDiscover = false;

window.onbeforeunload = function () {
    if (!redirect) {
        return 'The file upload is still going on. If you leave the page now your upload' +
            'will be cancelled. Are you sure you want to leave the page?';
    }
};

$(function () {
    var sessionId = $('input[name^="sessionId"]').val();
    checkIfTemporaryUrlExists(sessionId);

    var youtubeDropzone = new Dropzone("#youtubeVideo", {
        url: jsRoutes.controllers.YoutubeController.upload(sessionId).url,
        maxFilesize: 2048,
        dictDefaultMessage: "Drop your file here to upload(or click)",
        uploadMultiple: false,
        headers: {
            'CSRF-Token': document.getElementById('csrfToken').value
        },
        autoProcessQueue: false,
        maxFiles: 1,
        acceptedFiles: ".mov, .mpeg4, .mp4, .avi, .wmv, .mpegps, .flv, .3gpp, .webm",
        timeout: 0,
        init: function() {
          this.on("maxfilesexceeded", function(file) {
                this.removeAllFiles();
                this.addFile(file);
          });
        }
    });

    youtubeDropzone.on("sending", function (file, xhr, formData) {
        redirect = false;
        $("#uploadVideo").attr('disabled', true);
        $("#updateVideo").attr('disabled', true);
        xhr.setRequestHeader("filesize", file.size);
        var tags = $("#youtube-tags").val().split(",");
        for(var i=0 ; i<tags.length ; i++) {
            formData.append("tags[]", tags[i]);
        }
        formData.append("title", $("#youtube-title").val());
        formData.append("description", $("#youtube-description").val());
        formData.append("category", $("#youtube-category").val());
        formData.append("status", $("#youtube-status").val());
    });

    youtubeDropzone.on("complete", function (file) {
        redirect = true;
        uploading = true;
        $("#cancel-message").hide();
    });

    youtubeDropzone.on("success", function (file, response) {
        $("#show-progress").show();
        $("#cancel-video-button").show();
        $("#progress-message").show();
        cancel = false;
        $("#youtube-dropzone").hide();
        showProgress(sessionId);
    });

    youtubeDropzone.on("error", function (file, response) {
        $("#upload-video-button").show();
        files -= 1;
    });

    youtubeDropzone.on("addedfile", function(file) {
        files += 1;
    })

    $("#upload-success-message").hide();
    $("#cancel-video-button").hide();

    jsRoutes.controllers.YoutubeController.checkIfUploading(sessionId).ajax(
        {
            type: 'GET',
            processData: false,
            contentType: false,
            success: function (data) {
                $("#upload-success-message").hide();
                $("#already-upload").hide();
                $("#no-upload-cancel").hide();
                $("#cancel-message").hide();
                $("#show-progress").show();
                $("#cancel-video-button").show();
                $("#youtube-dropzone").hide();
                uploading = true;
                showProgress(sessionId);
            }
        });

    $("#cancelVideo").click(function () {
        if (uploading) {
            cancel = true;
            uploading = false;
            files -= 1;
            $("#upload-success-message").hide();
            cancelVideo(sessionId);
        } else {
            $("#upload-success-message").hide();
            $("#cancel-message").hide();
            $("#no-upload-cancel").show();
        }
    });

    $("#updateVideo").click(function () {
        $("#unsuccessful-update").hide();
        update(sessionId);
    });

    $(".bootstrap-tagsinput").addClass('update-field');

    $(".bootstrap-tagsinput input").keypress(function (event) {
        if (event.keyCode == 13) {
            event.preventDefault();
        }
    });

    $("#uploadVideo").click(function () {
        if(files >= 1) {
            youtubeDropzone.processQueue();
        } else {
            $("#no-file-added").show();
        }
    });

    $("#attach-video-link").click(function () {
        $("#youtubeURL").val("www.youtube.com/embed/" + newVideoURL);
    });

});

function showProgress(sessionId) {
    jsRoutes.controllers.YoutubeController.getPercentageUploaded(sessionId).ajax(
        {
            type: 'GET',
            processData: false,
            contentType: false,
            beforeSend: function (request) {
                var csrfToken = document.getElementById('csrfToken').value;

                return request.setRequestHeader('CSRF-Token', csrfToken);
            },
            success: function (data) {
                if (!cancel) {
                    if (data == 100) {
                        $("#upload-success-message").show();
                        $("#show-progress").hide();
                        $("#progress").width('0%');
                        $("#progress").text('0%');
                        uploading = false;
                        $("#youtube-dropzone").show();
                        $("#cancel-video-button").hide();
                        $("#progress-message").hide();
                        $("#uploadVideo").attr('disabled', false);
                        $("#updateVideo").attr('disabled', false);
                        getUpdateURL(sessionId);
                    } else {
                        var percentageUploaded = data;
                        $("#progress").width(percentageUploaded + '%');
                        $("#progress").text(Math.ceil(percentageUploaded) * 1 + '%');
                        showProgress(sessionId);
                    }
                }
            },
            error: function (er) {
                console.log("Show Progress failed with error = " + er.responseText);
            }
        });
}

function cancelVideo(sessionId) {

    jsRoutes.controllers.YoutubeController.cancel(sessionId).ajax(
        {
            type: 'GET',
            processData: false,
            contentType: false,
            success: function (data) {
                $("#upload-success-message").hide();
                $("#upload-failure-message").hide();
                $("#show-progress").hide();
                $("#progress").width('0%');
                $("#progress").text('0%');
                $("#cancel-message").show();
                $("#youtube-dropzone").show();
                $("#cancel-video-button").hide();
                $("#upload-video-button").show();
                $("#uploadVideo").attr('disabled', false);
                $("#updateVideo").attr('disabled', false);
            },
            error: function (er) {
                $("#upload-success-message").hide();
                $("#upload-failure-message").hide();
                $("#show-progress").hide();
                $("#progress").width('0%');
                $("#progress").text('0%');
                $("#cancel-message").show();
            }
        })
}

function getUpdateURL(sessionId) {
    jsRoutes.controllers.YoutubeController.getVideoId(sessionId).ajax(
        {
            type: 'GET',
            processData: false,
            contentType: false,
            success: function (data) {
                if(data == "No new video URL found") {
                    getUpdateURL(sessionId);
                } else {
                    newVideoURL = data;
                    $("#attach-video").show();
                }
            },
            error: function (er) {
                console.log("Couldn't find a new URL")
            }
        });
}

function getUrl(file) {
    var fileSize = file.size;
    var url = "/youtube/" + sessionId + "/" + fileSize + "/upload";
    return url;
}

function update(sessionId) {
    var title = $("#youtube-title").val();
    var description = $("#youtube-description").val();
    var tags = $("#youtube-tags").val().split(",");
    var status = $("#youtube-status").val();
    var category = $("#youtube-category").val();

    var formData = {
        "title": title,
        "description": description,
        "tags": tags,
        "status": status,
        "category": category
    };

    jsRoutes.controllers.YoutubeController.updateVideo(sessionId).ajax(
        {
            type: 'POST',
            processData: false,
            contentType: 'application/json',
            data: JSON.stringify(formData),
            beforeSend: function (request) {
                var csrfToken = document.getElementById('csrfToken').value;

                return request.setRequestHeader('CSRF-Token', csrfToken);
            },
            success: function (data) {
                $("#successful-update").show();
            },
            error: function (er) {
                $("#unsuccessful-update").show();
            }
        });
}

function checkIfTemporaryUrlExists(sessionId) {
    jsRoutes.controllers.YoutubeController.checkIfTemporaryUrlExists(sessionId).ajax(
        {
            type: 'GET',
            processData: false,
            contentType: false,
            success: function (data) {
                newVideoURL = data;
                $("#attach-video").show();
            }
        });
}
