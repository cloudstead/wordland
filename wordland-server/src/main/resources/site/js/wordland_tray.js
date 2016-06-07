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
            WLTray.add(oldItems[i].id);
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
    $('.trayTile').on('click', function (event) {
        $(this).css({zIndex: 5});
    });
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
        if (x < trayBounds.left || x > trayBounds.right) return null;
        if (y < trayBounds.top || y > trayBounds.bottom) return null;
        // todo: determine % between start and end. use that to discern an index in items array, return that slot number
        var trayLength = trayBounds.right - trayBounds.left;
        var pct = (x - trayBounds.left) / trayLength;
        return parseInt(WLTray.items.length * pct);
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
                var slot = findDropTargetTraySlot(event.pageX, event.pageY, '.traySlot');
                if (slot != null) {
                    var draggedSlot = findDraggedTraySlot(event.target.id);
                    if (draggedSlot != null) {
                        WLTray.items.remove(draggedSlot.index);
                        WLTray.items.splice(slot, 0, draggedSlot.item);
                    }
                }
                WLTray.redraw();
            }
        });
});