
local function append_all(list, list1)
	for i, v in ipairs(list1) do
		table.insert(list, v)
	end
end

local property = {
	{name = "Lane Geometry", item = {
		{name = "Half Lane", op = 920},
		{name = "Hybrid Lane", op = 922},
		{name = "Separate Lane", op = 924}
	}},
	{name = "Score Graph", item = {
		{name = "Off", op = 900},
		{name = "On", op = 901}
	}},
	{name = "Judge Count", item = {
		{name = "Off", op = 905},
		{name = "On", op = 906}
	}},
	{name = "Judge Detail", item = {
		{name = "Off", op = 910},
		{name = "EARLY/LATE", op = 911},
		{name = "+-ms", op = 912}
	}}
}
local function is_half_lane()
	return skin_config.option["Lane Geometry"] == 920
end
local function is_hybrid_lane()
	return skin_config.option["Lane Geometry"] == 922
end
local function is_separate_lane()
	return skin_config.option["Lane Geometry"] == 924
end
local function is_score_graph_enabled()
	return skin_config.option["Score Graph"] == 901
end
local function is_judge_count_enabled()
	return skin_config.option["Judge Count"] == 906
end
local function is_judge_detail_early_late()
	return skin_config.option["Judge Detail"] == 911
end
local function is_judge_detail_ms()
	return skin_config.option["Judge Detail"] == 912
end

local function timer_key_on(index)
	if index <= 9 then
		return 50 + index
	else
		return 1000 + index
	end
end

local function timer_key_hold(index)
	if index <= 9 then
		return 70 + index
	else
		return 1200 + index
	end
end

local function value_judge(index)
	if index <= 9 then
		return 500 + index
	else
		return 1500 + index
	end
end

local filepath = {
	{name = "Theme", path = "keyboard/*.png"},
	{name = "Laser", path = "laser/*.png"}
}

local header = {
	type = 16,
	name = "beatoraja default (lua)",
	w = 1280,
	h = 720,
	playstart = 1000,
	scene = 3600000,
	input = 500,
	close = 1500,
	fadeout = 1000,
	property = property,
	filepath = filepath
}

local function main()
	local skin = {}
	for k, v in pairs(header) do
		skin[k] = v
	end

	local geometry = {}

	if is_half_lane() then
		geometry.lanes_x = 56
		geometry.lanes_w = 560
		geometry.title_align = 0
		geometry.judge_x = 240
		geometry.ready_x = 161
		geometry.title_x = 700
		geometry.bga_x = 700
		geometry.bga_y = 144
		geometry.bga_w = 512
		geometry.bga_h = 512
	end
	if is_hybrid_lane() then
		geometry.lanes_x = 40
		geometry.lanes_w = 578
		geometry.title_align = 0
		geometry.judge_x = 240
		geometry.ready_x = 154
		geometry.title_x = 700
		geometry.bga_x = 700
		geometry.bga_y = 144
		geometry.bga_w = 512
		geometry.bga_h = 512
	end
	if is_separate_lane() then
		geometry.lanes_x = 20
		geometry.lanes_w = 950
		geometry.title_align = 1
		geometry.judge_x = 375
		geometry.ready_x = 320
		geometry.title_x = 495
		geometry.bga_x = 1000
		geometry.bga_y = 440
		geometry.bga_w = 256
		geometry.bga_h = 256
	end

	skin.source = {
		{id = 0, path = "system.png"},
		{id = 1, path = "play.png"},
		{id = 2, path = "playbg.png"},
		{id = 3, path = "gauge.png"},
		{id = 4, path = "judge.png"},
		{id = 5, path = "number.png"},
		{id = 6, path = "laser/*.png"},
		{id = 7, path = "keyboard/*.png"},
		{id = 8, path = "close.png"},
		{id = 9, path = "scoregraph.png"},
		{id = 10, path = "bomb.png"},
		{id = 11, path = "ready.png"},
		{id = 12, path = "lanecover.png"},
		{id = 13, path = "judgedetail.png"}
	}
	skin.font = {
		{id = 0, path = "VL-Gothic-Regular.ttf"}
	}

	skin.image = {
		{id = "background", src = 1, x = 0, y = 0, w = 1280, h = 720},
		{id = 1, src = 2, x = 0, y = 0, w = 1280, h = 720},
		{id = 6, src = 11, x = 0, y = 0, w = 216, h = 40},
		{id = 7, src = 0, x = 0, y = 0, w = 8, h = 8},
		{id = "close1", src = 8, x = 0, y = 500, w = 640, h = 240},
		{id = "close2", src = 8, x = 0, y = 740, w = 640, h = 240},
		{id = 11, src = 0, x = 168, y = 108, w = 126, h = 303},
		{id = 12, src = 0, x = 40, y = 108, w = 126, h = 303},
		{id = 13, src = 0, x = 10, y = 10, w = 10, h = 251},
		{id = 15, src = 0, x = 16, y = 0, w = 8, h = 8},

		{id = 100, src = 6, x = 47, y = 0, w = 28, h = 255},
		{id = 101, src = 6, x = 75, y = 0, w = 21, h = 255},
		{id = 102, src = 6, x = 0, y = 0, w = 47, h = 255},
		{id = "keybeam-w", src = 6, x = 48, y = 0, w = 27, h = 255},
		{id = "keybeam-b", src = 6, x = 76, y = 0, w = 20, h = 255},
		{id = "keybeam-s", src = 6, x = 0, y = 0, w = 47, h = 255},
		{id = "keybeam-w-pg", src = 6, x = 145, y = 0, w = 27, h = 255},
		{id = "keybeam-b-pg", src = 6, x = 173, y = 0, w = 20, h = 255},
		{id = "keybeam-s-pg", src = 6, x = 97, y = 0, w = 47, h = 255},
		
		{id = "note-b", src = 7, x = 1207, y = 405, w = 21, h = 12},
		{id = "note-w", src = 7, x = 1179, y = 405, w = 27, h = 12},
		{id = "note-su", src = 7, x = 1024, y = 400, w = 50, h = 15},
		{id = "note-sd", src = 7, x = 1074, y = 400, w = 50, h = 15},
		
		{id = "lns-b", src = 7, x = 1207, y = 457, w = 21, h = 13},
		{id = "lns-w", src = 7, x = 1179, y = 457, w = 27, h = 13},
		{id = "lns-s", src = 7, x = 1130, y = 457, w = 46, h = 12},

		{id = "lne-b", src = 7, x = 1207, y = 443, w = 21, h = 13},
		{id = "lne-w", src = 7, x = 1179, y = 443, w = 27, h = 13},
		{id = "lne-s", src = 7, x = 1130, y = 443, w = 46, h = 12},
		
		{id = "lnb-b", src = 7, x = 1207, y = 480, w = 21, h = 1},
		{id = "lnb-w", src = 7, x = 1179, y = 480, w = 27, h = 1},
		{id = "lnb-s", src = 7, x = 1130, y = 480, w = 46, h = 1},

		{id = "lna-b", src = 7, x = 1207, y = 476, w = 21, h = 1},
		{id = "lna-w", src = 7, x = 1179, y = 476, w = 27, h = 1},
		{id = "lna-s", src = 7, x = 1130, y = 476, w = 46, h = 1},

		{id = "hcns-b", src = 7, x = 1207, y = 508, w = 21, h = 13},
		{id = "hcns-w", src = 7, x = 1179, y = 508, w = 27, h = 13},
		{id = "hcns-s", src = 7, x = 1130, y = 508, w = 46, h = 12},

		{id = "hcne-b", src = 7, x = 1207, y = 494, w = 21, h = 13},
		{id = "hcne-w", src = 7, x = 1179, y = 494, w = 27, h = 13},
		{id = "hcne-s", src = 7, x = 1130, y = 494, w = 46, h = 12},

		{id = "hcnb-b", src = 7, x = 1207, y = 531, w = 21, h = 1},
		{id = "hcnb-w", src = 7, x = 1179, y = 531, w = 27, h = 1},
		{id = "hcnb-s", src = 7, x = 1130, y = 531, w = 46, h = 1},

		{id = "hcna-b", src = 7, x = 1207, y = 527, w = 21, h = 1},
		{id = "hcna-w", src = 7, x = 1179, y = 527, w = 27, h = 1},
		{id = "hcna-s", src = 7, x = 1130, y = 527, w = 46, h = 1},
		
		{id = "hcnd-b", src = 7, x = 1207, y = 528, w = 21, h = 1},
		{id = "hcnd-w", src = 7, x = 1179, y = 528, w = 27, h = 1},
		{id = "hcnd-s", src = 7, x = 1130, y = 528, w = 46, h = 1},

		{id = "hcnr-b", src = 7, x = 1207, y = 529, w = 21, h = 1},
		{id = "hcnr-w", src = 7, x = 1179, y = 529, w = 27, h = 1},
		{id = "hcnr-s", src = 7, x = 1130, y = 529, w = 46, h = 1},
		
		{id = "mine-b", src = 7, x = 1207, y = 423, w = 21, h = 8},
		{id = "mine-w", src = 7, x = 1179, y = 423, w = 27, h = 8},
		{id = "mine-s", src = 7, x = 1130, y = 423, w = 46, h = 8},

		{id = "section-line", src = 0, x = 0, y = 0, w = 1, h = 1},
		
		{id = "gauge-n1", src = 3, x = 0, y = 0, w = 5, h = 17},
		{id = "gauge-n2", src = 3, x = 5, y = 0, w = 5, h = 17},
		{id = "gauge-n3", src = 3, x = 10, y = 0, w = 5, h = 17},
		{id = "gauge-n4", src = 3, x = 15, y = 0, w = 5, h = 17},
		{id = "gauge-e1", src = 3, x = 0, y = 17, w = 5, h = 17},
		{id = "gauge-e2", src = 3, x = 5, y = 17, w = 5, h = 17},
		{id = "gauge-e3", src = 3, x = 10, y = 17, w = 5, h = 17},
		{id = "gauge-e4", src = 3, x = 15, y = 17, w = 5, h = 17},
		
		{id = "judgef-pg", src = 4, x = 0, y = 0, w = 180, h = 100, divy = 2, cycle = 100},
		{id = "judgef-gr", src = 4, x = 0, y = 150, w = 180, h = 50},
		{id = "judgef-gd", src = 4, x = 0, y = 200, w = 180, h = 50},
		{id = "judgef-bd", src = 4, x = 0, y = 250, w = 180, h = 50},
		{id = "judgef-pr", src = 4, x = 0, y = 300, w = 180, h = 50},
		{id = "judgef-ms", src = 4, x = 0, y = 300, w = 180, h = 50},

		{id = "judge-early", src = 13, x = 0, y = 0, w = 50, h = 20},
		{id = "judge-late", src = 13, x = 50, y = 0, w = 50, h = 20}
	}

	local function bomb_image(index, prefix, src_y, timer_func)
		local name = index
		if index == 25 then
			name = "su"
		elseif index == 26 then
			name = "sd"
		end
		return {id = prefix..name, src = 10, x = 0, y = src_y, w = 1810, h = 192, divx = 10, timer = timer_func(index), cycle = 160}
	end
	for i = 1, 26 do
		table.insert(skin.image, bomb_image(i, "bomb1-", 0, timer_key_on))
		table.insert(skin.image, bomb_image(i, "bomb2-", 576, timer_key_on))
		table.insert(skin.image, bomb_image(i, "bomb3-", 192, timer_key_on))
		table.insert(skin.image, bomb_image(i, "hold-", 384, timer_key_hold))
	end
	if is_half_lane() then
		table.insert(skin.image, {id = "lane-bg", src = 7, x = 56, y = 0, w = 560, h = 80})
		table.insert(skin.image, {id = "keys", src = 7, x = 56, y = 100, w = 560, h = 80})
	end
	if is_hybrid_lane() then
		table.insert(skin.image, {id = "lane-bg", src = 7, x = 40, y = 200, w = 578, h = 80})
		table.insert(skin.image, {id = "keys", src = 7, x = 40, y = 300, w = 578, h = 80})
	end
	if is_separate_lane() then
		table.insert(skin.image, {id = "lane-bg", src = 7, x = 0, y = 400, w = 950, h = 80})
		table.insert(skin.image, {id = "keys", src = 7, x = 0, y = 480, w = 950, h = 80})
	end
	skin.imageset = {}
	local function keybeam_imageset(index)
		local name = index
		if index == 25 then
			name = "su"
		elseif index == 26 then
			name = "sd"
		end
		local wbs
		if index == 25 or index == 26 then
			wbs = "s"
		elseif index % 2 == 1 then
			wbs = "w"
		else
			wbs = "b"
		end
		return {id = "keybeam"..name, ref = value_judge(index), images = { "keybeam-"..wbs, "keybeam-"..wbs.."-pg" }}
	end
	local function bomb_imageset(index)
		local name = index
		if index == 25 then
			name = "su"
		elseif index == 26 then
			name = "sd"
		end
		return {id = index + 109, ref = value_judge(index), images = { "bomb1-"..name, "bomb2-"..name, "bomb1-"..name, "bomb3-"..name }}
	end
	for i = 1, 26 do
		table.insert(skin.imageset, keybeam_imageset(i))
		table.insert(skin.imageset, bomb_imageset(i))
	end
	skin.value = {
		{id = 400, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 91},
		{id = 401, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 160},
		{id = 402, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 90},
		{id = 403, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 2, ref = 163},
		{id = 404, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 2, padding = 1, ref = 164},
		{id = 405, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 2, ref = 310},
		{id = 406, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 2, padding = 1, ref = 311},
		{id = 407, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 312},
		{id = 410, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 3, ref = 107},
		{id = 411, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 1, ref = 407},
		{id = 420, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 102},
		{id = 421, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 1, ref = 103},
		{id = 422, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 5, ref = 71},
		{id = 423, src = 5, x = 0, y = 24, w = 240, h = 24, divx = 10, digit = 5, ref = 150},
		{id = 424, src = 5, x = 0, y = 48, w = 240, h = 24, divx = 10, digit = 5, ref = 121},

		{id = 430, src = 5, x = 0, y = 0, w = 264, h = 24, divx = 11, digit = 4, ref = 110},
		{id = 431, src = 5, x = 0, y = 24, w = 264, h = 24, divx = 11, digit = 4, ref = 410},
		{id = 432, src = 5, x = 0, y = 48, w = 264, h = 24, divx = 11, digit = 4, ref = 411},
		{id = 433, src = 5, x = 0, y = 0, w = 264, h = 24, divx = 11, digit = 4, ref = 111},
		{id = 434, src = 5, x = 0, y = 24, w = 264, h = 24, divx = 11, digit = 4, ref = 412},
		{id = 435, src = 5, x = 0, y = 48, w = 264, h = 24, divx = 11, digit = 4, ref = 413},
		{id = 436, src = 5, x = 0, y = 0, w = 264, h = 24, divx = 11, digit = 4, ref = 112},
		{id = 437, src = 5, x = 0, y = 24, w = 264, h = 24, divx = 11, digit = 4, ref = 414},
		{id = 438, src = 5, x = 0, y = 48, w = 264, h = 24, divx = 11, digit = 4, ref = 415},
		{id = 439, src = 5, x = 0, y = 0, w = 264, h = 24, divx = 11, digit = 4, ref = 113},
		{id = 440, src = 5, x = 0, y = 24, w = 264, h = 24, divx = 11, digit = 4, ref = 416},
		{id = 441, src = 5, x = 0, y = 48, w = 264, h = 24, divx = 11, digit = 4, ref = 417},
		{id = 442, src = 5, x = 0, y = 0, w = 264, h = 24, divx = 11, digit = 4, ref = 114},
		{id = 443, src = 5, x = 0, y = 24, w = 264, h = 24, divx = 11, digit = 4, ref = 418},
		{id = 444, src = 5, x = 0, y = 48, w = 264, h = 24, divx = 11, digit = 4, ref = 419},
		{id = 445, src = 5, x = 0, y = 0, w = 264, h = 24, divx = 11, digit = 4, ref = 420},
		{id = 446, src = 5, x = 0, y = 24, w = 264, h = 24, divx = 11, digit = 4, ref = 421},
		{id = 447, src = 5, x = 0, y = 48, w = 264, h = 24, divx = 11, digit = 4, ref = 422},

		{id = 450, src = 0, x = 0, y = 550, w = 100, h = 15, divx = 10, digit = 3, ref = 14},
		{id = 451, src = 0, x = 0, y = 565, w = 100, h = 15, divx = 10, digit = 4, ref = 312},
		
		{id = "judgen-pg", src = 4, x = 200, y = 0, w = 300, h = 100, divx = 10, divy = 2, digit = 6, ref = 75, cycle = 100},
		{id = "judgen-gr", src = 4, x = 200, y = 150, w = 300, h = 50, divx = 10, digit = 6, ref = 75},
		{id = "judgen-gd", src = 4, x = 200, y = 200, w = 300, h = 50, divx = 10, digit = 6, ref = 75},
		{id = "judgen-bd", src = 4, x = 200, y = 250, w = 300, h = 50, divx = 10, digit = 6, ref = 75},
		{id = "judgen-pr", src = 4, x = 200, y = 300, w = 300, h = 50, divx = 10, digit = 6, ref = 75},
		{id = "judgen-ms", src = 4, x = 200, y = 300, w = 300, h = 50, divx = 10, digit = 6, ref = 75},

		{id = "judgems-1pp", src = 13, x = 0, y = 20, w = 120, h = 40, divx = 12, divy = 2, digit = 4, ref = 525},
		{id = "judgems-1pg", src = 13, x = 0, y = 60, w = 120, h = 40, divx = 12, divy = 2, digit = 4, ref = 525}
	}
	skin.text = {
		{id = 1000, font = 0, size = 24, align = geometry.title_align, ref = 12}
	}
	skin.slider = {
		{id = 1050, src = 0, x = 0, y = 289, w = 14, h = 20, angle = 2, range = 520,type = 6},
		{id = 1051, src = 0, x = 15, y = 289, w = 14, h = 20, angle = 2, range = 520,type = 6},
		{id = 1060, src = 12, x = 0, y = 0, w = 390, h = 580, angle = 2, range = 580,type = 4}
	}
	skin.hiddenCover = {
		{id = "hidden-cover", src = 12, x = 0, y = 0, w = 390, h = 580, disapearLine = 140, isDisapearLineLinkLift = true}
	}
	skin.graph = {
		{id = "graph-now", src = 9, x = 0, y = 0, w = 100, h = 296, type = 110},
		{id = "graph-best", src = 9, x = 100, y = 0, w = 100, h = 296, type = 113},
		{id = "graph-target", src = 9, x = 200, y = 0, w = 100, h = 296, type = 115},
		{id = "load-progress", src = 0, x = 0, y = 0, w = 8, h = 8, angle = 0, type = 102}
	}
	skin.note = {
		id = "notes",
		note = {"note-w", "note-b", "note-w", "note-b", "note-w", "note-w", "note-b", "note-w", "note-b", "note-w", "note-b", "note-w", "note-w", "note-b", "note-w", "note-b", "note-w", "note-w", "note-b", "note-w", "note-b", "note-w", "note-b", "note-w", "note-su", "note-sd"},
		lnend = {"lne-w", "lne-b", "lne-w", "lne-b", "lne-w", "lne-w", "lne-b", "lne-w", "lne-b", "lne-w", "lne-b", "lne-w", "lne-w", "lne-b", "lne-w", "lne-b", "lne-w", "lne-w", "lne-b", "lne-w", "lne-b", "lne-w", "lne-b", "lne-w", "lne-s", "lne-s"},
		lnstart = {"lns-w", "lns-b", "lns-w", "lns-b", "lns-w", "lns-w", "lns-b", "lns-w", "lns-b", "lns-w", "lns-b", "lns-w", "lns-w", "lns-b", "lns-w", "lns-b", "lns-w", "lns-w", "lns-b", "lns-w", "lns-b", "lns-w", "lns-b", "lns-w", "lns-s", "lne-s"},
		lnbody = {"lnb-w", "lnb-b", "lnb-w", "lnb-b", "lnb-w", "lnb-w", "lnb-b", "lnb-w", "lnb-b", "lnb-w", "lnb-b", "lnb-w", "lnb-w", "lnb-b", "lnb-w", "lnb-b", "lnb-w", "lnb-w", "lnb-b", "lnb-w", "lnb-b", "lnb-w", "lnb-b", "lnb-w", "lnb-s", "lnb-s"},
		lnactive = {"lna-w", "lna-b", "lna-w", "lna-b", "lna-w", "lna-w", "lna-b", "lna-w", "lna-b", "lna-w", "lna-b", "lna-w", "lna-w", "lna-b", "lna-w", "lna-b", "lna-w", "lna-w", "lna-b", "lna-w", "lna-b", "lna-w", "lna-b", "lna-w", "lna-s", "lna-s"},
		hcnend = {"hcne-w", "hcne-b", "hcne-w", "hcne-b", "hcne-w", "hcne-w", "hcne-b", "hcne-w", "hcne-b", "hcne-w", "hcne-b", "hcne-w", "hcne-w", "hcne-b", "hcne-w", "hcne-b", "hcne-w", "hcne-w", "hcne-b", "hcne-w", "hcne-b", "hcne-w", "hcne-b", "hcne-w", "hcne-s", "hcne-s"},
		hcnstart = {"hcns-w", "hcns-b", "hcns-w", "hcns-b", "hcns-w", "hcns-w", "hcns-b", "hcns-w", "hcns-b", "hcns-w", "hcns-b", "hcns-w", "hcns-w", "hcns-b", "hcns-w", "hcns-b", "hcns-w", "hcns-w", "hcns-b", "hcns-w", "hcns-b", "hcns-w", "hcns-b", "hcns-w", "hcns-s", "hcns-s"},
		hcnbody = {"hcnb-w", "hcnb-b", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-s", "hcnb-s"},
		hcnactive = {"hcna-w", "hcna-b", "hcna-w", "hcna-b", "hcna-w", "hcna-w", "hcna-b", "hcna-w", "hcna-b", "hcna-w", "hcna-b", "hcna-w", "hcna-w", "hcna-b", "hcna-w", "hcna-b", "hcna-w", "hcna-w", "hcna-b", "hcna-w", "hcna-b", "hcna-w", "hcna-b", "hcna-w", "hcna-s", "hcna-s"},
		hcndamage = {"hcnd-w", "hcnd-b", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-s", "hcnd-s"},
		hcnreactive = {"hcnr-w", "hcnr-b", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-s", "hcnr-s"},
		mine = {"mine-w", "mine-b", "mine-w", "mine-b", "mine-w", "mine-w", "mine-b", "mine-w", "mine-b", "mine-w", "mine-b", "mine-w", "mine-w", "mine-b", "mine-w", "mine-b", "mine-w", "mine-w", "mine-b", "mine-w", "mine-b", "mine-w", "mine-b", "mine-w", "mine-s", "mine-s"},
		hidden = {},
		processed = {},
		group = {
			{id = "section-line", offset = 50, dst = {
				{x = geometry.lanes_x, y = 140, w = geometry.lanes_w, h = 1, r = 128, g = 128, b = 128}
			}}
		},
		time = {
			{id = "section-line", offset = 50, dst = {
				{x = geometry.lanes_x, y = 140, w = geometry.lanes_w, h = 1, r = 64, g = 192, b = 192}
			}}
		},
		bpm = {
			{id = "section-line", offset = 50, dst = {
				{x = geometry.lanes_x, y = 140, w = geometry.lanes_w, h = 2, r = 0, g = 192, b = 0}
			}}
		},
		stop = {
			{id = "section-line", offset = 50, dst = {
				{x = geometry.lanes_x, y = 140, w = geometry.lanes_w, h = 2, r = 192, g = 192, b = 0}
			}}
		}
	}
	if is_half_lane() then
		skin.note.dst = {
			{x = 112,y = 140,w = 36,h = 580},
			{x = 130,y = 140,w = 36,h = 580},
			{x = 148,y = 140,w = 36,h = 580},
			{x = 166,y = 140,w = 36,h = 580},
			{x = 184,y = 140,w = 36,h = 580},
			{x = 220,y = 140,w = 36,h = 580},
			{x = 238,y = 140,w = 36,h = 580},
			{x = 256,y = 140,w = 36,h = 580},
			{x = 274,y = 140,w = 36,h = 580},
			{x = 292,y = 140,w = 36,h = 580},
			{x = 310,y = 140,w = 36,h = 580},
			{x = 328,y = 140,w = 36,h = 580},
			{x = 364,y = 140,w = 36,h = 580},
			{x = 382,y = 140,w = 36,h = 580},
			{x = 400,y = 140,w = 36,h = 580},
			{x = 418,y = 140,w = 36,h = 580},
			{x = 436,y = 140,w = 36,h = 580},
			{x = 472,y = 140,w = 36,h = 580},
			{x = 490,y = 140,w = 36,h = 580},
			{x = 508,y = 140,w = 36,h = 580},
			{x = 526,y = 140,w = 36,h = 580},
			{x = 544,y = 140,w = 36,h = 580},
			{x = 562,y = 140,w = 36,h = 580},
			{x = 580,y = 140,w = 36,h = 580},
			{x = 56,y = 140,w = 56,h = 580},
			{x = 56,y = 140,w = 56,h = 580}
		}
	end
	if is_hybrid_lane() then
		skin.note.dst = {
			{x = 90,y = 140,w = 32,h = 580},
			{x = 111,y = 140,w = 30,h = 580},
			{x = 130,y = 140,w = 32,h = 580},
			{x = 151,y = 140,w = 30,h = 580},
			{x = 170,y = 140,w = 32,h = 580},
			{x = 202,y = 140,w = 32,h = 580},
			{x = 223,y = 140,w = 30,h = 580},
			{x = 242,y = 140,w = 32,h = 580},
			{x = 263,y = 140,w = 30,h = 580},
			{x = 282,y = 140,w = 32,h = 580},
			{x = 303,y = 140,w = 30,h = 580},
			{x = 322,y = 140,w = 32,h = 580},
			{x = 354,y = 140,w = 32,h = 580},
			{x = 375,y = 140,w = 30,h = 580},
			{x = 394,y = 140,w = 32,h = 580},
			{x = 415,y = 140,w = 30,h = 580},
			{x = 434,y = 140,w = 32,h = 580},
			{x = 466,y = 140,w = 32,h = 580},
			{x = 487,y = 140,w = 30,h = 580},
			{x = 506,y = 140,w = 32,h = 580},
			{x = 527,y = 140,w = 30,h = 580},
			{x = 546,y = 140,w = 32,h = 580},
			{x = 567,y = 140,w = 30,h = 580},
			{x = 586,y = 140,w = 32,h = 580},
			{x = 40,y = 140,w = 50,h = 580},
			{x = 40,y = 140,w = 50,h = 580}
		}
	end
	if is_separate_lane() then
		skin.note.dst = {
			{x = 90,y = 140,w = 40,h = 580},
			{x = 130,y = 140,w = 32,h = 580},
			{x = 162,y = 140,w = 40,h = 580},
			{x = 202,y = 140,w = 32,h = 580},
			{x = 234,y = 140,w = 40,h = 580},
			{x = 274,y = 140,w = 40,h = 580},
			{x = 314,y = 140,w = 32,h = 580},
			{x = 346,y = 140,w = 40,h = 580},
			{x = 386,y = 140,w = 32,h = 580},
			{x = 418,y = 140,w = 40,h = 580},
			{x = 458,y = 140,w = 32,h = 580},
			{x = 490,y = 140,w = 40,h = 580},
			{x = 530,y = 140,w = 40,h = 580},
			{x = 570,y = 140,w = 32,h = 580},
			{x = 602,y = 140,w = 40,h = 580},
			{x = 642,y = 140,w = 32,h = 580},
			{x = 674,y = 140,w = 40,h = 580},
			{x = 714,y = 140,w = 40,h = 580},
			{x = 754,y = 140,w = 32,h = 580},
			{x = 786,y = 140,w = 40,h = 580},
			{x = 826,y = 140,w = 32,h = 580},
			{x = 858,y = 140,w = 40,h = 580},
			{x = 898,y = 140,w = 32,h = 580},
			{x = 930,y = 140,w = 40,h = 580},
			{x = 20,y = 140,w = 70,h = 580},
			{x = 20,y = 140,w = 70,h = 580}
		}
	end
	skin.gauge = {
		id = 2001,
		nodes = {"gauge-n1","gauge-n2","gauge-n3","gauge-n4","gauge-e1","gauge-e2","gauge-e3","gauge-e4"}
	}
	skin.judge = {
		{
			id = 2010,
			index = 0,
			images = {
				{id = "judgef-pg", loop = -1, timer = 46 ,offset = 50, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-gr", loop = -1, timer = 46 ,offset = 50, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-gd", loop = -1, timer = 46 ,offset = 50, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-bd", loop = -1, timer = 46 ,offset = 50, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-pr", loop = -1, timer = 46 ,offset = 50, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-ms", loop = -1, timer = 46 ,offset = 50, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}}
			},
			numbers = {
				{id = "judgen-pg", loop = -1, timer = 46, dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-gr", loop = -1, timer = 46, dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-gd", loop = -1, timer = 46, dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-bd", loop = -1, timer = 46, dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-pr", loop = -1, timer = 46, dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-ms", loop = -1, timer = 46, dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}}
			},
			shift = true
		}
	}
	skin.bga = {
		id = 2002
	}
	skin.destination = {
		{id = "background", dst = {
			{x = 0, y = 0, w = 1280, h = 720}
		}},
		{id = 1, dst = {
			{x = 0, y = 0, w = 1280, h = 720}
		}},

		{id = 400, dst = {
			{x = 520, y = 2, w = 18, h = 18}
		}},
		{id = 401, dst = {
			{x = 592, y = 2, w = 24, h = 24}
		}},
		{id = 402, dst = {
			{x = 688, y = 2, w = 18, h = 18}
		}},
		{id = 403, dst = {
			{x = 1148, y = 2, w = 24, h = 24}
		}},
		{id = 404, dst = {
			{x = 1220, y = 2, w = 24, h = 24}
		}},
		{id = 405, dst = {
			{x = 116, y = 2, w = 12, h = 24}
		}},
		{id = 406, dst = {
			{x = 154, y = 2, w = 10, h = 20}
		}},
		{id = 407, dst = {
			{x = 318, y = 2, w = 12, h = 24}
		}},

		{id = 13, dst = {
			{x = 4, y = 140, w = 12, h = 540}
		}},

		{id = "lane-bg", loop = 1000, dst = {
			{time = 0, x = geometry.lanes_x, y = 140, w = geometry.lanes_w, h = 0, a = 0},
			{time = 1000, h = 580, a = 255}
		}},
		{id = "keys", dst = {
			{x = geometry.lanes_x, y = 100, w = geometry.lanes_w, h = 80}
		}}
	}
	if is_half_lane() then
		append_all(skin.destination, {
			{id = "keybeam1",timer = 101,loop = 100,offset = 50,dst = {{time = 0,x = 121,y = 140,w = 18,h = 580},{time = 100,x = 112,w = 36}}},
			{id = "keybeam3",timer = 103,loop = 100,offset = 50,dst = {{time = 0,x = 157,y = 140,w = 18,h = 580},{time = 100,x = 148,w = 36}}},
			{id = "keybeam5",timer = 105,loop = 100,offset = 50,dst = {{time = 0,x = 193,y = 140,w = 18,h = 580},{time = 100,x = 184,w = 36}}},
			{id = "keybeam6",timer = 106,loop = 100,offset = 50,dst = {{time = 0,x = 229,y = 140,w = 18,h = 580},{time = 100,x = 220,w = 36}}},
			{id = "keybeam8",timer = 108,loop = 100,offset = 50,dst = {{time = 0,x = 265,y = 140,w = 18,h = 580},{time = 100,x = 256,w = 36}}},
			{id = "keybeam10",timer = 1410,loop = 100,offset = 50,dst = {{time = 0,x = 301,y = 140,w = 18,h = 580},{time = 100,x = 292,w = 36}}},
			{id = "keybeam12",timer = 1412,loop = 100,offset = 50,dst = {{time = 0,x = 337,y = 140,w = 18,h = 580},{time = 100,x = 328,w = 36}}},
			{id = "keybeam13",timer = 1413,loop = 100,offset = 50,dst = {{time = 0,x = 373,y = 140,w = 18,h = 580},{time = 100,x = 364,w = 36}}},
			{id = "keybeam15",timer = 1415,loop = 100,offset = 50,dst = {{time = 0,x = 409,y = 140,w = 18,h = 580},{time = 100,x = 400,w = 36}}},
			{id = "keybeam17",timer = 1417,loop = 100,offset = 50,dst = {{time = 0,x = 445,y = 140,w = 18,h = 580},{time = 100,x = 436,w = 36}}},
			{id = "keybeam18",timer = 1418,loop = 100,offset = 50,dst = {{time = 0,x = 481,y = 140,w = 18,h = 580},{time = 100,x = 472,w = 36}}},
			{id = "keybeam20",timer = 1420,loop = 100,offset = 50,dst = {{time = 0,x = 517,y = 140,w = 18,h = 580},{time = 100,x = 508,w = 36}}},
			{id = "keybeam22",timer = 1422,loop = 100,offset = 50,dst = {{time = 0,x = 553,y = 140,w = 18,h = 580},{time = 100,x = 544,w = 36}}},
			{id = "keybeam24",timer = 1424,loop = 100,offset = 50,dst = {{time = 0,x = 589,y = 140,w = 18,h = 580},{time = 100,x = 580,w = 36}}},
			{id = "keybeam2",timer = 102,loop = 100,offset = 50,dst = {{time = 0,x = 139,y = 140,w = 18,h = 580},{time = 100,x = 130,w = 36}}},
			{id = "keybeam4",timer = 104,loop = 100,offset = 50,dst = {{time = 0,x = 175,y = 140,w = 18,h = 580},{time = 100,x = 166,w = 36}}},
			{id = "keybeam7",timer = 107,loop = 100,offset = 50,dst = {{time = 0,x = 247,y = 140,w = 18,h = 580},{time = 100,x = 238,w = 36}}},
			{id = "keybeam9",timer = 109,loop = 100,offset = 50,dst = {{time = 0,x = 283,y = 140,w = 18,h = 580},{time = 100,x = 274,w = 36}}},
			{id = "keybeam11",timer = 1411,loop = 100,offset = 50,dst = {{time = 0,x = 319,y = 140,w = 18,h = 580},{time = 100,x = 310,w = 36}}},
			{id = "keybeam14",timer = 1414,loop = 100,offset = 50,dst = {{time = 0,x = 391,y = 140,w = 18,h = 580},{time = 100,x = 382,w = 36}}},
			{id = "keybeam16",timer = 1416,loop = 100,offset = 50,dst = {{time = 0,x = 427,y = 140,w = 18,h = 580},{time = 100,x = 418,w = 36}}},
			{id = "keybeam19",timer = 1419,loop = 100,offset = 50,dst = {{time = 0,x = 499,y = 140,w = 18,h = 580},{time = 100,x = 490,w = 36}}},
			{id = "keybeam21",timer = 1421,loop = 100,offset = 50,dst = {{time = 0,x = 535,y = 140,w = 18,h = 580},{time = 100,x = 526,w = 36}}},
			{id = "keybeam23",timer = 1423,loop = 100,offset = 50,dst = {{time = 0,x = 571,y = 140,w = 18,h = 580},{time = 100,x = 562,w = 36}}},
			{id = "keybeamsu",timer = 1425,loop = 100,offset = 50,dst = {{time = 0,x = 70,y = 140,w = 28,h = 580},{time = 100,x = 56,w = 56}}},
			{id = "keybeamsd",timer = 1426,loop = 100,offset = 50,dst = {{time = 0,x = 70,y = 140,w = 28,h = 580},{time = 100,x = 56,w = 56}}},
			
			{id = 15, offset = 50, dst = {
				{x = 56, y = 137, w = 560, h = 6}
			}},
			{id = "notes"},
			
			{id = 110,timer = 51,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 50,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 111,timer = 52,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 68,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 112,timer = 53,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 86,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 113,timer = 54,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 104,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 114,timer = 55,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 122,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 115,timer = 56,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 158,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 116,timer = 57,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 176,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 117,timer = 58,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 194,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 118,timer = 59,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 212,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 119,timer = 1010,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 230,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 120,timer = 1011,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 248,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 121,timer = 1012,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 266,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 122,timer = 1013,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 302,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 123,timer = 1014,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 320,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 124,timer = 1015,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 338,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 125,timer = 1016,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 356,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 126,timer = 1017,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 374,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 127,timer = 1018,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 410,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 128,timer = 1019,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 428,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 129,timer = 1020,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 446,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 130,timer = 1021,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 464,y = 28,w = 180,h = 192},{time = 160}}},
			
			{id = "hold-1",timer = 71,blend = 2,offset = 50,dst = {{time = 0,x = 50,y = 28,w = 180,h = 192}}},
			{id = "hold-2",timer = 72,blend = 2,offset = 50,dst = {{time = 0,x = 68,y = 28,w = 180,h = 192}}},
			{id = "hold-3",timer = 73,blend = 2,offset = 50,dst = {{time = 0,x = 86,y = 28,w = 180,h = 192}}},
			{id = "hold-4",timer = 74,blend = 2,offset = 50,dst = {{time = 0,x = 104,y = 28,w = 180,h = 192}}},
			{id = "hold-5",timer = 75,blend = 2,offset = 50,dst = {{time = 0,x = 122,y = 28,w = 180,h = 192}}},
			{id = "hold-6",timer = 76,blend = 2,offset = 50,dst = {{time = 0,x = 158,y = 28,w = 180,h = 192}}},
			{id = "hold-7",timer = 77,blend = 2,offset = 50,dst = {{time = 0,x = 176,y = 28,w = 180,h = 192}}},
			{id = "hold-8",timer = 78,blend = 2,offset = 50,dst = {{time = 0,x = 194,y = 28,w = 180,h = 192}}},
			{id = "hold-9",timer = 79,blend = 2,offset = 50,dst = {{time = 0,x = 212,y = 28,w = 180,h = 192}}},
			{id = "hold-10",timer = 1210,blend = 2,offset = 50,dst = {{time = 0,x = 230,y = 28,w = 180,h = 192}}},
			{id = "hold-11",timer = 1211,blend = 2,offset = 50,dst = {{time = 0,x = 248,y = 28,w = 180,h = 192}}},
			{id = "hold-12",timer = 1212,blend = 2,offset = 50,dst = {{time = 0,x = 266,y = 28,w = 180,h = 192}}},
			{id = "hold-13",timer = 1213,blend = 2,offset = 50,dst = {{time = 0,x = 302,y = 28,w = 180,h = 192}}},
			{id = "hold-14",timer = 1214,blend = 2,offset = 50,dst = {{time = 0,x = 320,y = 28,w = 180,h = 192}}},
			{id = "hold-15",timer = 1215,blend = 2,offset = 50,dst = {{time = 0,x = 338,y = 28,w = 180,h = 192}}},
			{id = "hold-16",timer = 1216,blend = 2,offset = 50,dst = {{time = 0,x = 356,y = 28,w = 180,h = 192}}},
			{id = "hold-17",timer = 1217,blend = 2,offset = 50,dst = {{time = 0,x = 374,y = 28,w = 180,h = 192}}},
			{id = "hold-18",timer = 1218,blend = 2,offset = 50,dst = {{time = 0,x = 410,y = 28,w = 180,h = 192}}},
			{id = "hold-19",timer = 1219,blend = 2,offset = 50,dst = {{time = 0,x = 428,y = 28,w = 180,h = 192}}},
			{id = "hold-20",timer = 1220,blend = 2,offset = 50,dst = {{time = 0,x = 446,y = 28,w = 180,h = 192}}},
			{id = "hold-21",timer = 1221,blend = 2,offset = 50,dst = {{time = 0,x = 464,y = 28,w = 180,h = 192}}},
			{id = "hold-22",timer = 1222,blend = 2,offset = 50,dst = {{time = 0,x = 482,y = 28,w = 180,h = 192}}},
			{id = "hold-23",timer = 1223,blend = 2,offset = 50,dst = {{time = 0,x = 500,y = 28,w = 180,h = 192}}},
			{id = "hold-24",timer = 1224,blend = 2,offset = 50,dst = {{time = 0,x = 518,y = 28,w = 180,h = 192}}},
			{id = "hold-su",timer = 1225,blend = 2,offset = 50,dst = {{time = 0,x = 4,y = 28,w = 180,h = 192}}},
			{id = "hold-sd",timer = 1226,blend = 2,offset = 50,dst = {{time = 0,x = 4,y = 28,w = 180,h = 192}}}
		})
	end
	if is_hybrid_lane() then
		append_all(skin.destination, {
			{id = "keybeam1",timer = 101,loop = 100,offset = 50,dst = {{time = 0,x = 98,y = 140,w = 16,h = 580},{time = 100,x = 90,w = 32}}},
			{id = "keybeam3",timer = 103,loop = 100,offset = 50,dst = {{time = 0,x = 138,y = 140,w = 16,h = 580},{time = 100,x = 130,w = 32}}},
			{id = "keybeam5",timer = 105,loop = 100,offset = 50,dst = {{time = 0,x = 178,y = 140,w = 16,h = 580},{time = 100,x = 170,w = 32}}},
			{id = "keybeam6",timer = 106,loop = 100,offset = 50,dst = {{time = 0,x = 210,y = 140,w = 16,h = 580},{time = 100,x = 202,w = 32}}},
			{id = "keybeam8",timer = 108,loop = 100,offset = 50,dst = {{time = 0,x = 250,y = 140,w = 16,h = 580},{time = 100,x = 242,w = 32}}},
			{id = "keybeam10",timer = 1410,loop = 100,offset = 50,dst = {{time = 0,x = 290,y = 140,w = 16,h = 580},{time = 100,x = 282,w = 32}}},
			{id = "keybeam12",timer = 1412,loop = 100,offset = 50,dst = {{time = 0,x = 330,y = 140,w = 16,h = 580},{time = 100,x = 322,w = 32}}},
			{id = "keybeam13",timer = 1413,loop = 100,offset = 50,dst = {{time = 0,x = 362,y = 140,w = 16,h = 580},{time = 100,x = 354,w = 32}}},
			{id = "keybeam15",timer = 1415,loop = 100,offset = 50,dst = {{time = 0,x = 402,y = 140,w = 16,h = 580},{time = 100,x = 394,w = 32}}},
			{id = "keybeam17",timer = 1417,loop = 100,offset = 50,dst = {{time = 0,x = 442,y = 140,w = 16,h = 580},{time = 100,x = 434,w = 32}}},
			{id = "keybeam18",timer = 1418,loop = 100,offset = 50,dst = {{time = 0,x = 474,y = 140,w = 16,h = 580},{time = 100,x = 466,w = 32}}},
			{id = "keybeam20",timer = 1420,loop = 100,offset = 50,dst = {{time = 0,x = 514,y = 140,w = 16,h = 580},{time = 100,x = 506,w = 32}}},
			{id = "keybeam22",timer = 1422,loop = 100,offset = 50,dst = {{time = 0,x = 554,y = 140,w = 16,h = 580},{time = 100,x = 546,w = 32}}},
			{id = "keybeam24",timer = 1424,loop = 100,offset = 50,dst = {{time = 0,x = 594,y = 140,w = 16,h = 580},{time = 100,x = 586,w = 32}}},
			{id = "keybeam2",timer = 102,loop = 100,offset = 50,dst = {{time = 0,x = 118,y = 140,w = 15,h = 580},{time = 100,x = 111,w = 30}}},
			{id = "keybeam4",timer = 104,loop = 100,offset = 50,dst = {{time = 0,x = 158,y = 140,w = 15,h = 580},{time = 100,x = 151,w = 30}}},
			{id = "keybeam7",timer = 107,loop = 100,offset = 50,dst = {{time = 0,x = 230,y = 140,w = 15,h = 580},{time = 100,x = 223,w = 30}}},
			{id = "keybeam9",timer = 109,loop = 100,offset = 50,dst = {{time = 0,x = 270,y = 140,w = 15,h = 580},{time = 100,x = 263,w = 30}}},
			{id = "keybeam11",timer = 1411,loop = 100,offset = 50,dst = {{time = 0,x = 310,y = 140,w = 15,h = 580},{time = 100,x = 303,w = 30}}},
			{id = "keybeam14",timer = 1414,loop = 100,offset = 50,dst = {{time = 0,x = 382,y = 140,w = 15,h = 580},{time = 100,x = 375,w = 30}}},
			{id = "keybeam16",timer = 1416,loop = 100,offset = 50,dst = {{time = 0,x = 422,y = 140,w = 15,h = 580},{time = 100,x = 415,w = 30}}},
			{id = "keybeam19",timer = 1419,loop = 100,offset = 50,dst = {{time = 0,x = 494,y = 140,w = 15,h = 580},{time = 100,x = 487,w = 30}}},
			{id = "keybeam21",timer = 1421,loop = 100,offset = 50,dst = {{time = 0,x = 534,y = 140,w = 15,h = 580},{time = 100,x = 527,w = 30}}},
			{id = "keybeam23",timer = 1423,loop = 100,offset = 50,dst = {{time = 0,x = 574,y = 140,w = 15,h = 580},{time = 100,x = 567,w = 30}}},
			{id = "keybeamsu",timer = 1425,loop = 100,offset = 50,dst = {{time = 0,x = 52,y = 140,w = 25,h = 580},{time = 100,x = 40,w = 50}}},
			{id = "keybeamsd",timer = 1426,loop = 100,offset = 50,dst = {{time = 0,x = 52,y = 140,w = 25,h = 580},{time = 100,x = 40,w = 50}}},
			
			{id = 15, offset = 50, dst = {
				{x = 40, y = 137, w = 578, h = 6}
			}},
			{id = "notes"},
			
			{id = 110,timer = 51,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 26,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 111,timer = 52,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 46,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 112,timer = 53,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 66,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 113,timer = 54,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 86,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 114,timer = 55,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 106,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 115,timer = 56,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 138,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 116,timer = 57,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 158,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 117,timer = 58,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 178,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 118,timer = 59,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 198,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 119,timer = 1010,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 218,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 120,timer = 1011,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 238,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 121,timer = 1012,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 258,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 122,timer = 1013,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 290,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 123,timer = 1014,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 310,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 124,timer = 1015,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 330,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 125,timer = 1016,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 350,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 126,timer = 1017,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 370,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 127,timer = 1018,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 402,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 128,timer = 1019,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 422,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 129,timer = 1020,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 442,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 130,timer = 1021,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 462,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 131,timer = 1022,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 482,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 132,timer = 1023,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 502,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 133,timer = 1024,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 522,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 134,timer = 1025,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = -15,y = 28,w = 180,h = 192},{time = 160}}},
			{id = 135,timer = 1026,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = -15,y = 28,w = 180,h = 192},{time = 160}}},
			
			{id = "hold-1",timer = 71,blend = 2,offset = 50,dst = {{time = 0,x = 26,y = 28,w = 180,h = 192}}},
			{id = "hold-2",timer = 72,blend = 2,offset = 50,dst = {{time = 0,x = 46,y = 28,w = 180,h = 192}}},
			{id = "hold-3",timer = 73,blend = 2,offset = 50,dst = {{time = 0,x = 66,y = 28,w = 180,h = 192}}},
			{id = "hold-4",timer = 74,blend = 2,offset = 50,dst = {{time = 0,x = 86,y = 28,w = 180,h = 192}}},
			{id = "hold-5",timer = 75,blend = 2,offset = 50,dst = {{time = 0,x = 106,y = 28,w = 180,h = 192}}},
			{id = "hold-6",timer = 76,blend = 2,offset = 50,dst = {{time = 0,x = 138,y = 28,w = 180,h = 192}}},
			{id = "hold-7",timer = 77,blend = 2,offset = 50,dst = {{time = 0,x = 158,y = 28,w = 180,h = 192}}},
			{id = "hold-8",timer = 78,blend = 2,offset = 50,dst = {{time = 0,x = 178,y = 28,w = 180,h = 192}}},
			{id = "hold-9",timer = 79,blend = 2,offset = 50,dst = {{time = 0,x = 198,y = 28,w = 180,h = 192}}},
			{id = "hold-10",timer = 1210,blend = 2,offset = 50,dst = {{time = 0,x = 218,y = 28,w = 180,h = 192}}},
			{id = "hold-11",timer = 1211,blend = 2,offset = 50,dst = {{time = 0,x = 238,y = 28,w = 180,h = 192}}},
			{id = "hold-12",timer = 1212,blend = 2,offset = 50,dst = {{time = 0,x = 258,y = 28,w = 180,h = 192}}},
			{id = "hold-13",timer = 1213,blend = 2,offset = 50,dst = {{time = 0,x = 290,y = 28,w = 180,h = 192}}},
			{id = "hold-14",timer = 1214,blend = 2,offset = 50,dst = {{time = 0,x = 310,y = 28,w = 180,h = 192}}},
			{id = "hold-15",timer = 1215,blend = 2,offset = 50,dst = {{time = 0,x = 330,y = 28,w = 180,h = 192}}},
			{id = "hold-16",timer = 1216,blend = 2,offset = 50,dst = {{time = 0,x = 350,y = 28,w = 180,h = 192}}},
			{id = "hold-17",timer = 1217,blend = 2,offset = 50,dst = {{time = 0,x = 370,y = 28,w = 180,h = 192}}},
			{id = "hold-18",timer = 1218,blend = 2,offset = 50,dst = {{time = 0,x = 402,y = 28,w = 180,h = 192}}},
			{id = "hold-19",timer = 1219,blend = 2,offset = 50,dst = {{time = 0,x = 422,y = 28,w = 180,h = 192}}},
			{id = "hold-20",timer = 1220,blend = 2,offset = 50,dst = {{time = 0,x = 442,y = 28,w = 180,h = 192}}},
			{id = "hold-21",timer = 1221,blend = 2,offset = 50,dst = {{time = 0,x = 462,y = 28,w = 180,h = 192}}},
			{id = "hold-22",timer = 1222,blend = 2,offset = 50,dst = {{time = 0,x = 482,y = 28,w = 180,h = 192}}},
			{id = "hold-23",timer = 1223,blend = 2,offset = 50,dst = {{time = 0,x = 502,y = 28,w = 180,h = 192}}},
			{id = "hold-24",timer = 1224,blend = 2,offset = 50,dst = {{time = 0,x = 522,y = 28,w = 180,h = 192}}},
			{id = "hold-su",timer = 1225,blend = 2,offset = 50,dst = {{time = 0,x = -15,y = 28,w = 180,h = 192}}},
			{id = "hold-sd",timer = 1226,blend = 2,offset = 50,dst = {{time = 0,x = -15,y = 28,w = 180,h = 192}}}
		})
	end
	if is_separate_lane() then
		append_all(skin.destination, {
			{id = "keybeam1",timer = 101,loop = 100,offset = 50,dst = {{time = 0,x = 100,y = 140,w = 20,h = 580},{time = 100,x = 90,w = 40}}},
			{id = "keybeam2",timer = 102,loop = 100,offset = 50,dst = {{time = 0,x = 138,y = 140,w = 16,h = 580},{time = 100,x = 130,w = 32}}},
			{id = "keybeam3",timer = 103,loop = 100,offset = 50,dst = {{time = 0,x = 172,y = 140,w = 20,h = 580},{time = 100,x = 162,w = 40}}},
			{id = "keybeam4",timer = 104,loop = 100,offset = 50,dst = {{time = 0,x = 210,y = 140,w = 16,h = 580},{time = 100,x = 202,w = 32}}},
			{id = "keybeam5",timer = 105,loop = 100,offset = 50,dst = {{time = 0,x = 244,y = 140,w = 20,h = 580},{time = 100,x = 234,w = 40}}},
			{id = "keybeam6",timer = 106,loop = 100,offset = 50,dst = {{time = 0,x = 284,y = 140,w = 20,h = 580},{time = 100,x = 274,w = 40}}},
			{id = "keybeam7",timer = 107,loop = 100,offset = 50,dst = {{time = 0,x = 322,y = 140,w = 16,h = 580},{time = 100,x = 314,w = 32}}},
			{id = "keybeam8",timer = 108,loop = 100,offset = 50,dst = {{time = 0,x = 356,y = 140,w = 20,h = 580},{time = 100,x = 346,w = 40}}},
			{id = "keybeam9",timer = 109,loop = 100,offset = 50,dst = {{time = 0,x = 394,y = 140,w = 16,h = 580},{time = 100,x = 386,w = 32}}},
			{id = "keybeam10",timer = 1410,loop = 100,offset = 50,dst = {{time = 0,x = 428,y = 140,w = 20,h = 580},{time = 100,x = 418,w = 40}}},
			{id = "keybeam11",timer = 1411,loop = 100,offset = 50,dst = {{time = 0,x = 466,y = 140,w = 16,h = 580},{time = 100,x = 458,w = 32}}},
			{id = "keybeam12",timer = 1412,loop = 100,offset = 50,dst = {{time = 0,x = 500,y = 140,w = 20,h = 580},{time = 100,x = 490,w = 40}}},
			{id = "keybeam13",timer = 1413,loop = 100,offset = 50,dst = {{time = 0,x = 540,y = 140,w = 20,h = 580},{time = 100,x = 530,w = 40}}},
			{id = "keybeam14",timer = 1414,loop = 100,offset = 50,dst = {{time = 0,x = 578,y = 140,w = 16,h = 580},{time = 100,x = 570,w = 32}}},
			{id = "keybeam15",timer = 1415,loop = 100,offset = 50,dst = {{time = 0,x = 612,y = 140,w = 20,h = 580},{time = 100,x = 602,w = 40}}},
			{id = "keybeam16",timer = 1416,loop = 100,offset = 50,dst = {{time = 0,x = 650,y = 140,w = 16,h = 580},{time = 100,x = 642,w = 32}}},
			{id = "keybeam17",timer = 1417,loop = 100,offset = 50,dst = {{time = 0,x = 684,y = 140,w = 20,h = 580},{time = 100,x = 674,w = 40}}},
			{id = "keybeam18",timer = 1418,loop = 100,offset = 50,dst = {{time = 0,x = 724,y = 140,w = 20,h = 580},{time = 100,x = 714,w = 40}}},
			{id = "keybeam19",timer = 1419,loop = 100,offset = 50,dst = {{time = 0,x = 762,y = 140,w = 16,h = 580},{time = 100,x = 754,w = 32}}},
			{id = "keybeam20",timer = 1420,loop = 100,offset = 50,dst = {{time = 0,x = 796,y = 140,w = 20,h = 580},{time = 100,x = 786,w = 40}}},
			{id = "keybeam21",timer = 1421,loop = 100,offset = 50,dst = {{time = 0,x = 834,y = 140,w = 16,h = 580},{time = 100,x = 826,w = 32}}},
			{id = "keybeam22",timer = 1422,loop = 100,offset = 50,dst = {{time = 0,x = 868,y = 140,w = 20,h = 580},{time = 100,x = 858,w = 40}}},
			{id = "keybeam23",timer = 1423,loop = 100,offset = 50,dst = {{time = 0,x = 906,y = 140,w = 16,h = 580},{time = 100,x = 898,w = 32}}},
			{id = "keybeam24",timer = 1424,loop = 100,offset = 50,dst = {{time = 0,x = 940,y = 140,w = 20,h = 580},{time = 100,x = 930,w = 40}}},
			{id = "keybeamsu",timer = 1425,loop = 100,offset = 50,dst = {{time = 0,x = 37,y = 140,w = 35,h = 580},{time = 100,x = 20,w = 70}}},
			{id = "keybeamsd",timer = 1426,loop = 100,offset = 50,dst = {{time = 0,x = 37,y = 140,w = 35,h = 580},{time = 100,x = 20,w = 70}}},
			
			{id = 15, offset = 50, dst = {
				{x = 20, y = 137, w = 950, h = 6}
			}},
			{id = "notes"},
			
			{id = 110,timer = 51,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = -26,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 111,timer = 52,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 14,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 112,timer = 53,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 46,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 113,timer = 54,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 86,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 114,timer = 55,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 118,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 115,timer = 56,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 158,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 116,timer = 57,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 198,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 117,timer = 58,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 230,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 118,timer = 59,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 270,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 119,timer = 1010,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 302,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 120,timer = 1011,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 342,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 121,timer = 1012,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 374,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 122,timer = 1013,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 414,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 123,timer = 1014,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 454,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 124,timer = 1015,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 486,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 125,timer = 1016,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 526,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 126,timer = 1017,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 558,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 127,timer = 1018,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 598,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 128,timer = 1019,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 638,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 129,timer = 1020,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 670,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 130,timer = 1021,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 710,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 131,timer = 1022,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 742,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 132,timer = 1023,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 782,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 133,timer = 1024,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = 814,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 134,timer = 1025,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = -76,y = -62,w = 322,h = 344},{time = 160}}},
			{id = 135,timer = 1026,loop = -1,blend = 2,offset = 50,dst = {{time = 0,x = -76,y = -62,w = 322,h = 344},{time = 160}}},
			
			{id = "hold-1",timer = 71,blend = 2,offset = 50,dst = {{time = 0,x = -26,y = -62,w = 322,h = 344}}},
			{id = "hold-2",timer = 72,blend = 2,offset = 50,dst = {{time = 0,x = 14,y = -62,w = 322,h = 344}}},
			{id = "hold-3",timer = 73,blend = 2,offset = 50,dst = {{time = 0,x = 46,y = -62,w = 322,h = 344}}},
			{id = "hold-4",timer = 74,blend = 2,offset = 50,dst = {{time = 0,x = 86,y = -62,w = 322,h = 344}}},
			{id = "hold-5",timer = 75,blend = 2,offset = 50,dst = {{time = 0,x = 118,y = -62,w = 322,h = 344}}},
			{id = "hold-6",timer = 76,blend = 2,offset = 50,dst = {{time = 0,x = 158,y = -62,w = 322,h = 344}}},
			{id = "hold-7",timer = 77,blend = 2,offset = 50,dst = {{time = 0,x = 198,y = -62,w = 322,h = 344}}},
			{id = "hold-8",timer = 78,blend = 2,offset = 50,dst = {{time = 0,x = 230,y = -62,w = 322,h = 344}}},
			{id = "hold-9",timer = 79,blend = 2,offset = 50,dst = {{time = 0,x = 270,y = -62,w = 322,h = 344}}},
			{id = "hold-10",timer = 1210,blend = 2,offset = 50,dst = {{time = 0,x = 302,y = -62,w = 322,h = 344}}},
			{id = "hold-11",timer = 1211,blend = 2,offset = 50,dst = {{time = 0,x = 342,y = -62,w = 322,h = 344}}},
			{id = "hold-12",timer = 1212,blend = 2,offset = 50,dst = {{time = 0,x = 374,y = -62,w = 322,h = 344}}},
			{id = "hold-13",timer = 1213,blend = 2,offset = 50,dst = {{time = 0,x = 414,y = -62,w = 322,h = 344}}},
			{id = "hold-14",timer = 1214,blend = 2,offset = 50,dst = {{time = 0,x = 454,y = -62,w = 322,h = 344}}},
			{id = "hold-15",timer = 1215,blend = 2,offset = 50,dst = {{time = 0,x = 486,y = -62,w = 322,h = 344}}},
			{id = "hold-16",timer = 1216,blend = 2,offset = 50,dst = {{time = 0,x = 526,y = -62,w = 322,h = 344}}},
			{id = "hold-17",timer = 1217,blend = 2,offset = 50,dst = {{time = 0,x = 558,y = -62,w = 322,h = 344}}},
			{id = "hold-18",timer = 1218,blend = 2,offset = 50,dst = {{time = 0,x = 598,y = -62,w = 322,h = 344}}},
			{id = "hold-19",timer = 1219,blend = 2,offset = 50,dst = {{time = 0,x = 638,y = -62,w = 322,h = 344}}},
			{id = "hold-20",timer = 1220,blend = 2,offset = 50,dst = {{time = 0,x = 670,y = -62,w = 322,h = 344}}},
			{id = "hold-21",timer = 1221,blend = 2,offset = 50,dst = {{time = 0,x = 710,y = -62,w = 322,h = 344}}},
			{id = "hold-22",timer = 1222,blend = 2,offset = 50,dst = {{time = 0,x = 742,y = -62,w = 322,h = 344}}},
			{id = "hold-23",timer = 1223,blend = 2,offset = 50,dst = {{time = 0,x = 782,y = -62,w = 322,h = 344}}},
			{id = "hold-24",timer = 1224,blend = 2,offset = 50,dst = {{time = 0,x = 814,y = -62,w = 322,h = 344}}},
			{id = "hold-su",timer = 1225,blend = 2,offset = 50,dst = {{time = 0,x = -76,y = -62,w = 322,h = 344}}},
			{id = "hold-sd",timer = 1226,blend = 2,offset = 50,dst = {{time = 0,x = -76,y = -62,w = 322,h = 344}}}
		})
	end
	append_all(skin.destination, {
		{id = 2010},
		{id = "judge-early", loop = -1, timer = 46 ,op = {911,1242},offset = 50, dst = {
			{time = 0, x = 320, y = 290, w = 50, h = 20},
			{time = 500}
		}},
		{id = "judge-late", loop = -1, timer = 46 ,op = {911,1243},offset = 50, dst = {
			{time = 0, x = 320, y = 290, w = 50, h = 20},
			{time = 500}
		}},
		{id = "judgems-1pp", loop = -1, timer = 46 ,op = {912,241},offset = 50, dst = {
			{time = 0, x = 320, y = 290, w = 10, h = 20},
			{time = 500}
		}},
		{id = "judgems-1pg", loop = -1, timer = 46 ,op = {912,-241},offset = 50, dst = {
			{time = 0, x = 320, y = 290, w = 10, h = 20},
			{time = 500}
		}},
		{id = "hidden-cover", dst = {
			{x = geometry.lanes_x, y = -440, w = geometry.lanes_w, h = 580}
		}},
		{id = 1060, dst = {
			{x = geometry.lanes_x, y = 720, w = geometry.lanes_w, h = 580}
		}},
		{id = 2001, dst = {
			{time = 0, x = 20, y = 30, w = 390, h = 30}
		}},
		{id = 410, dst = {
			{time = 0, x = 314, y = 60, w = 24, h = 24}
		}},
		{id = 411, dst = {
			{time = 0, x = 386, y = 60, w = 18, h = 18}
		}}
	})
	append_all(skin.destination, {
		{id = 2002, dst = {
			{time = 0, x = geometry.bga_x, y = geometry.bga_y, w = geometry.bga_w, h = geometry.bga_h}
		}},
		{id = 1000, dst = {
			{time = 0, x = geometry.title_x, y = 674, w = 24, h = 24},
			{time = 1000, a = 0},
			{time = 2000, a = 255}
		}},
		{id = 11, op = {901},dst = {
			{x = 1132, y = 50, w = 120, h = 360}
		}},
		{id = "graph-now", op = {901},dst = {
			{x = 1133, y = 50, w = 38, h = 360}
		}},
		{id = "graph-best", op = {901},dst = {
			{x = 1173, y = 50, w = 38, h = 360}
		}},
		{id = "graph-target", op = {901},dst = {
			{x = 1213, y = 50, w = 38, h = 360}
		}},
		{id = 12, op = {901},dst = {
			{x = 1132, y = 50, w = 120, h = 360}
		}},
		{id = 420, op = {901},dst = {
			{x = 1020, y = 230, w = 12, h = 18}
		}},
		{id = 421, op = {901},dst = {
			{x = 1068, y = 230, w = 8, h = 12}
		}},
		{id = 422, op = {901},dst = {
			{x = 1020, y = 210, w = 12, h = 18}
		}},
		{id = 423, op = {901},dst = {
			{x = 1020, y = 190, w = 12, h = 18}
		}},
		{id = 424, op = {901},dst = {
			{x = 1020, y = 170, w = 12, h = 18}
		}},
		{id = 1050, blend = 2, dst = {
			{x = 2, y = 660, w = 16, h = 20}
		}},
		{id = 1051, blend = 2, timer = 143,dst = {
			{x = 2, y = 660, w = 16, h = 20}
		}},

		{id = 431 ,op = {901,906},dst = {
			{x = 1000, y = 140, w = 12, h = 18}
		}},
		{id = 432 ,op = {901,906},dst = {
			{x = 1060, y = 140, w = 12, h = 18}
		}},
		{id = 434 ,op = {901,906},dst = {
			{x = 1000, y = 122, w = 12, h = 18}
		}},
		{id = 435 ,op = {901,906},dst = {
			{x = 1060, y = 122, w = 12, h = 18}
		}},
		{id = 437 ,op = {901,906},dst = {
			{x = 1000, y = 104, w = 12, h = 18}
		}},
		{id = 438 ,op = {901,906},dst = {
			{x = 1060, y = 104, w = 12, h = 18}
		}},
		{id = 440 ,op = {901,906},dst = {
			{x = 1000, y = 86, w = 12, h = 18}
		}},
		{id = 441 ,op = {901,906},dst = {
			{x = 1060, y = 86, w = 12, h = 18}
		}},
		{id = 443 ,op = {901,906},dst = {
			{x = 1000, y = 68, w = 12, h = 18}
		}},
		{id = 444 ,op = {901,906},dst = {
			{x = 1060, y = 68, w = 12, h = 18}
		}},
		{id = 446 ,op = {901,906},dst = {
			{x = 1000, y = 50, w = 12, h = 18}
		}},
		{id = 447 ,op = {901,906},dst = {
			{x = 1060, y = 50, w = 12, h = 18}
		}},

		{id = 431 ,op = {900,906},dst = {
			{x = 1000, y = 140, w = 12, h = 18}
		}},
		{id = 432 ,op = {900,906},dst = {
			{x = 1060, y = 140, w = 12, h = 18}
		}},
		{id = 434 ,op = {900,906},dst = {
			{x = 1000, y = 122, w = 12, h = 18}
		}},
		{id = 435 ,op = {900,906},dst = {
			{x = 1060, y = 122, w = 12, h = 18}
		}},
		{id = 437 ,op = {900,906},dst = {
			{x = 1000, y = 104, w = 12, h = 18}
		}},
		{id = 438 ,op = {900,906},dst = {
			{x = 1060, y = 104, w = 12, h = 18}
		}},
		{id = 440 ,op = {900,906},dst = {
			{x = 1000, y = 86, w = 12, h = 18}
		}},
		{id = 441 ,op = {900,906},dst = {
			{x = 1060, y = 86, w = 12, h = 18}
		}},
		{id = 443 ,op = {900,906},dst = {
			{x = 1000, y = 68, w = 12, h = 18}
		}},
		{id = 444 ,op = {900,906},dst = {
			{x = 1060, y = 68, w = 12, h = 18}
		}},
		{id = 446 ,op = {900,906},dst = {
			{x = 1000, y = 50, w = 12, h = 18}
		}},
		{id = 447 ,op = {900,906},dst = {
			{x = 1060, y = 50, w = 12, h = 18}
		}},

		{id = 450, offset = 51, op = {270},dst = {
			{time = 0, x = 120, y = 720, w = 10, h = 15}
		}},
		{id = 451, offset = 51, op = {270},dst = {
			{time = 0, x = 310, y = 720, w = 10, h = 15}
		}},

		{id = "load-progress", loop = 0, op = {80}, dst = {
			{time = 0, x = geometry.lanes_x, y = 440, w = geometry.lanes_w, h = 4},
			{time = 500, a = 192, r = 0},
			{time = 1000, a = 128, r = 255, g = 0},
			{time = 1500, a = 192, g = 255, b = 0},
			{time = 2000, a = 255, b = 255}
		}},

		{id = 6, loop = -1, timer = 40, dst = {
			{time = 0, x = geometry.ready_x, y = 250, w = 350, h = 60, a = 0},
			{time = 750, y = 300, a = 255},
			{time = 1000}
		}},

		{id = "close2", loop = 700, timer = 3, dst = {
			{time = 0, x = 0, y = -360, w = 1280, h = 360},
			{time = 500, y = 0},
			{time = 600, y = -40},
			{time = 700, y = 0}
		}},
		{id = "close1", loop = 700, timer = 3, dst = {
			{time = 0, x = 0, y = 720, w = 1280, h = 360},
			{time = 500, y = 360},
			{time = 600, y = 400},
			{time = 700, y = 360}
		}},

		{id = 7, loop = 500, timer = 2, dst = {
			{time = 0, x = 0, y = 0, w = 1280, h = 720, a = 0},
			{time = 500, a = 255}
		}}
	})
	return skin
end

return {
	header = header,
	main = main
}
