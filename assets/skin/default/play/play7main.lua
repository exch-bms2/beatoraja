
local function append_all(list, list1)
	for i, v in ipairs(list1) do
		table.insert(list, v)
	end
end

local property = {
	{name = "Play Side", item = {
		{name = "1P", op = 920},
		{name = "2P", op = 921}
	}},
	{name = "Scratch Side", item = {
		{name = "Left", op = 922},
		{name = "Right", op = 923}
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
local function is_left_side()
	return skin_config.option["Play Side"] == 920
end
local function is_right_side()
	return skin_config.option["Play Side"] == 921
end
local function is_left_scratch()
	return skin_config.option["Scratch Side"] == 922
end
local function is_right_scratch()
	return skin_config.option["Scratch Side"] == 923
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
	if index == 8 then
		return 50
	else
		return 50 + index
	end
end

local function timer_key_hold(index)
	if index == 8 then
		return 70
	else
		return 70 + index
	end
end

local function timer_key_on(index)
	if index == 8 then
		return 100
	else
		return 100 + index
	end
end

local function timer_key_off(index)
	if index == 8 then
		return 120
	else
		return 120 + index
	end
end

local function value_judge(index)
	if index == 8 then
		return 500
	else
		return 500 + index
	end
end

local filepath = {
	{name = "Background", path = "background/*.png"},
	{name = "Note", path = "notes/*.png"},
	{name = "Bomb", path = "bomb/*.png"},
	{name = "Gauge", path = "gauge/*.png"},
	{name = "Judge", path = "judge/*.png"},
	{name = "Laser", path = "laser/*.png"},
	{name = "Lanecover", path = "lanecover/*.png"},
}

local offset = {
	{name = "Laser Offset", id = 40, x = false, y = false, w = false, h = true, r = false, a = true},
	{name = "Bomb Offset", id = 41, x = true, y = true, w = true, h = true, r = false, a = true},
	{name = "Judge Count Offset", id = 42, x = true, y = true, w = false, h = false, r = false, a = true},
	{name = "BGA Offset", id = 43, x = true, y = true, w = true, h = true, r = false, a = true},
	{name = "Lane Background Offset", id = 44, x = false, y = false, w = false, h = false, r = false, a = true},
}

local header = {
	type = 0,
	name = "beatoraja default (lua)",
	w = 1280,
	h = 720,
	playstart = 1000,
	scene = 3600000,
	input = 500,
	close = 1500,
	fadeout = 1000,
	property = property,
	filepath = filepath,
	offset = offset
}

local key_wbs = { 0, 1, 0, 1, 0, 1, 0, 2 }
local function get_key_wbs(i)
	return key_wbs[(i - 1) % 8 + 1]
end
local function get_key_wbss(i)
	return key_wbs[(i - 1) % 8 + 1]
end

local keybeam_order = { 1, 2, 3, 4, 5, 6, 7, 8}

local function main()
	local play_parts = require("play_parts")

	local skin = {}
	for k, v in pairs(header) do
		skin[k] = v
	end

	local geometry = {}

	if is_left_side() then
		geometry.lanes_x = 20
		geometry.lanes_w = 390
		geometry.lane_w_width = 50
		geometry.lane_b_width = 40
		geometry.lane_s_width = 70
		geometry.note_w_width = 50
		geometry.note_b_width = 40
		geometry.note_s_width = 70
		geometry.title_align = 0
		geometry.judge_x = 115
		geometry.judgedetail_x = 200
		geometry.judgedetail_y = 290
		geometry.judgecount_x = 476
		geometry.judgecount_y = 50
		geometry.ready_x = 40
		geometry.title_x = 450
		geometry.gauge_x = 20
		geometry.gauge_w = 390
		geometry.gaugevalue_x = 314
		geometry.bga_x = 440
		geometry.bga_y = 50
		geometry.bga_w = 800
		geometry.bga_h = 650
		geometry.judgegraph_x = 740
		geometry.judgegraph_y = 100
		geometry.judgegraph_w = 450
		geometry.judgegraph_h = 100
		geometry.timing_x = 740
		geometry.timing_y = 50
		geometry.timing_w = 450
		geometry.timing_h = 50
		geometry.progress_x = 2
		geometry.progress_y = 140
		geometry.progress_w = 16
		geometry.progress_h = 540
	end
	if is_right_side() then
		geometry.lanes_x = 870
		geometry.lanes_w = 390
		geometry.lane_w_width = 50
		geometry.lane_b_width = 40
		geometry.lane_s_width = 70
		geometry.note_w_width = 50
		geometry.note_b_width = 40
		geometry.note_s_width = 70
		geometry.title_align = 2
		geometry.judge_x = 965
		geometry.judgedetail_x = 1050
		geometry.judgedetail_y = 290
		geometry.judgecount_x = 720
		geometry.judgecount_y = 50
		geometry.ready_x = 890
		geometry.title_x = 840
		geometry.gauge_x = 1260
		geometry.gauge_w = -390
		geometry.gaugevalue_x = 870
		geometry.bga_x = 40
		geometry.bga_y = 50
		geometry.bga_w = 800
		geometry.bga_h = 650
		geometry.judgegraph_x = 90
		geometry.judgegraph_y = 100
		geometry.judgegraph_w = 450
		geometry.judgegraph_h = 100
		geometry.timing_x = 90
		geometry.timing_y = 50
		geometry.timing_w = 450
		geometry.timing_h = 50
		geometry.progress_x = 1262
		geometry.progress_y = 140
		geometry.progress_w = 16
		geometry.progress_h = 540
	end
	if is_score_graph_enabled() then
    	if is_left_side() then
    		geometry.graph_x = geometry.lanes_x + geometry.lanes_w
    		geometry.title_x = geometry.title_x + 90
    		geometry.bga_x = geometry.bga_x + 90
    		geometry.bga_w = geometry.bga_w - 90
    		geometry.judgecount_x = geometry.judgecount_x + 90
    	else
    		geometry.graph_x = geometry.lanes_x - 90
    		geometry.title_x = geometry.title_x - 90
    		geometry.bga_w = geometry.bga_w - 90
    		geometry.judgecount_x = geometry.judgecount_x - 90
    	end
		geometry.graph_y = 220
		geometry.graph_w = 90
		geometry.graph_h = 480
	else
		geometry.graph_x = 0
		geometry.graph_y = 0
		geometry.graph_w = 0
		geometry.graph_h = 0
	end
	do
		geometry.notes_x = {}
		geometry.notes_w = {}
		geometry.lanes_center_x = {}
		local x = geometry.lanes_x
		if is_left_scratch() then
			geometry.lanebg_x = geometry.lanes_x
			geometry.lanebg_w = geometry.lanes_w
			x = x + geometry.lane_s_width;
			geometry.notes_x[8] = geometry.lanes_x
			geometry.notes_w[8] = geometry.lane_s_width
		end
		local adjust_w = (geometry.note_w_width - geometry.lane_w_width) / 2
		local adjust_b = (geometry.note_b_width - geometry.lane_b_width) / 2
		for i = 1, 7 do
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
		if is_right_scratch() then
			geometry.lanebg_x = geometry.lanes_x + geometry.lanes_w
			geometry.lanebg_w = -geometry.lanes_w
			geometry.notes_x[8] = x
			geometry.notes_w[8] = geometry.lane_s_width
		end
		for i = 1, 8 do
			geometry.lanes_center_x[i] = geometry.notes_x[i] + geometry.notes_w[i] / 2
		end
	end

	skin.source = {
		{id = 0, path = "../system.png"},
		{id = "bg", path = "background/*.png"},
		{id = 2, path = "../playbg.png"},
		{id = 3, path = "gauge/*.png"},
		{id = 4, path = "judge/*.png"},
		{id = 5, path = "../number.png"},
		{id = 6, path = "laser/*.png"},
		{id = 7, path = "notes/*.png"},
		{id = 8, path = "../close.png"},
		{id = 9, path = "../scoregraph.png"},
		{id = 10, path = "bomb/*.png"},
		{id = 11, path = "../ready.png"},
		{id = 12, path = "lanecover/*.png"},
		{id = 13, path = "../judgedetail.png"}
	}
	skin.font = {
		{id = 0, path = "../VL-Gothic-Regular.ttf"}
	}

	skin.image = {
		{id = "background", src = "bg", x = 0, y = 0, w = 1280, h = 720},
		{id = 1, src = 2, x = 0, y = 0, w = 1280, h = 720},
		{id = "ready", src = 11, x = 0, y = 0, w = 216, h = 40},
		{id = 7, src = 0, x = 0, y = 0, w = 8, h = 8},
		{id = "close1", src = 8, x = 0, y = 500, w = 640, h = 240},
		{id = "close2", src = 8, x = 0, y = 740, w = 640, h = 240},
		{id = "lane-bg", src = 0, x = 30, y = 0, w = 390, h = 10},
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
		
		{id = "note-b", src = 7, x = 127, y = 5, w = 21, h = 12},
		{id = "note-w", src = 7, x = 99, y = 5, w = 27, h = 12},
		{id = "note-s", src = 7, x = 50, y = 5, w = 46, h = 12},
		
		{id = "lns-b", src = 7, x = 127, y = 57, w = 21, h = 13},
		{id = "lns-w", src = 7, x = 99, y = 57, w = 27, h = 13},
		{id = "lns-s", src = 7, x = 50, y = 57, w = 46, h = 12},

		{id = "lne-b", src = 7, x = 127, y = 43, w = 21, h = 13},
		{id = "lne-w", src = 7, x = 99, y = 43, w = 27, h = 13},
		{id = "lne-s", src = 7, x = 50, y = 43, w = 46, h = 12},
		
		{id = "lnb-b", src = 7, x = 127, y = 80, w = 21, h = 1},
		{id = "lnb-w", src = 7, x = 99, y = 80, w = 27, h = 1},
		{id = "lnb-s", src = 7, x = 50, y = 80, w = 46, h = 1},

		{id = "lna-b", src = 7, x = 127, y = 76, w = 21, h = 1},
		{id = "lna-w", src = 7, x = 99, y = 76, w = 27, h = 1},
		{id = "lna-s", src = 7, x = 50, y = 76, w = 46, h = 1},

		{id = "hcns-b", src = 7, x = 127, y = 108, w = 21, h = 13},
		{id = "hcns-w", src = 7, x = 99, y = 108, w = 27, h = 13},
		{id = "hcns-s", src = 7, x = 50, y = 108, w = 46, h = 12},

		{id = "hcne-b", src = 7, x = 127, y = 94, w = 21, h = 13},
		{id = "hcne-w", src = 7, x = 99, y = 94, w = 27, h = 13},
		{id = "hcne-s", src = 7, x = 50, y = 94, w = 46, h = 12},

		{id = "hcnb-b", src = 7, x = 127, y = 131, w = 21, h = 1},
		{id = "hcnb-w", src = 7, x = 99, y = 131, w = 27, h = 1},
		{id = "hcnb-s", src = 7, x = 50, y = 131, w = 46, h = 1},

		{id = "hcna-b", src = 7, x = 127, y = 127, w = 21, h = 1},
		{id = "hcna-w", src = 7, x = 99, y = 127, w = 27, h = 1},
		{id = "hcna-s", src = 7, x = 50, y = 127, w = 46, h = 1},
		
		{id = "hcnd-b", src = 7, x = 127, y = 128, w = 21, h = 1},
		{id = "hcnd-w", src = 7, x = 99, y = 128, w = 27, h = 1},
		{id = "hcnd-s", src = 7, x = 50, y = 128, w = 46, h = 1},

		{id = "hcnr-b", src = 7, x = 127, y = 129, w = 21, h = 1},
		{id = "hcnr-w", src = 7, x = 99, y = 129, w = 27, h = 1},
		{id = "hcnr-s", src = 7, x = 50, y = 129, w = 46, h = 1},
	
		{id = "mine-b", src = 7, x = 127, y = 23, w = 21, h = 8},
		{id = "mine-w", src = 7, x = 99, y = 23, w = 27, h = 8},
		{id = "mine-s", src = 7, x = 50, y = 23, w = 46, h = 8},

		{id = "section-line", src = 0, x = 0, y = 0, w = 1, h = 1},
		
		{id = "gauge-r1", src = 3, x = 0, y = 0, w = 5, h = 17},
		{id = "gauge-b1", src = 3, x = 5, y = 0, w = 5, h = 17},
		{id = "gauge-r2", src = 3, x = 10, y = 0, w = 5, h = 17},
		{id = "gauge-b2", src = 3, x = 15, y = 0, w = 5, h = 17},
		{id = "gauge-r3", src = 3, x = 0, y = 34, w = 5, h = 17},
		{id = "gauge-b3", src = 3, x = 5, y = 34, w = 5, h = 17},
		{id = "gauge-y1", src = 3, x = 0, y = 17, w = 5, h = 17},
		{id = "gauge-p1", src = 3, x = 5, y = 17, w = 5, h = 17},
		{id = "gauge-y2", src = 3, x = 10, y = 17, w = 5, h = 17},
		{id = "gauge-p2", src = 3, x = 15, y = 17, w = 5, h = 17},
		{id = "gauge-y3", src = 3, x = 10, y = 34, w = 5, h = 17},
		{id = "gauge-p3", src = 3, x = 15, y = 34, w = 5, h = 17},

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
	for i = 1, 8 do
		table.insert(skin.image, bomb_image(i, "bomb1-", 0, timer_key_bomb))
		table.insert(skin.image, bomb_image(i, "bomb2-", 576, timer_key_bomb))
		table.insert(skin.image, bomb_image(i, "bomb3-", 192, timer_key_bomb))
		table.insert(skin.image, bomb_image(i, "hold-", 384, timer_key_hold))
	end
	skin.imageset = {}
	do
		local wbs = { "w", "b", "s" }
		for i = 1, 8 do
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
	for i = 1, 8 do
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
		{id = "minbpm", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 91},
		{id = "nowbpm", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 160},
		{id = "maxbpm", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 90},
		{id = "timeleft-m", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 2, ref = 163},
		{id = "timeleft-s", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 2, padding = 1, ref = 164},
		{id = "hispeed", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 2, ref = 310},
		{id = "hispeed-d", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 2, padding = 1, ref = 311},
		{id = "duration", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 312},
		{id = "gaugevalue", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 3, ref = 107},
		{id = "gaugevalue-d", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 1, ref = 407},
		{id = "graphvalue-rate", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 4, ref = 102},
		{id = "graphvalue-rate-d", src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 1, ref = 103},
		{id = 422, src = 5, x = 0, y = 0, w = 240, h = 24, divx = 10, digit = 5, ref = 71},
		{id = 423, src = 5, x = 0, y = 24, w = 240, h = 24, divx = 10, digit = 5, ref = 150},
		{id = 424, src = 5, x = 0, y = 48, w = 240, h = 24, divx = 10, digit = 5, ref = 121},

		{id = "lanecover-value", src = 0, x = 0, y = 550, w = 100, h = 15, divx = 10, digit = 3, ref = 14},
		{id = "lanecover-duration", src = 0, x = 0, y = 565, w = 100, h = 15, divx = 10, digit = 4, ref = 312},
		
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
		{id = "song-title", font = 0, size = 24, align = geometry.title_align, ref = 12}
	}
	skin.slider = {
		{id = "musicprogress", src = 0, x = 0, y = 289, w = 14, h = 20, angle = 2, range = geometry.progress_h - 20,type = 6},
		{id = "musicprogress-fin", src = 0, x = 15, y = 289, w = 14, h = 20, angle = 2, range = geometry.progress_h - 20,type = 6},
		{id = "lanecover", src = 12, x = 0, y = 0, w = 390, h = 580, angle = 2, range = 580,type = 4}
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
		note = {"note-w", "note-b", "note-w", "note-b", "note-w", "note-b", "note-w", "note-s"},
		lnend = {"lne-w", "lne-b", "lne-w", "lne-b", "lne-w", "lne-b", "lne-w", "lne-s"},
		lnstart = {"lns-w", "lns-b", "lns-w", "lns-b", "lns-w", "lns-b", "lns-w", "lne-s"},
		lnbody = {"lnb-w", "lnb-b", "lnb-w", "lnb-b", "lnb-w", "lnb-b", "lnb-w", "lnb-s"},
		lnactive = {"lna-w", "lna-b", "lna-w", "lna-b", "lna-w", "lna-b", "lna-w", "lna-s"},
		hcnend = {"hcne-w", "hcne-b", "hcne-w", "hcne-b", "hcne-w", "hcne-b", "hcne-w", "hcne-s"},
		hcnstart = {"hcns-w", "hcns-b", "hcns-w", "hcns-b", "hcns-w", "hcns-b", "hcns-w", "hcns-s"},
		hcnbody = {"hcnb-w", "hcnb-b", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-b", "hcnb-w", "hcnb-s"},
		hcnactive = {"hcna-w", "hcna-b", "hcna-w", "hcna-b", "hcna-w", "hcna-b", "hcna-w", "hcna-s"},
		hcndamage = {"hcnd-w", "hcnd-b", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-b", "hcnd-w", "hcnd-s"},
		hcnreactive = {"hcnr-w", "hcnr-b", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-b", "hcnr-w", "hcnr-s"},
		mine = {"mine-w", "mine-b", "mine-w", "mine-b", "mine-w", "mine-b", "mine-w", "mine-s"},
		hidden = {},
		processed = {},
		group = {
			{id = "section-line", offset = 3, dst = {
				{x = geometry.lanes_x, y = 140, w = geometry.lanes_w, h = 1, r = 128, g = 128, b = 128}
			}}
		},
		time = {
			{id = "section-line", offset = 3, dst = {
				{x = geometry.lanes_x, y = 140, w = geometry.lanes_w, h = 1, r = 64, g = 192, b = 192}
			}}
		},
		bpm = {
			{id = "section-line", offset = 3, dst = {
				{x = geometry.lanes_x, y = 140, w = geometry.lanes_w, h = 2, r = 0, g = 192, b = 0}
			}}
		},
		stop = {
			{id = "section-line", offset = 3, dst = {
				{x = geometry.lanes_x, y = 140, w = geometry.lanes_w, h = 2, r = 192, g = 192, b = 0}
			}}
		}
	}
	skin.note.dst = {}
	for i = 1, 8 do
		skin.note.dst[i] = {
			x = geometry.notes_x[i],
			y = 140,
			w = geometry.notes_w[i],
			h = 580
		}
	end
	skin.gauge = {
		id = "gauge",
		nodes = {"gauge-r1","gauge-p1","gauge-r2","gauge-p2","gauge-r3","gauge-p3"
			,"gauge-r1","gauge-p1","gauge-r2","gauge-p2","gauge-r3","gauge-p3"
			,"gauge-r1","gauge-b1","gauge-r2","gauge-b2","gauge-r3","gauge-b3"
			,"gauge-r1","gauge-p1","gauge-r2","gauge-p2","gauge-r3","gauge-p3"
			,"gauge-y1","gauge-p1","gauge-y2","gauge-p2","gauge-y3","gauge-p3"
			,"gauge-p1","gauge-p1","gauge-p2","gauge-p2","gauge-p3","gauge-p3"}
	}
	skin.judge = {
		{
			id = "judge",
			index = 0,
			images = {
				{id = "judgef-pg", loop = -1, timer = 46 ,offsets = {3, 32}, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-gr", loop = -1, timer = 46 ,offsets = {3, 32}, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-gd", loop = -1, timer = 46 ,offsets = {3, 32}, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-bd", loop = -1, timer = 46 ,offsets = {3, 32}, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-pr", loop = -1, timer = 46 ,offsets = {3, 32}, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}},
				{id = "judgef-ms", loop = -1, timer = 46 ,offsets = {3, 32}, dst = {
					{time = 0, x = geometry.judge_x, y = 240, w = 180, h = 40},
					{time = 500}
				}}
			},
			numbers = {
				{id = "judgen-pg", loop = -1, timer = 46,offsets = {3, 32},  dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-gr", loop = -1, timer = 46,offsets = {3, 32},  dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-gd", loop = -1, timer = 46,offsets = {3, 32},  dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-bd", loop = -1, timer = 46,offsets = {3, 32},  dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-pr", loop = -1, timer = 46,offsets = {3, 32},  dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}},
				{id = "judgen-ms", loop = -1, timer = 46,offsets = {3, 32},  dst = {
					{time = 0, x = 200, y = 0, w = 40, h = 40},
					{time = 500}
				}}
			},
			shift = true
		}
	}
	skin.bga = {
		id = "bga"
	}
	skin.judgegraph = {
		{id = "judgegraph", type = 1, backTexOff = 1}
	}
	skin.bpmgraph = {
		{id = "bpmgraph"}
	}
	skin.timingvisualizer = {
		{id = "timing"}
	}
	skin.destination = {
		{id = "background", dst = {
			{x = 0, y = 0, w = 1280, h = 720}
		}},
		{id = 1, dst = {
			{x = 0, y = 0, w = 1280, h = 720}
		}},

		{id = "minbpm", dst = {
			{x = 520, y = 2, w = 18, h = 18}
		}},
		{id = "nowbpm", dst = {
			{x = 592, y = 2, w = 24, h = 24}
		}},
		{id = "maxbpm", dst = {
			{x = 688, y = 2, w = 18, h = 18}
		}},
		{id = "timeleft-m", dst = {
			{x = 1148, y = 2, w = 24, h = 24}
		}},
		{id = "timeleft-s", dst = {
			{x = 1220, y = 2, w = 24, h = 24}
		}},
		{id = "hispeed", dst = {
			{x = 116, y = 2, w = 12, h = 24}
		}},
		{id = "hispeed-d", dst = {
			{x = 154, y = 2, w = 10, h = 20}
		}},
		{id = "duration", dst = {
			{x = 318, y = 2, w = 12, h = 24}
		}},

		{id = 13, dst = {
			{x = geometry.progress_x + 2, y = geometry.progress_y, w = geometry.progress_w - 4, h = geometry.progress_h}
		}},

		{id = "lane-bg", loop = 1000, offset = 44, dst = {
			{time = 0, x = geometry.lanebg_x, y = 140, w = geometry.lanebg_w, h = 0, a = 0},
			{time = 1000, h = 580, a = 255}
		}},
		{id = "keys", dst = {
			{x = geometry.lanes_x, y = 100, w = geometry.lanes_w, h = 80}
		}}
	}
	for _, i in ipairs(keybeam_order) do
		name = i
		if i == 25 then
			name = "s"
		elseif i == 26 then
			name = "sd"
		end
		table.insert(skin.destination, {
			id = "keybeam"..name,
			timer = timer_key_on(i),
			loop = 100,
			offsets = {3, 40},
			dst = {
				{ time = 0, x = geometry.notes_x[i] + geometry.notes_w[i] / 4, y = 140, w = geometry.notes_w[i] / 2, h = 580 },
				{ time = 100, x = geometry.notes_x[i], w = geometry.notes_w[i] }
			}
		})
	end
	table.insert(skin.destination, {id = 15, offset = 50, dst = { {x = geometry.lanes_x, y = 137, w = geometry.lanes_w, h = 6} }})
	table.insert(skin.destination, {id = "notes", offset = 30})
	for i = 1, 8 do
		table.insert(skin.destination, {
			id = 109 + i,
			timer = timer_key_bomb(i),
			blend = 2,
			loop = -1,
			offsets = {3, 41},
			dst = {
				{ time = 0, x = geometry.lanes_center_x[i] - 80, y = 28, w = 180, h = 192 },
				{ time = 160 }
			}
		})
	end
	for i = 1, 8 do
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
			offset = 3,
			dst = {
				{ time = 0, x = geometry.lanes_center_x[i] - 80, y = 28, w = 180, h = 192 }
			}
		})
	end
	append_all(skin.destination, {
		{id = "judge"},
		{id = "judge-early", loop = -1, timer = 46 ,op = {911,1242},offsets = {3, 33}, dst = {
			{time = 0, x = geometry.judgedetail_x, y = geometry.judgedetail_y, w = 50, h = 20},
			{time = 500}
		}},
		{id = "judge-late", loop = -1, timer = 46 ,op = {911,1243},offsets = {3, 33}, dst = {
			{time = 0, x = geometry.judgedetail_x, y = geometry.judgedetail_y, w = 50, h = 20},
			{time = 500}
		}},
		{id = "judgems-1pp", loop = -1, timer = 46 ,op = {912,241},offsets = {3, 33}, dst = {
			{time = 0, x = geometry.judgedetail_x, y = geometry.judgedetail_y, w = 10, h = 20},
			{time = 500}
		}},
		{id = "judgems-1pg", loop = -1, timer = 46 ,op = {912,-241},offsets = {3, 33}, dst = {
			{time = 0, x = geometry.judgedetail_x, y = geometry.judgedetail_y, w = 10, h = 20},
			{time = 500}
		}},
		{id = "hidden-cover", dst = {
			{x = geometry.lanes_x, y = -440, w = geometry.lanes_w, h = 580}
		}},
		{id = "lanecover", dst = {
			{x = geometry.lanes_x, y = 720, w = geometry.lanes_w, h = 580}
		}},
		{id = "gauge", dst = {
			{time = 0, x = geometry.gauge_x, y = 30, w = geometry.gauge_w, h = 30}
		}},
		{id = "gaugevalue", dst = {
			{time = 0, x = geometry.gaugevalue_x, y = 60, w = 24, h = 24}
		}},
		{id = "gaugevalue-d", dst = {
			{time = 0, x = geometry.gaugevalue_x + 72, y = 60, w = 18, h = 18}
		}}
	})
	append_all(skin.destination, {
		{id = "bga", offset = 43, dst = {
			{time = 0, x = geometry.bga_x, y = geometry.bga_y, w = geometry.bga_w, h = geometry.bga_h}
		}},
		{id = "judgegraph", dst = {
			{time = 0, x = geometry.judgegraph_x, y = geometry.judgegraph_y, w = geometry.judgegraph_w, h = geometry.judgegraph_h}
		}},
		{id = "bpmgraph", dst = {
			{time = 0, x = geometry.judgegraph_x, y = geometry.judgegraph_y, w = geometry.judgegraph_w, h = geometry.judgegraph_h}
		}},
		{id = "timing", dst = {
			{time = 0, x = geometry.timing_x, y = geometry.timing_y, w = geometry.timing_w, h = geometry.timing_h}
		}},
		{id = "song-title", dst = {
			{time = 0, x = geometry.title_x, y = 674, w = 24, h = 24},
			{time = 1000, a = 0},
			{time = 2000, a = 255}
		}},
		{id = 11, op = {901},dst = {
			{x = geometry.graph_x, y = geometry.graph_y, w = geometry.graph_w, h = geometry.graph_h}
		}},
		{id = "graph-now", op = {901},dst = {
			{x = geometry.graph_x + 1, y = geometry.graph_y, w = geometry.graph_w / 3 - 2, h = geometry.graph_h}
		}},
		{id = "graph-best", op = {901},dst = {
			{x = geometry.graph_x + geometry.graph_w / 3 + 1, y = geometry.graph_y, w = geometry.graph_w / 3 - 2, h = geometry.graph_h}
		}},
		{id = "graph-target", op = {901},dst = {
			{x = geometry.graph_x + geometry.graph_w * 2 / 3 + 1, y = geometry.graph_y, w = geometry.graph_w / 3 - 2, h = geometry.graph_h}
		}},
		{id = 12, op = {901},dst = {
			{x = geometry.graph_x, y = geometry.graph_y, w = geometry.graph_w, h = geometry.graph_h}
		}},
		{id = "graphvalue-rate", op = {901},dst = {
			{x = geometry.graph_x + 10, y = 200, w = 12, h = 18}
		}},
		{id = "graphvalue-rate-d", op = {901},dst = {
			{x = geometry.graph_x + 58, y = 200, w = 8, h = 12}
		}},
		{id = 422, op = {901},dst = {
			{x = geometry.graph_x + 10, y = 180, w = 12, h = 18}
		}},
		{id = 423, op = {901},dst = {
			{x = geometry.graph_x + 10, y = 160, w = 12, h = 18}
		}},
		{id = 424, op = {901},dst = {
			{x = geometry.graph_x + 10, y = 140, w = 12, h = 18}
		}},
		{id = "musicprogress", blend = 2, dst = {
			{x = geometry.progress_x, y = geometry.progress_y + geometry.progress_h - 20, w = geometry.progress_w, h = 20}
		}},
		{id = "musicprogress-fin", blend = 2, timer = 143,dst = {
			{x = geometry.progress_x, y = geometry.progress_y + geometry.progress_h - 20, w = geometry.progress_w, h = 20}
		}},
	})
	append_all(skin.destination, play_parts.judge_count_destinations("judge-count-", geometry.judgecount_x, geometry.judgecount_y, {906}, 42))
	append_all(skin.destination, {
		{id = "lanecover-value", offset = 4, op = {270},dst = {
			{time = 0, x = geometry.lanes_x + geometry.lanes_w / 3 - 24, y = 720, w = 12, h = 18}
		}},
		{id = "lanecover-duration", offset = 4, op = {270},dst = {
			{time = 0, x = geometry.lanes_x + geometry.lanes_w * 2 / 3 - 24, y = 720, w = 12, h = 18}
		}},

		{id = "load-progress", loop = 0, op = {80}, dst = {
			{time = 0, x = geometry.lanes_x, y = 440, w = geometry.lanes_w, h = 4},
			{time = 500, a = 192, r = 0},
			{time = 1000, a = 128, r = 255, g = 0},
			{time = 1500, a = 192, g = 255, b = 0},
			{time = 2000, a = 255, b = 255}
		}},

		{id = "ready", loop = -1, timer = 40, dst = {
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
