// from https://stackoverflow.com/a/105074/1251543
function guid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
            .toString(16)
            .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
}

NO_TOKEN = "-no-token-";
function get_token() {
    return sessionStorage.getItem('histori_session') || NO_TOKEN;
}

function add_api_auth (xhr) {
    var token = get_token();
    xhr.setRequestHeader(Api.API_TOKEN, token);
}

// Must match what is in histori-config.yml, then add a trailing slash here
API_PREFIX = "/api/";

Api = {
    // must match API_TOKEN in ApiConstants.java
    API_TOKEN: 'x-wordland-api-key',

    _get: function (url, success, fail) {
        var result = null;
        $.ajax({
            type: 'GET',
            url: API_PREFIX + url,
            async: (typeof success != "undefined" && success != null),
            beforeSend: add_api_auth,
            success: function (data, status, jqXHR) {
                result = data;
                if (typeof success != "undefined" && success != null) success(result);
            },
            error: function (jqXHR, status, error) {
                if (jqXHR.status == 200 && typeof success != "undefined" && success != null) {
                    success(jqXHR.responseText);
                } else if (typeof fail != "undefined" && fail != null) {
                    console.log('_get error: status='+status+' (jqXHR.status='+jqXHR.status+'), error='+error);
                    fail(jqXHR, status, error);
                }
            }
        });
        return result;
    },

    _update: function (method, url, data, success, fail, skipTokenCheck) {
        if (get_token() == NO_TOKEN && (typeof skipTokenCheck == "undefined" || skipTokenCheck == false)) Api.create_anonymous_session();
        var result = null;
        $.ajax({
            type: method,
            url: API_PREFIX + url,
            async: (typeof success != "undefined" && success != null),
            contentType: 'application/json',
            data: JSON.stringify(data),
            beforeSend: add_api_auth,
            success: function (data, status, jqXHR) {
                result = data;
                if (typeof success != "undefined" && success != null) success(result);
            },
            error: function (jqXHR, status, error) {
                console.log('_update error: status='+status+' (jqXHR.status='+jqXHR.status+'), error='+error);
                if (typeof fail != "undefined" && fail != null) fail(jqXHR, status, error);
            }
        });
        return result;
    },

    _post: function(url, data, success, fail, skipTokenCheck) { return Api._update('POST', url, data, success, fail, skipTokenCheck); },
    _put:  function(url, data, success, fail) { return Api._update('PUT', url, data, success, fail); },

    _delete: function (url, success, fail) {
        var ok = false;
        $.ajax({
            type: 'DELETE',
            url: API_PREFIX + url,
            async: (typeof success != "undefined" && success != null),
            beforeSend: add_api_auth,
            'success': function (accounts, status, jqXHR) {
                ok = true;
                if (typeof success != "undefined" && success != null) success();
            },
            'error': function (jqXHR, status, error) {
                if (jqXHR.status == 200 && typeof success != "undefined" && success != null) {
                    success();
                } else if (typeof fail != "undefined" && fail != null) {
                    console.log('_delete error: status='+status+' (jqXHR.status='+jqXHR.status+'), error='+error);
                    fail(jqXHR, status, error);
                }
            }
        });
        return ok;
    },

    login: function (email, password, success, fail) {
        return Api._post('accounts/login', {'name': email, 'password': password}, success, fail);
    },

    register: function (reg, success, fail) {
        return Api._post('accounts/register', reg, success, fail);
    },

    forgot_password: function (email, success, fail) {
        Api._post('accounts/forgot_password', email, success, fail);
    },

    reset_password: function (key, password, success, fail) {
        Api._post('accounts/reset_password', {'token': key, 'password': password}, success, fail);
    },

    update_account: function (info, success, fail) {
        Api._post('accounts', info, success, fail);
    },

    remove_account: function (info, success, fail) {
        Api._post('accounts/remove', info, success, fail);
    },

    get_config: function (name, success, fail) {
        return Api._get('configs/'+name, success, fail);
    },

    list_rooms: function (room_name, player_info, success, fail) {
        Api._get('rooms', success, fail);
    },

    join_game: function (room_name, player_info, success, fail) {
        Api._post('rooms/' + room_name + '/join', player_info, success, fail);
    },

    // Bookmark endpoints
    get_bookmarks: function (success, fail) {
        Api._get('bookmarks', success, fail);
    },
    get_bookmark: function (name, success, fail) {
        Api._get('bookmarks/'+encodeURIComponent(name), success, fail);
    },
    add_bookmark: function (name, state, success, fail) {
        Api._put('bookmarks/'+encodeURIComponent(name), state, success, fail);
    },
    update_bookmark: function (name, state, success, fail) {
        Api._post('bookmarks/'+encodeURIComponent(name), state, success, fail);
    },
    remove_bookmark: function (name, success, fail) {
        Api._delete('bookmarks/'+encodeURIComponent(name), success, fail);
    },
    make_permalink: function (name, success, fail) {
        Api._get('bookmarks/'+encodeURIComponent(name)+'/permalink', success, fail);
    },
    get_permalink: function (link_id, success, fail) {
        Api._get('permalinks/'+encodeURIComponent(link_id), success, fail);
    },
    get_standard_permalinks: function (success, fail) {
        Api._get('permalinks', success, fail);
    },

    transform_image: function (src, width, height) {
        return src.replace("/public/image/", "/public/transform/w_"+width+"-h_"+height+"/");
    }

    //addRegion: function (email, password) { return Api._post('/api/accounts/' + email, {'name': email, 'password': password}); }

};