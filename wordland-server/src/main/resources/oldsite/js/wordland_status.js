WLStatus = {
    reset: function () {
        $('#current_player_name').html(Wordland.player.name);
        $('#current_player_score').html('0');
        $('#current_room_name').html(Wordland.room);
    }
};