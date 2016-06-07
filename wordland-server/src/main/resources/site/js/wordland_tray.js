/**
 * An item in the tray
 * @param id the ID of the item on the board
 * @constructor
 */
var TrayItem = function (id) {
    this.id = id;
    this.slotId = guid();
    this.tile = WLGame.cells[id];

    this.div = $('<div class="trayTile" id="tray_div_'+id+'">'+this.tile.symbol+'</div>');

    this.cell = $('<td class="traySlot" id="tray_slot_'+this.slotId+'"></td>');
    this.cell.append(this.div);

    this.cell.on('click', function () {
        WLTray.remove(id);
    });

    $('#td_'+id).addClass('usedInTray');
    WLTray.items.push(this);
};

WLTray = {

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
    add: function (id) {
        // ensure item is not already in tray
        if (WLTray.contains(id)) return;

        var trayItem = new TrayItem(id);

        var trayRow = $('#game_tray_tr');
        trayRow.append(trayItem.cell);
        $('.trayButton').css({visibility: 'visible'});

        var rect = trayRow[0].getBoundingClientRect();
        $('#tbounds').html('top:'+rect.top+', bottom:'+rect.bottom+', left:'+rect.left+', right:'+rect.right);
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
                WLTray.items.remove(i, i);   // remove from tiles array
                if (WLTray.items.length == 0) WLTray.clear();
                return;
            }
        }
    },

    /**
     * Move a cell in the tray from one position to another
     * @param from the cell to move
     * @param to the index of the cell, in the current list, that it should appear *before*. if it would appear before itself, it remains in the same position.
     */
    move: function (from, to) {
        var fromItem = WLTray.items[from];
        var toItem = WLTray.items[to];
        WLTray.items[to] = fromItem;
        WLTray.items[from] = toItem;

        // redraw tray
        WLTray.redraw();
    },

    redraw: function () {
        var trayRow = $('#game_tray_tr');
        trayRow.empty();
        for (var i=0; i<WLTray.items.length; i++) {
            trayRow.append(WLTray.items[i].cell.clone());
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
        target.style.webkitTransform =
            target.style.transform =
                'translate(' + x + 'px, ' + y + 'px)';

        // update the posiion attributes
        target.setAttribute('data-x', x);
        target.setAttribute('data-y', y);
    }

    function findTraySlot(x, y) {
        var trayBounds = $('#game_tray_tr')[0].getBoundingClientRect();
        if (x < trayBounds.left || x > trayBounds.right) return null;
        if (y < trayBounds.top || y > trayBounds.bottom) return null;
        // todo: determine % between start and end. use that to discern an index in items array, return that slot number
        var trayLength = trayBounds.right - trayBounds.left;
        var pct = (x - trayBounds.left) / trayLength;
        var pos = parseInt(WLTray.items.length * pct);
        console.log('pos='+pos);
    }

    interact('.trayTile')
        .draggable({
            // enable inertial throwing
            inertia: true,
            // keep the element within the area of it's parent
            restrict: {
                restriction: ".gameTray",
                endOnly: true,
                elementRect: { top: 0, left: 0, bottom: 1, right: 1 }
            },
            // enable autoScroll
            autoScroll: true,

            // call this function on every dragmove event
            onmove: dragMoveListener,
            // call this function on every dragend event
            onend: function (event) {
                var slot = findTraySlot(event.pageX, event.pageY, '.traySlot');
                if (slot != null) {
                    // todo: move event.target div into slot, slide everything else down one
                    console.log("found slot: "+JSON.stringify(slot));
                }
            }
        });
});