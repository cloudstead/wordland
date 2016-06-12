const WORDLAND_CLIENT_ID = 'wordland_client_id';
const EVENTS_PORT = 9099;

Wordland = {

    room: null,
    player: null,

    clientId: function () {
        var id = localStorage.getItem(WORDLAND_CLIENT_ID);
        if (typeof id == 'undefined' || id == null) {
            id = guid();
            localStorage.setItem(WORDLAND_CLIENT_ID, id);
        }
        return id;
    },

    eventsApi: function () {
        var host = document.location.host;
        if (host.indexOf(':') != -1) {
            host = host.substring(0, host.indexOf(':'));
        }
        return document.location.protocol + "//" + host + ':' + EVENTS_PORT;
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

    showLobby: function () {
        $('#gameRoomContainer').css({visibility: 'hidden'});
        $('#lobbyContainer').css({visibility: 'visible'});
    },

    showGameRoom: function () {
        $('#lobbyContainer').css({visibility: 'hidden'});
        $('#gameRoomContainer').css({visibility: 'visible'});
    },

    join: function (room, player_info) {
        if (Wordland.player != null) {
            console.log('game already in progress');

        } else {
            player_info.clientId = Wordland.clientId();
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
            Api.quit_game(Wordland.room, Wordland.player.id, Wordland.player.apiKey, Wordland.clientId(), function (data) {
                Wordland.room = null;
                Wordland.player = null;
                Wordland.showLobby();

            }, Wordland.apiError("error quitting game"));
        }
    },

    socket: null,

    toMessage: function (message) {
        message.id = Wordland.player.id;
        message.apiKey = Wordland.player.apiKey;
        message.clientId = Wordland.clientId();
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
    $('#join_button').on('click', function (e) {
        Wordland.join( $('#room_to_join').find(':selected').text(), {name: $('#player_name').val()} );
    });

    const name = $('#player_name');
    name.on('keyup', function (e) {
        const playerName = $('#player_name');
        if (playerName.is(":focus") && $('#lobbyContainer').css('visibility') != 'visible') {
            playerName.val('');
            return;
        }
        switch (e.keyCode) {
            case 13:
                Wordland.join($('#room_to_join').find(':selected').text(), {name: playerName.val()});
                break;
            case 27:
                playerName.val('');
                break;
        }
    });
    name.focus();
    if (Wordland.player && Wordland.player.name) name.val(Wordland.player.name);

    $('#lobbyControlsContainer').centerTop();

    Api.list_rooms(function (data) {
        if (is_array(data)) {
            WordlandLobby.refreshRooms(data);
            Wordland.showLobby();
        }
    }, Wordland.apiError("error listing rooms"));

    var logged = false;
    var socket = atmosphere;
    var transport = 'websocket';

    // We are now ready to cut the request
    var request = { url: Wordland.eventsApi()+'/events/'+Wordland.clientId(),
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
            request.fallbackTransport = 'long-polling';
        }
        $('.lobbyControl').attr('disabled', 'disabled');
        console.log('Default transport is WebSocket, fallback is ' + request.fallbackTransport);
    };

    request.onMessage = function (response) {
        console.log('onMessage: '+response.responseBody);
        var obj = JSON.parse(response.responseBody);
        //try {
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
        //} catch (e) {
        //    console.log('This doesn\'t look like valid JSON: ', response.responseBody);
        //}
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