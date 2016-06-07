WLGame = {

    cells: {},   // map by ID
    cellsByCoordinates: {},

    length: 0,
    width: 0,

    start: function () {
        WLStatus.reset();
        Api.get_game_state(Wordland.room, function (data) {

            Wordland.cells = {};
            WLGame.width = data.width;
            WLGame.length = data.length;

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
                    WLGame.cellsByCoordinates[''+x+','+y] = tile;

                    row.append(cell);
                }
            }
            Wordland.showScreen('gameRoomContainer');

        }, Wordland.apiError("error getting game state"));
    },

    cellAt: function (x, y) { return WLGame.cellsByCoordinates[''+x+','+y]; },

    addToTrayFunc: function (id) {
        return function () { WLTray.add(id); }
    }

};