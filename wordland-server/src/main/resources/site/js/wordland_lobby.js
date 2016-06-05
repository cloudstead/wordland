WordlandLobby = {

    refreshRooms: function (rooms) {
        var roomSelect = $('#room_to_join');
        roomSelect.empty();
        for (var i=0; i<rooms.length; i++) {
            roomSelect.append($('<option>'+rooms[i].name+'</option>'));
        }
    }

};