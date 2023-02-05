var ws;
var ctx;
var monitor = null;
var id = "";
var ys = {};
var host;
var port;

function E(id) {
	return document.getElementById(id);
}

function _focus() {
	let c = E('canvas');
	c.focus();	
}

window.onload = function() {
	host = window.location.hostname;
	port = window.location.port;
	if(port == null
	|| port == "") port = "8080";
	ws = new WebSocket("ws://" + host + ":" + port + "/monitor/ws");
	ws["clients"] = clients;
	ws["screen"] = screen;
	ws["mouse"] = mouse;
	ws["redraw"] = redraw;
	ws.onopen = function() {
		ws.send("browser");
	};
	ws.onmessage = function(e) {
		let o = e.data.split(" ");
		ws[o[0]](o);
	}

	let c = E('canvas');
	c.addEventListener('keydown', keydown);
	c.addEventListener('keyup', keyup);
	c.addEventListener('mousedown', mousedown);
	c.addEventListener('mousemove', mousemove);
	c.addEventListener('mouseup', mouseup);
	c.addEventListener('contextmenu', function(event){
		event.preventDefault();
		event.stopPropagation();
	});
	c = E('body');
	c.addEventListener('wheel', mousewheel);
	c.addEventListener('contextmenu', function(event){
		event.preventDefault();
		event.stopPropagation();
	});
	c = E('in');
	c.addEventListener('keydown', function (event) {
		if(event.keyCode == 13) {
			send_in();
			event.preventDefault();
			event.stopPropagation();
			return false;
		}
		return true;
	});
	sel_zoom();
	window.onresize = function() {
		sel_zoom();
	};
};

var zoom = 0;
function sel_zoom() {
	_zoom();
	_screen();
	_redraw();
	_mouse();
}
function _zoom() {
	let c = E('zoom');
	zoom = c.value;
	if(c.value == 0) {
		let c = E('body');
		let w = (c.clientWidth - 2) / screen_w;
		let h = (c.clientHeight - 2) / screen_h;
		if(w > h) w = h;
		zoom = parseInt(w * 100);
	}
}

function send_in() {
	if(monitor != null) {
		let c = E('in');
		if(c.value.length > 0) {
			ws.send("string " + c.value);
			c.value = "";
		}
	}
} 

function keydown(ev) {
	let k = keys(ev);
	if(monitor != null && ( ! ev.repeat)) {
		ws.send("keydown " + ev.keyCode + k);
		if(_ctrl > 1) ctrl(0);
		if(_shift > 1) shift(0);
		if(_alt > 1) alt(0);
	}
	if(ev.keyCode == 17) ctrl(1);
	else if(ev.keyCode == 16) shift(1);
	else if(ev.keyCode == 18) alt(1);
	ev.preventDefault();
	ev.stopPropagation();
	return false;
} 
function keyup(ev) {
	let k = keys(ev);
	if(monitor != null) {
		ws.send("keyup " + ev.keyCode + k);
	}
	if(ev.keyCode == 17) ctrl(0);
	else if(ev.keyCode == 16) shift(0);
	else if(ev.keyCode == 18) alt(0);
	ev.preventDefault();
	ev.stopPropagation();
	return false;
} 

var _mx = -1, _my = -1;
function mousewheel(ev) {
	let k = keys(ev);
	if(monitor != null) {
		ws.send("mousewheel " + _mx + " " + _my + " " + ev.deltaY + k);
	}
	ev.preventDefault();
	ev.stopPropagation();
	return false;
} 
function mousemove(ev) {
	let k = keys(ev);
	if(monitor != null && ev.ctrlKey) {
		_mx = parseInt(ev.layerX * 100 / zoom);
		_my = parseInt(ev.layerY * 100 / zoom);
		ws.send("mousemove " + _mx + " " + _my + k);
	}
	ev.preventDefault();
	ev.stopPropagation();
	return false;
} 
function mousedown(ev) {
	E('body').focus();
	let k = keys(ev);
	if(monitor != null) {
		_mx = parseInt(ev.layerX * 100 / zoom);
		_my = parseInt(ev.layerY * 100 / zoom);
		ws.send("mousedown " + _mx + " " + _my + " " + ev.button + k);
	}
	ev.preventDefault();
	ev.stopPropagation();
	return false;
} 
function mouseup(ev) {
	let k = keys(ev);
	if(monitor != null) {
		_mx = parseInt(ev.layerX * 100 / zoom);
		_my = parseInt(ev.layerY * 100 / zoom);
		ws.send("mouseup " + _mx + " " + _my + " " + ev.button + k);
	}
	ev.preventDefault();
	ev.stopPropagation();
	return false;
} 

var mouse_x = 0, mouse_y = 0;
function mouse(o) {
	mouse_x = parseInt(o[1]);
	mouse_y = parseInt(o[2]);
	_mouse();
}
function _mouse() {
	let x = parseInt(mouse_x * zoom / 100);
	let y = parseInt(mouse_y * zoom / 100);
	let c = E('cursor');
	c.style.left = (x - 5) + "px";
	c.style.top = (y - 5) + "px";
}

var redraw_time = null;
var redraw_src = null;
function drawimage() {
	const img = new Image();
	img.onload = (ev) => {
		let w = ev.target.width;
		let h = ev.target.height;
		let ww = w * zoom / 100;
		let hh = h * zoom / 100;
		ctx.drawImage(ev.target, 0, 0, w, h, 0, 0, ww, hh);
		redraw_time = null;
	};
	img.src = redraw_src;
}
function redraw() {
	redraw_src = "http://" + host + ":" + port + "/monitor/api?id=" + id;
	_redraw()
}	
function _redraw() {
	if(redraw_src == null) return;
	if(redraw_time == null) {
		redraw_time = setTimeout(drawimage, 1);
	}
}

var screen_w = 800, screen_h = 640;
function screen(o) {
	screen_w = parseInt(o[1]);
	screen_h = parseInt(o[2]);
	_zoom();
	_screen();
	_redraw();
	_mouse();
}
function _screen() {
	let c = E('canvas');
	let w = screen_w * zoom / 100;
	let h = screen_h * zoom / 100;
	c.width = w;
	c.height = h;
	ctx = c.getContext("2d");
	c = E('keys');
	c.style.width = w + "px";
}

var clients_list = "";
var clients_eq = false;
function clients(o) {
	if(o[1] == "[") {
		clients_list = "";
		clients_eq = false;
		return;
	}
	if(o[1] == "]") {
		E("list").innerHTML = clients_list;
		if( ! clients_eq) {
			document.title = "Disconnect";
		}
		return;
	}
	if(o.length > 2) o[1] += " " + o[2];
	clients_list += "<div class=s onclick='select(this)'>" + o[1] + "</div>";
	if(o[1] == monitor) clients_eq = true;
}

function select(e) {
	monitor = e.innerHTML;
	document.title = monitor;
	id = e.innerHTML.split(":")[0];
	ws.send("browser " + id);
	h = E('cursor');
	h.style.display = "inline-block";
	hidelist(1);
	if(event != undefined) {
		event.preventDefault();
		event.stopPropagation();
	}	
}

var hidelist_time = null;
function showlist() {
	if(hidelist_time != null) {
		clearTimeout(hidelist_time);
		hidelist_time = null;
	}
	E("lists").className = "s";
	E("show").style.display = "none";
	E("hide").style.display = "";
}
function hidelist(htime) {
	if(hidelist_time != null) {
		clearTimeout(hidelist_time);
		hidelist_time = null;
	}
	hidelist_time = setTimeout(function() {
		hidelist_time = null;
		E("lists").className = "";
		E("show").style.display = "";
		E("hide").style.display = "none";
	}, htime);
}

function keys(ev) {
	let k = "";
	if(_alt > 0 || ev.altKey) k += " alt";
	if(_ctrl > 0 || ev.ctrlKey) k += " ctrl";
	if(_shift > 0 || ev.shiftKey) k += " shift";
	return k;
}

var _alt=0;
var _shift=0;
var _ctrl=0;
function alt(m) {
	if(m != undefined) _alt = m;
	else _alt = (_alt==0)? 1 : 0;
	E("alt").className = (_alt==2)? "l" : (_alt==1)? "s" : "";
	if(event != undefined) {
		event.preventDefault();
		event.stopPropagation();
	}	
}
function shift(m) {
	if(m != undefined) _shift = m;
	else _shift = (_shift==0)? 1 : 0;
	E("shift").className = (_shift==2)? "l" : (_shift==1)? "s" : "";	
	if(event != undefined) {
		event.preventDefault();
		event.stopPropagation();
	}	
}
function ctrl(m) {
	if(m != undefined) _ctrl = m;
	else _ctrl = (_ctrl==0)? 1 : 0;
	E("ctrl").className = (_ctrl==2)? "l" : (_ctrl==1)? "s" : "";
	if(event != undefined) {
		event.preventDefault();
		event.stopPropagation();
	}	
}

function load() {
	ws.send("browser");
	if(event != undefined) {
		event.preventDefault();
		event.stopPropagation();
	}	
}
