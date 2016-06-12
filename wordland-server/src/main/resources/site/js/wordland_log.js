WLLog = {
    entries: [],
    maxEntries: 20,
    logWordPlayed: function (player, word) {
        const id = guid();
        if (WLLog.entries.length >= WLLog.maxEntries) {
            $('#'+WLLog.entries.shift()).remove();
        }
        WLLog.entries.push(id);
        const playDescription = $('<div id="'+id+'"></div>').addClass(WLGame.playerLogCss(player));
        playDescription.html(word);
        $('#logInsertPoint').after(playDescription);
    }
};