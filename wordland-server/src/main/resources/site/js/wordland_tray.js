var TrayItem = function (boardId) {
    this.boardId = boardId;
    this.id = 'tray_'+boardId;
    this.tile = WLGame.cells[boardId];
    this.cell = $('<td class="gameCell" id="'+this.id+'">'+this.tile.symbol+'</td>');
    this.cell.on('click', function () {
        WLTray.remove(boardId);
    });
    $('#'+boardId).addClass('usedInTray');
    WLTray.items.push(this);
};

WLTray = {

    items: [],

    contains: function (boardId) {
        for (var i=0; i<WLTray.items.length; i++) {
            if (WLTray.items[i].boardId == boardId) return true;
        }
        return false;
    },

    add: function (boardId) {
        // ensure item is not already in tray
        if (WLTray.contains(boardId)) return;

        var trayItem = new TrayItem(boardId);

        var trayRow = $('#game_tray_tr');
        trayRow.append(trayItem.cell);
        $('.trayButton').css({visibility: 'visible'});
    },

    remove: function (boardId) {
        var found = null;
        var pos = -1;
        for (var i=0; i<WLTray.items.length; i++) {
            if (WLTray.items[i].boardId == boardId) {
                pos = i; found = WLTray.items[i]; break;
            }
        }
        if (found == null) return;
        WLTray.items.remove(pos, pos);
        $('#'+found.id).remove();
        $('#'+found.boardId).removeClass('usedInTray');
    },

    move: function (from, to) {

    },

    clear: function () {
        $('#game_tray_tr').empty();
        WLTray.items = [];
        $('.trayButton').css({visibility: 'hidden'});
    },

    submit: function () {

    }

};