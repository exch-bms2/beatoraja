
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

local function timer_key_bomb(index)
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

local function timer_key_on(index)
	if index <= 9 then
		return 100 + index
	else
		return 1400 + index
	end
end

local function timer_key_off(index)
	if index <= 9 then
		return 120 + index
	else
		return 1600 + index
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
	{name = "Background", path = "play/background/*.png"},
	{name = "Theme", path = "keyboard/*.png"},
	{name = "Laser", path = "play/laser/*.png"},
	{name = "Lanecover", path = "play/lanecover/*.png"},
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

local key_wbs = { 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0 }
local function get_key_wbs(i)
	local k = (i - 1) % 26
	if k >= 24 then
		return 2
	end
	return key_wbs[k % 12 + 1]
end
local function get_key_wbss(i)
	local k = (i - 1) % 26
	if k == 24 then
		return 2
	elseif k == 25 then
		return 3
	end
	return key_wbs[k % 12 + 1]
end

local keybeam_order = { 1, 3, 5, 6, 8, 10, 12, 13, 15, 17, 18, 20, 22, 24, 2, 4, 7, 9, 11, 14, 16, 19, 21, 23, 25, 26 }

local function main()
	local play_parts = require("play_parts")

	local skin = {}
	for k, v in pairs(header) do
		skin[k] = v
	end

	local geometry = {}

	if is_half_lane() then
		geometry.lanes_x = 56
		geometry.lanes_w = 560
		geometry.lane_w_width = 36
		geometry.lane_b_width = 0
		geometry.lane_s_width = 56
		geometry.note_w_width = 36
		geometry.note_b_width = 36
		geometry.note_s_width = 56
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
		geometry.lane_w_width = 32
		geometry.lane_b_width = 8
		geometry.lane_s_width = 50
		geometry.note_w_width = 32
		geometry.note_b_width = 30
		geometry.note_s_width = 50
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
		geometry.lane_w_width = 40
		geometry.lane_b_width = 32
		geometry.lane_s_width = 70
		geometry.note_w_width = 40
		geometry.note_b_width = 32
		geometry.note_s_width = 70
		geometry.title_align = 1
		geometry.judge_x = 375
		geometry.ready_x = 320
		geometry.title_x = 495
		geometry.bga_x = 1000
		geometry.bga_y = 440
		geometry.bga_w = 256
		geometry.bga_h = 256
	end
	do
		geometry.notes_x = {}
		geometry.notes_w = {}
		geometry.lanes_center_x = {}
		local x = geometry.lanes_x + geometry.lane_s_width
		local adjust_w = (geometry.note_w_width - geometry.lane_w_width) / 2
		local adjust_b = (geometry.note_b_width - geometry.lane_b_width) / 2
		for i = 1, 24 do
			if get_key_wbs(i) == 0 then
				geometry.notes_x[i] = x - adjust_w
				geometry.notes_w[i] = geometry.note_w_width
				x = x + geometry.lane_w_width
			else
				geometry.notes_x[i] = x - adjust_b
				geometry.notes_w[i] = geometry.note_b_width
				x = x + geometry.lane_b_width
			end
		end
		geometry.notes_x[25] = geometry.lanes_x
		geometry.notes_w[25] = geometry.lane_s_width
		geometry.notes_x[26] = geometry.lanes_x
		geometry.notes_w[26] = geometry.lane_s_width
		for i = 1, 26 do
			geometry.lanes_center_x[i] = geometry.notes_x[i] + geometry.notes_w[i] / 2
		end
	end

	skin.source = {
		{id = 0, path = "system.png"},
		{id = 1, path = "play/background/*.png"},
		{id = 2, path = "playbg.png"},
		{id = 3, path = "gauge.png"},
		{id = 4, path = "judge.png"},
		{id = 5, path = "number.png"},
		{id = 6, path = "play/laser/*.png"},
		{id = 7, path = "keyboard/*.png"},
		{id = 8, path = "close.png"},
		{id = 9, path = "scoregraph.png"},
		{id = 10, path = "bomb.png"},
		{id = 11, path = "ready.png"},
		{id = 12, path = "play/lanecover/*.png"},
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
		table.insert(skin.image, bomb_image(i, "bomb1-", 0, timer_key_bomb))
		table.insert(skin.image, bomb_image(i, "bomb2-", 576, timer_key_bomb))
		table.insert(skin.image, bomb_image(i, "bomb3-", 192, timer_key_bomb))
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
	do
		local wbs = { "w", "b", "s" }
		for i = 1, 26 do
			local name = i
			if i == 25 then
				name = "su"
			elseif i == 26 then
				name = "sd"
			end
			local img_suffix = wbs[get_key_wbs(i) + 1]
			table.insert(skin.imageset, {
				id = "keybeam"..name,
				ref = value_judge(i),
				images = { "keybeam-"..img_suffix, "keybeam-"..img_suffix.."-pg" }
			})
		end
	end
	for i = 1, 26 do
		local name = i
		if i == 25 then
			name = "su"
		elseif i == 26 then
			name = "sd"
		end
		table.insert(skin.imageset, {
			id = i + 109,
			ref = value_judge(i),
			images = { "bomb1-"..name, "bomb2-"..name, "bomb1-"..name, "bomb3-"..name }
		})
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
	append_all(skin.value, play_parts.judge_count_sources("judge-count-", 5))
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
	skin.note.dst = {}
	for i = 1, 26 do
		skin.note.dst[i] = {
			x = geometry.notes_x[i],
			y = 140,
			w = geometry.notes_w[i],
			h = 580
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
	for _, i in ipairs(keybeam_order) do
		name = i
		if i == 25 then
			name = "su"
		elseif i == 26 then
			name = "sd"
		end
		table.insert(skin.destination, {
			id = "keybeam"..name,
			timer = timer_key_on(i),
			loop = 100,
			offset = 50,
			dst = {
				{ time = 0, x = geometry.notes_x[i] + geometry.notes_w[i] / 4, y = 140, w = geometry.notes_w[i] / 2, h = 580 },
				{ time = 100, x = geometry.notes_x[i], w = geometry.notes_w[i] }
			}
		})
	end
	table.insert(skin.destination, {id = 15, offset = 50, dst = { {x = geometry.lanes_x, y = 137, w = geometry.lanes_w, h = 6} }})
	table.insert(skin.destination, {id = "notes"})
	for i = 1, 26 do
		table.insert(skin.destination, {
			id = 109 + i,
			timer = timer_key_bomb(i),
			blend = 2,
			loop = -1,
			offset = 50,
			dst = {
				{ time = 0, x = geometry.lanes_center_x[i] - 80, y = 28, w = 180, h = 192 },
				{ time = 160 }
			}
		})
	end
	for i = 1, 26 do
		name = i
		if i == 25 then
			name = "su"
		elseif i == 26 then
			name = "sd"
		end
		table.insert(skin.destination, {
			id = "hold-"..name,
			timer = timer_key_hold(i),
			blend = 2,
			offset = 50,
			dst = {
				{ time = 0, x = geometry.lanes_center_x[i] - 80, y = 28, w = 180, h = 192 }
			}
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
	})
	append_all(skin.destination, play_parts.judge_count_destinations("judge-count-", 1000, 50, {906}, -1))
	append_all(skin.destination, {
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
