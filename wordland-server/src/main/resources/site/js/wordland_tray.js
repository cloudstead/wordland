/**
 * An item in the tray
 * @param id the ID of the item on the board
 * @constructor
 */
var TrayItem = function (id) {
    this.id = id;
    this.slotId = guid();
    this.tile = WLGame.cells[this.id];
    this.init();
};

TrayItem.prototype.reinit = function () {
    this.div.remove();
    this.cell.remove();
    this.init();
    return this;
};

TrayItem.prototype.init = function () {

    this.div = this.newDiv();
    this.cell = this.newCell();
    this.cell.append(this.div);

    const self = this;
    this.cell.on('click', function () {
        WLTray.remove(self.id);
    });

    $('#td_'+this.id).addClass('usedInTray');
    WLGame.notifyInTray(this.id);
    this.index = WLTray.items.length;
    WLTray.items.push(this);
    return this;
};

TrayItem.prototype.newDiv = function () {
    return $('<div class="trayTile" id="tray_div_'+this.id+'">'+this.tile.symbol+'</div>')
};
TrayItem.prototype.newCell = function () {
    return $('<td class="traySlot" id="tray_slot_'+this.slotId+'"></td>');
};

WLTray = {

    max_items: 30,

    board_kb_target: { x: 0, y: 0 },

    /**
     * an array of TrayItems representing the items in the tray.
     */
    items: [],

    log: function () { console.log(JSON.stringify(WLTray.items)); }, // useful for debugging

    contains: function (id) {
        for (var i=0; i<WLTray.items.length; i++) {
            if (WLTray.items[i].id == id) return true;
        }
        return false;
    },

    indexOf: function (id) {
        for (var i=0; i<WLTray.items.length; i++) {
            if (WLTray.items[i].id == id) return i;
        }
        return -1;
    },

    /**
     * Add a cell from the board to the tray. Highlight the board cell.
     * @param id the DOM ID of the cell on the board
     */
    add: function (id, update_kb) {
        if (WLTray.items.length >= WLTray.max_items) return false;

        // update KB target to new select location
        if (typeof update_kb == 'undefined' || update_kb) {
            WLTray.board_kb_target = { x: WLGame.cells[id].x, y: WLGame.cells[id].y };
        }

        // ensure item is not already in tray
        if (WLTray.contains(id)) return false;

        var trayItem = new TrayItem(id);

        var trayRow = $('#game_tray_tr');
        trayRow.append(trayItem.cell);
        $('.trayButton').css({visibility: 'visible'});

        var rect = trayRow[0].getBoundingClientRect();
        $('#tbounds').html('top:'+rect.top+', bottom:'+rect.bottom+', left:'+rect.left+', right:'+rect.right);
        $('#game_tray_help').remove();
        return true;
    },

    /**
     * Remove a cell from the tray. Un-highlight it on the board.
     * @param id
     */
    remove: function (id) {
        for (var i=0; i<WLTray.items.length; i++) {
            if (WLTray.items[i].id == id) {
                $('#tray_slot_'+WLTray.items[i].slotId).remove(); // remove td from tray row
                $('#td_'+id).removeClass('usedInTray'); // un-highlight tile on board
                WLGame.notifyOutOfTray(id);
                WLTray.items.remove(i, i);   // remove from tiles array
                if (WLTray.items.length == 0) WLTray.clear();
                return true;
            }
        }
        return false;
    },

    redraw: function () {
        var trayRow = $('#game_tray_tr');
        if (typeof trayRow == 'undefined' || trayRow.length == 0) {
            trayRow = $('<tr id="game_tray_tr" class="gameTray"></tr>');
            var trayTbody = $('#game_tray_tbody');
            trayTbody.empty();
            trayTbody.append(trayRow);
        } else {
            trayRow.empty();
        }
        var oldItems = WLTray.items.slice(0);
        WLTray.items = [];
        for (var i=0; i<oldItems.length; i++) {
            WLTray.add(oldItems[i].id, false);
        }
    },

    /**
     * Clear the tray. Un-highlight all board letters.
     */
    clear: function () {
        WLTray.items = [];
        $('.gameCell').removeClass('usedInTray');
        $('#game_tray_tr').empty();
        $('.trayButton').css({visibility: 'hidden'});
    },

    /**
     * Submit the tray.
     */
    submit: function () {
        var tiles = [];
        var word = '';
        for (var i=0; i<WLTray.items.length; i++) {
            var tile = WLTray.items[i].tile;
            tiles.push({x: tile.x, y: tile.y, symbol: tile.symbol });
            word += tile.symbol;
        }
        $('.usedInTray').css({'border-style': 'double'});
        if (tiles.length > 0) Wordland.send({tiles: tiles, word: word, stateChange: 'word_played'});
    },

    selectNearest: function (xOrigin, yOrigin, keyCode) {
        if (WLTray.items.length >= WLTray.max_items) return;
        var sym = String.fromCharCode(keyCode);
        var distances = [];
        var maxSearchDist = 25;
        for (var i=Math.max(0, xOrigin-maxSearchDist); i<Math.min(WLGame.length, xOrigin+maxSearchDist); i++) {
            for (var j=Math.max(0, yOrigin-maxSearchDist); j<Math.min(WLGame.width, yOrigin+maxSearchDist); j++) {
                var deltaX = Math.abs(i - xOrigin);
                var deltaY = Math.abs(j - yOrigin);
                distances.push({
                    tile: WLGame.cellAt(i, j),
                    distance: Math.sqrt((deltaX*deltaX) + (deltaY*deltaY))
                });
            }
        }
        distances.sort(function (a, b) { return a.distance - b.distance; });
        for (var k=0; k<distances.length; k++) {
            if (distances[k].tile.symbol.toUpperCase() == sym.toUpperCase()) {
                if (WLTray.add(distances[k].tile.id, false)) return;
            }
        }
    }

};

$(function () {
    function dragMoveListener (event) {
        $('#e_px').html(event.pageX);
        $('#e_py').html(event.pageY);
        var target = event.target,
        // keep the dragged position in the data-x/data-y attributes
            x = (parseFloat(target.getAttribute('data-x')) || 0) + event.dx,
            y = (parseFloat(target.getAttribute('data-y')) || 0) + event.dy;

        // translate the element
        target.style.zIndex = 5;
        target.style.webkitTransform =
            target.style.transform =
                'translate(' + x + 'px, ' + y + 'px)';

        // update the posiion attributes
        target.setAttribute('data-x', x);
        target.setAttribute('data-y', y);
    }

    function findDropTargetTraySlot(x, y) {
        var trayBounds = $('#game_tray_tr')[0].getBoundingClientRect();
        if (y > trayBounds.bottom + 20) return null; // remove from tray
        var trayLength = trayBounds.right - trayBounds.left;
        var pct = (x - trayBounds.left) / trayLength;
        var index = parseInt(WLTray.items.length * pct);
        index = Math.min(Math.max(0, index), WLTray.items.length);
        return index;
    }

    function findDraggedTraySlot(id) {
        for (var i=0; i<WLTray.items.length; i++) {
            if (id.indexOf(WLTray.items[i].id) != -1) {
                return { index: i, item: WLTray.items[i] };
            }
        }
        return null;
    }

    interact('.trayTile')
        .draggable({
            // enable inertial throwing
            inertia: true,
            // keep the element within the area of it's parent
            restrict: {
                restriction: "#wordland_body",
                endOnly: true,
                elementRect: { top: 0, left: 0, bottom: 1, right: 1 }
            },
            // enable autoScroll
            autoScroll: true,

            // call this function on every dragmove event
            onstart: function (event) {
                $('#'+event.target.id).addClass('dragging');
                WLTray.numItemsAtDrag = WLTray.items.length;
                WLTray.draggingSlot = findDraggedTraySlot(event.target.id);
            },
            onmove: dragMoveListener,
            onend: function (event) {
                $('#'+event.target.id).removeClass('dragging');
                var slot = findDropTargetTraySlot(event.pageX, event.pageY, '.traySlot');
                if (slot != null) {
                    if (WLTray.draggingSlot != null) {
                        if (WLTray.numItemsAtDrag == WLTray.items.length) {
                            WLTray.items.remove(WLTray.draggingSlot.index);
                        }
                        WLTray.items.splice(slot, 0, WLTray.draggingSlot.item);
                    }
                } else {
                    if (WLTray.draggingSlot != null) {
                        WLTray.remove(WLTray.draggingSlot.item.id);
                    }
                }
                WLTray.draggingSlot = null;
                WLTray.numItemsAtDrag = WLTray.items.length;
                WLTray.redraw();
            }
        });

    $(document).unbind('keydown').bind('keydown', function (event) {
        switch (event.keyCode) {
            case 27:         // escape
                WLTray.clear();
                event.preventDefault();
                break;
            case 13:         // enter
                WLTray.submit();
                event.preventDefault();
                break;

            case 8: case 46: // backspace or delete
                if (WLTray.items.length > 0) WLTray.remove(WLTray.items[WLTray.items.length-1].id);
                event.preventDefault();
                break;

            default:
                WLTray.selectNearest(WLTray.board_kb_target.x, WLTray.board_kb_target.y, event.keyCode);
                break;
        }
    })
});