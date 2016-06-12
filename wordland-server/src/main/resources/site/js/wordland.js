
Wordland = {

    room: null,
    player: null,
    clientId: guid(),

    eventsApi: function () {
        var host = document.location.host;
        if (host.indexOf(':') != -1) {
            host = host.substring(0, host.indexOf(':'));
        }
        return document.location.protocol + "//" + host + ":9099";
    },

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
        const err = $('#wordland_global_error');
        err.html(message);
        err.css({visibility: 'visible'});
    },

    validationErrors: function (message) {
        // todo: highlight fields that have errors
        const err = $('#wordland_global_error');
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
                Wordland.send({type: 'player_joined', id: Wordland.player.id, apiKey: Wordland.player.apiKey, room: Wordland.room});
                WLGame.start();

            }, Wordland.apiError("error joining game"));
        }
    },

    quit: function () {
        if (Wordland.player == null) {
            Wordland.globalError('no game in progress');

        } else {
            Api.quit_game(Wordland.room, Wordland.player.id, Wordland.player.apiKey, function (data) {
                Wordland.room = null;
                Wordland.player = null;
                Wordland.showScreen('lobbyContainer');

            }, Wordland.apiError("error quitting game"));
        }
    },

    socket: null,

    toMessage: function (message) {
        message.id = Wordland.player.id;
        message.apiKey = Wordland.player.apiKey;
        message.clientId = Wordland.clientId;
        message.room = Wordland.room;
        return JSON.stringify(message);
    },

    send: function (obj) {
        if (obj) Wordland.socket.push(Wordland.toMessage(obj));
    }

};

$(function() {
    "use strict";

    $('.lobbyControl').attr('disabled', 'disabled');

    Api.list_rooms(function (data) {
        if (is_array(data)) {
            WordlandLobby.refreshRooms(data);
            Wordland.showScreen('lobbyContainer')
        }
    }, Wordland.apiError("error listing rooms"));

    var logged = false;
    var socket = atmosphere;
    var transport = 'websocket';

    // We are now ready to cut the request
    var request = { url: Wordland.eventsApi()+'/events/'+Wordland.clientId,
        contentType: "application/json",
        logLevel: 'debug',
        transport: transport,
        trackMessageLength: true,
        enableProtocol: true,
        fallbackTransport: 'long-polling'};


    request.onOpen = function (response) {
        console.log('Atmosphere connected using ' + response.transport);
        $('.lobbyControl').removeAttr('disabled');
        $('#player_name').focus();
        transport = response.transport;
    };

    // For demonstration of how you can customize the fallbackTransport using the onTransportFailure function
    request.onTransportFailure = function (errorMsg, request) {
        atmosphere.util.info(errorMsg);
        if (window.EventSource) {
            request.fallbackTransport = "sse";
        }
        $('.lobbyControl').attr('disabled', 'disabled');
        console.log('Default transport is WebSocket, fallback is ' + request.fallbackTransport);
    };

    request.onMessage = function (response) {
        console.log('onMessage: '+response.responseBody);
        var obj = JSON.parse(response.responseBody);
        try {
            if (obj.stateChange) {
                WLGame.applyStateChange(obj);
            } else if (obj.notification) {
                switch (obj.notification) {
                    case 'invalid_word':
                        $("#game_tray").fadeIn(100).fadeOut(100).fadeIn(100).fadeOut(100).fadeIn(100);
                        break;
                    default:
                        console.log('unsupported notification type: '+obj.notification);
                }
            } else {
                console.log('unsupported message object: '+obj);
            }
        } catch (e) {
            console.log('This doesn\'t look like valid JSON: ', message.data);
        }
    };

    request.onClose = function (response) {
        console.log('onClose: closing');
        logged = false;
    };

    request.onError = function (response) {
        console.log('Sorry, but there\'s some problem with your socket or the server is down');
    };

    Wordland.socket = socket.subscribe(request);
});