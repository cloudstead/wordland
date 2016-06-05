String.prototype.trim = String.prototype.trim || function trim() { return this.replace(/^\s\s*/, '').replace(/\s\s*$/, ''); };

String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

// adapted from https://stackoverflow.com/a/13538245/1251543
String.prototype.escape = function() {
    var tagsToReplace = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '\'': '&apos;'
    };
    return this.replace(/[&<>]/g, function(tag) {
        return tagsToReplace[tag] || tag;
    });
};

String.prototype.unescape = function() {
    var tags = [
        {'&amp;': '&'},
        {'&lt;': '<'},
        {'&gt;': '>'},
        {'&apos;': '\''}
    ];
    var s = this;
    for (var i=0; i<tags.length; i++) s = s.replace(tags[i][0], tags[i][1]);
    return s;
};

if (!String.prototype.startsWith) {
    String.prototype.startsWith = function(searchString, position){
        position = position || 0;
        return this.substr(position, searchString.length) === searchString;
    };
}

// From: http://werxltd.com/wp/2010/05/13/javascript-implementation-of-javas-string-hashcode-method/
String.prototype.hashCode = function() {
    var hash = 0, i, chr, len;
    if (this.length === 0) return hash;
    for (i = 0, len = this.length; i < len; i++) {
        chr   = this.charCodeAt(i);
        hash  = ((hash << 5) - hash) + chr;
        hash |= 0; // Convert to 32bit integer
    }
    return hash;
};

function is_array (x) {
    return Object.prototype.toString.call( x ) === '[object Array]'
}

function is_string (x) {
    return Object.prototype.toString.call(x) == '[object String]';
}

// from https://stackoverflow.com/a/2901298/1251543
function number_with_commas (x) {
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// From: https://stackoverflow.com/a/901144/1251543
function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

// From: http://stackoverflow.com/a/210733
jQuery.fn.center = function () {
    this.css("position","absolute");
    this.css("top", Math.max(0, (($(window).height() - $(this).outerHeight()) / 2) +
            $(window).scrollTop()) + "px");
    this.css("left", Math.max(0, (($(window).width() - $(this).outerWidth()) / 2) +
            $(window).scrollLeft()) + "px");
    return this;
};

jQuery.fn.centerTop = function (offset) {
    if (typeof offset == "undefined") offset = 20;
    keep_centerTop(offset)(this);
    this.resize(keep_centerTop(offset));
    return this;
};

function keep_centerTop (offset) {
    return function (jqElement) {
        jqElement.css("position", "absolute");
        jqElement.css("top", offset + "px");
        jqElement.css("left", Math.max(0, (($(window).width() - $(jqElement).outerWidth()) / 2) + $(window).scrollLeft()) + "px");
    };
}

jQuery.fn.centerBottom = function (offset) {
    if (typeof offset == "undefined") offset = 20;
    keep_centerBottom(offset)(this);
    this.resize(keep_centerBottom(offset));
    return this;
};

function keep_centerBottom (offset) {
    return function (jqElement) {
        jqElement.css("position", "absolute");
        jqElement.css("top", ($(window).height() - offset) + "px");
        jqElement.css("left", Math.max(0, (($(window).width() - $(jqElement).outerWidth()) / 2) + $(window).scrollLeft()) + "px");
    };
}

// from: http://stackoverflow.com/a/18197341
function download(filename, contentType, base64data) {
    var element = document.createElement('a');
    element.setAttribute('href', 'data:'+contentType+';base64,' + base64data);
    element.setAttribute('download', filename);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
}

function rightSizeIframe () {
    if (window.parent != window) {
        var eof = $('#EOF');
        var top = 600;
        if (typeof eof !== "undefined") {
            top = eof.offset().top;
            if (top < 600) top = 600;
        }
        $('#mainIframe', window.parent.document).height(top);
    } else {
        $('body').css({overflow: 'scroll', 'padding-left': '50px', 'padding-right': '50px'});
    }
}

$(function () {
    rightSizeIframe();
});