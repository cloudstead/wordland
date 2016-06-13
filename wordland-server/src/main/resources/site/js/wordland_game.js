WLGame = {

    cells: {},   // map by ID
    cellsByCoordinates: {},

    length: 0,
    width: 0,

    start: function () {
        WLStatus.reset();
        Api.get_room_settings(Wordland.room, function(data) {
            WLGame.roomSettings = data;

            Api.get_game_state(Wordland.room, function (data) {

                WLGame.cells = {};
                WLGame.width = data.width;
                WLGame.length = data.length;
                var x, y, tile;

                var tbody = $('#game_tbody');
                for (x = 0; x < data.width; x++) {
                    var row = $('<tr id="g_row_' + x + '"></tr>');
                    tbody.append(row);
                    for (y = 0; y < data.length; y++) {
                        tile = data.tiles[x][y];
                        tile.x = x;
                        tile.y = y;
                        tile.id = 'cell_' + guid();

                        var cell = $('<td class="gameCell" id="td_' + tile.id + '">' + tile.symbol + '</td>');
                        cell.on('click', WLGame.addToTrayFunc(tile.id));
                        tile.element = cell;
                        WLGame.cells[tile.id] = tile;
                        WLGame.cellsByCoordinates[WLGame.coords(x, y)] = tile;
                        if (tile.owner) cell.addClass(WLGame.playerCellCss(tile.owner));

                        row.append(cell);
                    }
                }
                for (x = 0; x < data.width; x++) {
                    for (y = 0; y < data.length; y++) {
                        tile = data.tiles[x][y];
                        if (tile.owner && WLGame.cellProtector(x, y) == tile.owner) {
                            tile.element.addClass(WLGame.playerCellProtectedCss(tile.owner));
                        }
                    }
                }
                Wordland.showGameRoom();

            }, Wordland.apiError("error getting game state"));
        }, Wordland.apiError("error getting room settings"));
    },

    coords: function (x, y) { return '' + x + ',' + y; },

    cellAt: function (x, y) { return WLGame.cellsByCoordinates[WLGame.coords(x, y)]; },

    addToTrayFunc: function (id) {
        return function () { WLTray.add(id); }
    },

    cellProtector: function (x, y) {
        const targetCell = WLGame.cellAt(x, y);
        if (!targetCell.owner) return null;

        var owner;
        if (x > 0) {
            owner = WLGame.cellAt(x-1, y).owner;
            if (!owner || owner != targetCell.owner) return null;
        }
        if (x < WLGame.width-1) {
            owner = WLGame.cellAt(x+1, y).owner;
            if (!owner || owner != targetCell.owner) return null;
        }
        if (y > 0) {
            owner = WLGame.cellAt(x, y-1).owner;
            if (!owner || owner != targetCell.owner) return null;
        }
        if (y < WLGame.length-1) {
            owner = WLGame.cellAt(x, y+1).owner;
            if (!owner || owner != targetCell.owner) return null;
        }
        return targetCell.owner;
    },

    playerStyleIndex: function (id) { return parseInt(id.substring(id.length-4), 16) % 8; },
    playerCellCss: function (id) {
        if (id == Wordland.player.id) {
            return 'playerCell';
        } else {
            return 'playerCell_' + WLGame.playerStyleIndex(id);
        }
    },
    playerCellProtectedCss: function (id) {
        //if (id == Wordland.player.id) {
            return 'playerProtectedCell';
        //} else {
        //    return 'playerProtectedCell_' + WLGame.playerStyleIndex(id);
        //}
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
            boardCell.element.removeClass(WLGame.playerCellCss(boardCell.owner));
        }
    },

    notifyOutOfTray: function (id) {
        const boardCell = WLGame.cells[id];
        if (boardCell.owner && boardCell.owner != Wordland.player.id) {
            boardCell.element.addClass(WLGame.playerCellCss(boardCell.owner));
        }
    },

    unprotectCellsAround: function (x, y) {
        const targetCell = WLGame.cellAt(x, y);
        var owner, cell;
        if (x > 0) {
            cell = WLGame.cellAt(x-1, y);
            owner = cell.owner;
            if (owner && owner != targetCell.owner) {
                cell.element.removeClass(WLGame.playerCellProtectedCss(owner));
                cell.element.addClass(WLGame.playerCellCss(owner));
            }
        }
        if (x < WLGame.width-1) {
            cell = WLGame.cellAt(x+1, y);
            owner = cell.owner;
            if (owner && owner != targetCell.owner) {
                cell.element.removeClass(WLGame.playerCellProtectedCss(owner));
                cell.element.addClass(WLGame.playerCellCss(owner));
            }
        }
        if (y > 0) {
            cell = WLGame.cellAt(x, y-1);
            owner = cell.owner;
            if (owner && owner != targetCell.owner) {
                cell.element.removeClass(WLGame.playerCellProtectedCss(owner));
                cell.element.addClass(WLGame.playerCellCss(owner));
            }
        }
        if (y < WLGame.length-1) {
            cell = WLGame.cellAt(x, y+1);
            owner = cell.owner;
            if (owner && owner != targetCell.owner) {
                cell.element.removeClass(WLGame.playerCellProtectedCss(owner));
                cell.element.addClass(WLGame.playerCellCss(owner));
            }
        }
        return false;
    },

    checkCellProtection: function (player, x, y) {
        var protector = WLGame.cellProtector(x, y);
        var boardCell = WLGame.cellAt(x, y);
        if (protector == player) {
            boardCell.element.addClass(WLGame.playerCellProtectedCss(player));
        }
    },

    updateTiles: function (player, tiles) {
        var isLocalPlayer = player == Wordland.player.id;
        if (isLocalPlayer) WLTray.clear();

        var word = '';
        var i, tile, protector, boardCell;

        // check for tiles that are protected by other players, we cannot claim them
        // also accumulate the word
        for (i=0; i<tiles.length; i++) {
            tile = tiles[i];
            word += tile.symbol;
            protector = WLGame.cellProtector(tile.x, tile.y);
            if (protector != null && protector != player) {
                tiles[i].protected = true;
            }
        }
        // claim tiles that we can
        for (i=0; i<tiles.length; i++) {
            tile = tiles[i];
            if (tile.protected) continue; // protected by another player
            boardCell = WLGame.cellAt(tile.x, tile.y);
            if (boardCell.owner) {
                if (boardCell.owner != player) {
                    boardCell.element.removeClass(WLGame.playerCellCss(boardCell.owner));
                }
            }
            boardCell.owner = player;
            boardCell.element.addClass(WLGame.playerCellCss(player));
            WLGame.unprotectCellsAround(tile.x, tile.y)
        }

        // check for cells that are now protected by the current owner
        for (i=0; i<tiles.length; i++) {
            tile = tiles[i];
            if (tile.protected) continue; // protected by another player
            WLGame.checkCellProtection(player, tile.x, tile.y);
            if (tile.x > 0) WLGame.checkCellProtection(player, tile.x-1, tile.y);
            if (tile.x < WLGame.width-1) WLGame.checkCellProtection(player, tile.x+1, tile.y);
            if (tile.y > 0) WLGame.checkCellProtection(player, tile.x, tile.y-1);
            if (tile.y < WLGame.length-1) WLGame.checkCellProtection(player, tile.x, tile.y+1);
        }
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