WLGame = {

    cells: {},   // map by ID
    cellsByCoordinates: {},

    length: 0,
    width: 0,

    start: function () {
        WLStatus.reset();
        Api.get_game_state(Wordland.room, function (data) {

            WLGame.cells = {};
            WLGame.width = data.width;
            WLGame.length = data.length;

            var tbody = $('#game_tbody');
            for (var x=0; x<data.width; x++) {
                var row = $('<tr id="g_row_'+x+'"></tr>');
                tbody.append(row);
                for (var y=0; y<data.length; y++) {
                    var tile = data.tiles[x][y];
                    tile.x = x;
                    tile.y = y;
                    tile.id = 'cell_'+guid();

                    var cell = $('<td class="gameCell" id="td_'+tile.id+'">'+tile.symbol+'</td>');
                    cell.on('click', WLGame.addToTrayFunc(tile.id));
                    WLGame.cells[tile.id] = tile;
                    WLGame.cellsByCoordinates[WLGame.coords(x, y)] = tile;
                    if (tile.owner) cell.addClass(WLGame.playerCss(tile.owner));

                    row.append(cell);
                }
            }
            Wordland.showGameRoom();

        }, Wordland.apiError("error getting game state"));
    },

    coords: function (x, y) { return '' + x + ',' + y; },

    cellAt: function (x, y) { return WLGame.cellsByCoordinates[WLGame.coords(x, y)]; },

    addToTrayFunc: function (id) {
        return function () { WLTray.add(id); }
    },

    cellIsClaimable: function (player, x, y) {
        const targetCell = WLGame.cellAt(x, y);
        if (!targetCell.owner) return true;
        if (targetCell.owner == player) return true;

        // check cell above
        var owner;
        if (x > 0) {
            owner = WLGame.cellAt(x-1, y).owner;
            if (!owner || owner != targetCell.owner) return true;
        }
        if (x < WLGame.width-1) {
            owner = WLGame.cellAt(x+1, y).owner;
            if (!owner || owner != targetCell.owner) return true;
        }
        if (y > 0) {
            owner = WLGame.cellAt(x, y-1).owner;
            if (!owner || owner != targetCell.owner) return true;
        }
        if (y < WLGame.length-1) {
            owner = WLGame.cellAt(x, y+1).owner;
            if (!owner || owner != targetCell.owner) return true;
        }
        return false;
    },

    playerStyleIndex: function (id) { return parseInt(id.substring(id.length-4), 16) % 8; },
    playerCss: function (id) {
        if (id == Wordland.player.id) {
            return 'playerOwnedCell';
        } else {
            return 'playerOwnedCell_' + WLGame.playerStyleIndex(id);
        }
    },
    playerLogCss: function (id) {
        if (id == Wordland.player.id) {
            return 'playerLog';
        } else {
            return 'playerLog_' + WLGame.playerStyleIndex(id);
        }
    },

    notifyInTray: function (id) {
        const boardCell = WLGame.cells[id];
        if (boardCell.owner && boardCell.owner != Wordland.player.id) {
            $('#td_' + boardCell.id).removeClass(WLGame.playerCss(boardCell.owner));
        }
    },

    notifyOutOfTray: function (id) {
        const boardCell = WLGame.cells[id];
        if (boardCell.owner && boardCell.owner != Wordland.player.id) {
            $('#td_' + boardCell.id).addClass(WLGame.playerCss(boardCell.owner));
        }
    },

    updateTiles: function (player, tiles) {
        var isLocalPlayer = player == Wordland.player.id;
        var word = '';
        for (var i=0; i<tiles.length; i++) {
            const tile = tiles[i];
            word += tile.symbol;
            if (WLGame.cellIsClaimable(player, tile.x, tile.y)) {
                const boardCell = WLGame.cellAt(tile.x, tile.y);
                var boardCellElement = $('#td_' + boardCell.id);
                if (boardCell.owner) {
                    if (boardCell.owner != player) {
                        boardCellElement.removeClass(WLGame.playerCss(boardCell.owner));
                    }
                }
                boardCell.owner = player;
                boardCellElement.addClass(WLGame.playerCss(player));
            }
        }
        if (isLocalPlayer) WLTray.clear();
        return word;
    },

    applyStateChange: function (stateUpdate) {
        switch (stateUpdate.stateChange) {
            case 'word_played':
                const player = stateUpdate.object.id;
                const word = WLGame.updateTiles(player, stateUpdate.object.tiles);
                WLLog.logWordPlayed(player, word);
                break;
            default:
                console.log('applyStateChange: unsupported update type: '+stateUpdate.stateChange);
        }
    }

};