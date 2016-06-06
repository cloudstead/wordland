Wordland = {

    room: null,
    player: null,

    apiError: function (message) {
        return function (jqXHR, status, error) {
            if (jqXHR.status != 422) {
                // any other error besides validation
                Wordland.globalError(message + ' (status ' + jqXHR.status + '): ' + jqXHR.responseText + ')');
                return;
            }
            Wordland.validationErrors(jqXHR.responseJSON);
        }
    },

    globalError: function (message) {
        var err = $('#wordland_global_error');
        err.html(message);
        err.css({visibility: 'visible'});
    },

    validationErrors: function (message) {
        // todo: highlight fields that have errors
        var err = $('#wordland_global_error');
        err.html(message);
        err.css({visibility: 'visible'});
    },

    showScreen: function (name) {
        $('.dialogContainer').css({visibility: 'hidden'});
        $('#'+name).css({visibility: 'visible'});
    },

    join: function (room, player_info) {
        if (Wordland.player != null) {
            Wordland.globalError('game already in progress');

        } else {
            Api.join_game(room, player_info, function (data) {
                Wordland.room = room;
                Wordland.player = data;
                WLGame.start();

            }, Wordland.apiError("error joining game"));
        }
    },

    quit: function () {
        if (Wordland.player == null) {
            Wordland.globalError('no game in progress');

        } else {
            Api.quit_game(Wordland.room, Wordland.player.id, function (data) {
                Wordland.room = null;
                Wordland.player = null;
                Wordland.showScreen('lobbyContainer');

            }, Wordland.apiError("error quitting game"));
        }
    }

};

$(function() {
    Api.list_rooms(function (data) {
        if (is_array(data)) {
            WordlandLobby.refreshRooms(data);
            Wordland.showScreen('lobbyContainer')
        }

    }, Wordland.apiError("error listing rooms"));
});