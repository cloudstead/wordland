WLGame = {

    cells: {},   // map by ID

    start: function () {
        WLStatus.reset();
        Api.get_game_state(Wordland.room, function (data) {

            Wordland.cells = {};

            var tbody = $('#game_tbody');
            for (var x=0; x<data.width; x++) {
                var row = $('<tr id="g_row_'+x+'"></tr>');
                tbody.append(row);
                for (var y=0; y<data.width; y++) {
                    var tile = data.tiles[x][y];
                    tile.x = x;
                    tile.y = y;
                    tile.id = 'cell_'+guid();

                    var cell = $('<td class="gameCell" id="td_'+tile.id+'">'+tile.symbol+'</td>');
                    cell.on('click', WLGame.addToTrayFunc(tile.id));
                    WLGame.cells[tile.id] = tile;

                    row.append(cell);
                }
            }
            Wordland.showScreen('gameRoomContainer');

        }, Wordland.apiError("error getting game state"));
    },

    addToTrayFunc: function (id) {
        return function () { WLTray.add(id); }
    }

};