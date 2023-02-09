var ctx;
var monitor = null;
var monitor_id = null;
var id = "";
var ys = {};
var host;
var port;

function E(id) {
	return document.getElementById(id);
}

function bodyfocus() {
	let c = E('body');
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
	ws["sysmon"] = sysmon;
	ws["cpu"] = sysinfo;
	ws["mem"] = sysinfo;
	ws["drv"] = sysinfo;
	ws.onopen = function() {
		ws.send("browser");
	};
	ws.onmessage = function(e) {
		let o = e.data.split(" ");
		ws[o[0]](o);
	}

	let c = E('body');
	c.addEventListener('keydown', keydown);
	c.addEventListener('keyup', keyup);
	c.addEventListener('dblclick', dblclick);
	c.addEventListener('mousedown', mousedown);
	c.addEventListener('mousemove', mousemove);
	c.addEventListener('mouseup', mouseup);
	c.addEventListener('wheel', mousewheel);
	c.addEventListener('contextmenu', function(event){
		event.preventDefault();
		event.stopPropagation();
	});
	c = E('keys');
	c.addEventListener('contextmenu', function(event){
		event.preventDefault();
		event.stopPropagation();
	});
//	c = E('body');
//	c.addEventListener('contextmenu', function(event){
//		event.preventDefault();
//		event.stopPropagation();
//	});
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

function keydown(event) {
	console.log("keydown " + event.keyCode);
	if( ! event.keydown) {
		let k = keys(event);
		if(monitor != null) {
			ws.send("keydown " + event.keyCode + k);
			if(_ctrl > 1) ctrl(0);
			if(_shift > 1) shift(0);
			if(_alt > 1) alt(0);
		}
		if(event.keyCode == 17) ctrl(1);
		else if(event.keyCode == 16) shift(1);
		else if(event.keyCode == 18) alt(1);
	}
	event.preventDefault();
	event.stopPropagation();
	return false;
} 
function keyup(event) {
	console.log("keyup " + event.keyCode);
	let k = keys(event);
	if(monitor != null) {
		ws.send("keyup " + event.keyCode + k);
	}
	if(event.keyCode == 17) ctrl(0);
	else if(event.keyCode == 16) shift(0);
	else if(event.keyCode == 18) alt(0);
	event.preventDefault();
	event.stopPropagation();
	return false;
} 

var mouseevent_x = -1;
var mouseevent_y = -1;
var mouseevent_b = {'0':0, '1':0, '2':0};
function mouseevent(event) {
	mouseevent_x = parseInt(event.layerX * 100 / zoom);
	mouseevent_y = parseInt(event.layerY * 100 / zoom);
	return mouseevent_x + " " + mouseevent_y;
} 
function mousewheel(event) {
	if(event.target.id == "alt") return true;
	if(event.target.id == "shift") return true;
	if(event.target.id == "ctrl") return true;
	let k = keys(event);
	if(monitor != null) {
		ws.send("mousewheel "
			+ mouseevent_x + " " + mouseevent_y
			+ " " + event.deltaY + k);
	}
	event.preventDefault();
	event.stopPropagation();
	return false;
} 
function mousemove(event) {
	if(event.target.id == "alt") return true;
	if(event.target.id == "shift") return true;
	if(event.target.id == "ctrl") return true;
	let k = keys(event);
	if(monitor != null) {
		if(mouseevent_b['0'] > 0
		|| mouseevent_b['1'] > 0
		|| mouseevent_b['2'] > 0
		|| event.ctrlKey) {
			ws.send("mousemove " + mouseevent(event) + k);
		}
	}
	event.preventDefault();
	event.stopPropagation();
	return false;
} 
function mousedown(event) {
	if(event.target.id == "alt") return true;
	if(event.target.id == "shift") return true;
	if(event.target.id == "ctrl") return true;
	console.log("mousedown " + event.button);
	E('body').focus();
	let k = keys(event);
	if(monitor != null) {
		mouseevent_b[event.button] = 1;
		ws.send("mousedown " + mouseevent(event) + " " + event.button + k);
	}
	event.preventDefault();
	event.stopPropagation();
	return false;
}
function mouseup(event) {
	if(event.target.id == "alt") return true;
	if(event.target.id == "shift") return true;
	if(event.target.id == "ctrl") return true;
	console.log("mouseup " + event.button);
	let k = keys(event);
	if(monitor != null) {
		mouseevent_b[event.button] = 0;
		ws.send("mouseup "
			+ mouseevent_x + " " + mouseevent_y
			+ " " + event.button + k);
	}
	event.preventDefault();
	event.stopPropagation();
	return false;
}
function dblclick(event) {
	if(event.target.id == "alt") return true;
	if(event.target.id == "shift") return true;
	if(event.target.id == "ctrl") return true;
	console.log("dblclick " + event.button);
	let k = keys(event);
	if(monitor != null) {
		mouseevent_b[event.button] = 0;
		ws.send("dblclick " + mouseevent(event) + " " + event.button + k);
	}
	event.preventDefault();
	event.stopPropagation();
	return false;
}

var mouse_x = 0;
var mouse_y = 0;
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
function redraw_image() {
	const img = new Image();
	img.onload = (event) => {
		let w = event.target.width;
		let h = event.target.height;
		let ww = parseInt(w * zoom / 100);
		let hh = parseInt(h * zoom / 100);
		ctx.drawImage(event.target, 0, 0, w, h, 0, 0, ww, hh);
		redraw_time = null;
	};
	img.src = redraw_src;
}
function redraw() {
	redraw_src = "http://" + host + ":" + port + "/monitor/api?id=" + monitor_id;
	_redraw()
}	
function _redraw() {
	if(redraw_src == null) return;
	if(redraw_time == null) {
		redraw_time = setTimeout(redraw_image, 1);
	}
}

var screen_w = 800;
var screen_h = 640;
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
	let w = parseInt(screen_w * zoom / 100);
	let h = parseInt(screen_h * zoom / 100);
	c.width = w;
	c.height = h;
	ctx = c.getContext("2d");
	//c = E('keys');
	//c.style.width = w + "px";
	c = E('body');
	c.width = w;
	c.height = h;
	//c = E('info');
	//c.style.width = w + "px";
}

var clients_list = "";
var clients_eq = false;
var clients_no = 0;
function clients(o) {
	if(o[1] == "[") {
		clients_list = "";
		clients_eq = false;
		clients_no = 0;
		return;
	}
	if(o[1] == "]") {
		E("list").innerHTML = clients_list;
		if( ! clients_eq) {
			document.title = "Disconnect";
			monitor = null;
			monitor_id = null;
			E('tool').className = "";
			E('body').className = "";
			E('sysmon').style.color = null;
		}
		return;
	}
	clients_no++;
	if(o.length > 2) o[1] += " " + o[2];
	clients_list += "<div class=s onclick='select(this)' id='h" + clients_no + "'";
	let id = o[1].split(":")[0];
	if(id == monitor_id) {
		monitor = o[1];
		document.title = monitor;
		clients_eq = true;
		clients_list += " class='s'><img src='monitor.png' id='ih" + clients_no + "'>";
	} else {
		clients_list += "><img src='none.png' id='ih" + clients_no + "'>";
	}
	clients_list += "<span id='nh" + clients_no + "'>" + o[1] + "</span></div>";
}

function select(e) {
	let h = E('n' + e.id);
	let id = h.innerHTML.split(":")[0];
	if(monitor_id == id) {
		h = E('i' + e.id);
		h.src = "none.png";
		h.className = "";
		monitor = null;
		monitor_id = null;
		clients_eq = false;
		document.title = "Disconnect";
		ws.send("view");
		E('tool').className = "";
		E('body').className = "";
		E('sysmon').style.color = null;
		return;
	}
	for(let i=1; i<=clients_no; i++) {
		h = E('ih' + i);
		if(h.src != "none.png") {
			h.src = "none.png";
			h.className = "";
		}
	}
	h = E('i' + e.id);
	h.src = "monitor.png";
	h.className = "s";
	monitor_id = id;
	monitor = E('n' + e.id).innerHTML;
	document.title = monitor;
	ws.send("view " + monitor_id);
	setTimeout(function() {
		ws.send("sysmon");
	}, 100)
	redraw();
	E('tool').className = "s2";
	E('body').className = "s2";
	E('sysmon').style.color = "black";
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

function keys(event) {
	let k = "";
	if(_alt > 0 || event.altKey) k += " alt";
	if(_ctrl > 0 || event.ctrlKey) k += " ctrl";
	if(_shift > 0 || event.shiftKey) k += " shift";
	return k;
}

var _alt=0;
var _shift=0;
var _ctrl=0;
function alt(m) {
	if(m != 2) _alt = m;
	else _alt = (_alt!=m)? m : 0;
	E("alt").className = "s" + _alt;
	if(event != undefined) {
		event.preventDefault();
		event.stopPropagation();
		E("body").focus();
	}
	return false;
}
function shift(m) {
	if(m != 2) _shift = m;
	else _shift = (_shift!=m)? m : 0;
	E("shift").className = "s" + _shift;
	if(event != undefined) {
		event.preventDefault();
		event.stopPropagation();
		E("body").focus();
	}
	return false;
}
function ctrl(m) {
	if(m != 2) _ctrl = m;
	else _ctrl = (_ctrl!=m)? m : 0;
	E("ctrl").className = "s" + _ctrl;
	if(event != undefined) {
		event.preventDefault();
		event.stopPropagation();
		E("body").focus();
	}
	return false;
}

function load() {
	ws.send("browser");
	if(event != undefined) {
		event.preventDefault();
		event.stopPropagation();
	}	
}

function sysmon(o) {
	E(o[1]).innerHTML = o[2];
}
function sysinfo(o) {
	let t = "<span>" + o[0].toUpperCase();
	for(let i=1; i<o.length; i++) {
		t += " " + o[i];
	}
	t + "</span>";
	E("info").innerHTML = t.replace(/\n/g, "<br>");
}

var hideinfo_time = null;
function showinfo(m) {
	if(monitor == null) return;
	if(m == 0) m = "cpu"; 
	else if(m == 1) m = "mem"; 
	else if(m == 2) m = "drv";
	else m = null;
	if(m != null) {
		ws.send(m);
		if(hideinfo_time != null) {
			clearTimeout(hideinfo_time);
			hideinfo_time = null;
		}
		E("info").style.display = "inline-block";
		E("info").innerHTML  = "";
	} 
	hideinfo_time = setTimeout(function() {
		hideinfo_time = null;
		E("info").style.display = "none";
		E("info").innerHTML = "";
	}, 10000);
}

