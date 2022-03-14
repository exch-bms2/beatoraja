
local function append_all(list, list1)
	for i, v in ipairs(list1) do
		table.insert(list, v)
	end
end

local property = {
}


local filepath = {
}

local header = {
	type = 9,
	name = "beatoraja default (lua)",
	w = 1280,
	h = 720,
	scene = 3000,
	input = 500,
	fadeout = 500,
	property = property,
	filepath = filepath
}

local function main()

	local skin = {}
	for k, v in pairs(header) do
		skin[k] = v
	end


	skin.source = {
		{id = 0, path = "../skinselect.png"},
	}
	skin.font = {
		{id = 0, path = "../VL-Gothic-Regular.ttf"}
	}

	skin.image = {
		{id = "preview-bg", src = 0, x = 0, y = 664, w = 640, h = 360},
		{id = "arrow-l", src = 0, x = 989, y = 0, w = 12, h = 12},
		{id = "arrow-r", src = 0, x = 1001, y = 0, w = 12, h = 12},
		{id = "arrow-l-active", src = 0, x = 989, y = 12, w = 12, h = 12},
		{id = "arrow-r-active", src = 0, x = 1001, y = 12, w = 12, h = 12},
		{id = "scroll-bg", src = 0, x = 1014, y = 0, w = 10, h = 251},

		{id = "button-skin", src = 0, x = 640, y = 0, w = 0, h = 0, act = 190, click = 2},
		{id = "button-custom-1", src = 0, x = 640, y = 10, w = 120, h = 48, act = 220, click = 2},
		{id = "button-custom-2", src = 0, x = 640, y = 10, w = 120, h = 48, act = 221, click = 2},
		{id = "button-custom-3", src = 0, x = 640, y = 10, w = 120, h = 48, act = 222, click = 2},
		{id = "button-custom-4", src = 0, x = 640, y = 10, w = 120, h = 48, act = 223, click = 2},
		{id = "button-custom-5", src = 0, x = 640, y = 10, w = 120, h = 48, act = 224, click = 2},
		{id = "button-custom-6", src = 0, x = 640, y = 10, w = 120, h = 48, act = 225, click = 2},

		{id = "type-off-5" , src = 0, x = 0, y = 0, w = 300, h = 30},
		{id = "type-off-6" , src = 0, x = 0, y = 30, w = 300, h = 30},
		{id = "type-off-7" , src = 0, x = 0, y = 60, w = 300, h = 30},
		{id = "type-off-15" , src = 0, x = 0, y = 90, w = 300, h = 30},
		{id = "type-off-10" , src = 0, x = 0, y = 120, w = 300, h = 30},
		{id = "type-off-8" , src = 0, x = 0, y = 150, w = 300, h = 30},
		{id = "type-off-9" , src = 0, x = 0, y = 180, w = 300, h = 30},
		{id = "type-off-11" , src = 0, x = 0, y = 210, w = 300, h = 30},
		{id = "type-off-0" , src = 0, x = 0, y = 240, w = 300, h = 30},
		{id = "type-off-12" , src = 0, x = 0, y = 270, w = 300, h = 30},
		{id = "type-off-2" , src = 0, x = 0, y = 300, w = 300, h = 30},
		{id = "type-off-1" , src = 0, x = 0, y = 330, w = 300, h = 30},
		{id = "type-off-13" , src = 0, x = 0, y = 360, w = 300, h = 30},
		{id = "type-off-3" , src = 0, x = 0, y = 390, w = 300, h = 30},
		{id = "type-off-4" , src = 0, x = 0, y = 420, w = 300, h = 30},
		{id = "type-off-14" , src = 0, x = 0, y = 450, w = 300, h = 30},
		{id = "type-off-16" , src = 0, x = 0, y = 480, w = 300, h = 30},
		{id = "type-off-18" , src = 0, x = 0, y = 510, w = 300, h = 30},
		{id = "type-off-17" , src = 0, x = 0, y = 540, w = 300, h = 30},
		{id = "type-on-5" , src = 0, x = 300, y = 0, w = 300, h = 30},
		{id = "type-on-6" , src = 0, x = 300, y = 30, w = 300, h = 30},
		{id = "type-on-7" , src = 0, x = 300, y = 60, w = 300, h = 30},
		{id = "type-on-15" , src = 0, x = 300, y = 90, w = 300, h = 30},
		{id = "type-on-10" , src = 0, x = 300, y = 120, w = 300, h = 30},
		{id = "type-on-8" , src = 0, x = 300, y = 150, w = 300, h = 30},
		{id = "type-on-9" , src = 0, x = 300, y = 180, w = 300, h = 30},
		{id = "type-on-11" , src = 0, x = 300, y = 210, w = 300, h = 30},
		{id = "type-on-0" , src = 0, x = 300, y = 240, w = 300, h = 30},
		{id = "type-on-12" , src = 0, x = 300, y = 270, w = 300, h = 30},
		{id = "type-on-2" , src = 0, x = 300, y = 300, w = 300, h = 30},
		{id = "type-on-1" , src = 0, x = 300, y = 330, w = 300, h = 30},
		{id = "type-on-13" , src = 0, x = 300, y = 360, w = 300, h = 30},
		{id = "type-on-3" , src = 0, x = 300, y = 390, w = 300, h = 30},
		{id = "type-on-4" , src = 0, x = 300, y = 420, w = 300, h = 30},
		{id = "type-on-14" , src = 0, x = 300, y = 450, w = 300, h = 30},
		{id = "type-on-16" , src = 0, x = 300, y = 480, w = 300, h = 30},
		{id = "type-on-18" , src = 0, x = 300, y = 510, w = 300, h = 30},
		{id = "type-on-17" , src = 0, x = 300, y = 540, w = 300, h = 30},
	}

	skin.imageset = {
		{id = "type-0" , images = {"type-off-0", "type-on-0"}, act = 170, ref = 170},
		{id = "type-1" , images = {"type-off-1", "type-on-1"}, act = 171, ref = 171},
		{id = "type-2" , images = {"type-off-2", "type-on-2"}, act = 172, ref = 172},
		{id = "type-3" , images = {"type-off-3", "type-on-3"}, act = 173, ref = 173},
		{id = "type-4" , images = {"type-off-4", "type-on-4"}, act = 174, ref = 174},
		{id = "type-5" , images = {"type-off-5", "type-on-5"}, act = 175, ref = 175},
		{id = "type-6" , images = {"type-off-6", "type-on-6"}, act = 176, ref = 176},
		{id = "type-7" , images = {"type-off-7", "type-on-7"}, act = 177, ref = 177},
		{id = "type-8" , images = {"type-off-8", "type-on-8"}, act = 178, ref = 178},
		{id = "type-9" , images = {"type-off-9", "type-on-9"}, act = 179, ref = 179},
		{id = "type-10" , images = {"type-off-10", "type-on-10"}, act = 180, ref = 180},
		{id = "type-11" , images = {"type-off-11", "type-on-11"}, act = 181, ref = 181},
		{id = "type-12" , images = {"type-off-12", "type-on-12"}, act = 182, ref = 182},
		{id = "type-13" , images = {"type-off-13", "type-on-13"}, act = 183, ref = 183},
		{id = "type-14" , images = {"type-off-14", "type-on-14"}, act = 184, ref = 184},
		{id = "type-15" , images = {"type-off-15", "type-on-15"}, act = 185, ref = 185},
		{id = "type-16" , images = {"type-off-16", "type-on-16"}, act = 386, ref = 386},
		{id = "type-17" , images = {"type-off-17", "type-on-17"}, act = 387, ref = 387},
		{id = "type-18" , images = {"type-off-18", "type-on-18"}, act = 388, ref = 388},
	}
	skin.value = {}
	skin.text = {
		{id = "skin-name", font = 0, size = 24, align = 1, ref = 50},
		{id = "custom-label-1", font = 0, size = 24, align = 2, ref = 100},
		{id = "custom-label-2", font = 0, size = 24, align = 2, ref = 101},
		{id = "custom-label-3", font = 0, size = 24, align = 2, ref = 102},
		{id = "custom-label-4", font = 0, size = 24, align = 2, ref = 103},
		{id = "custom-label-5", font = 0, size = 24, align = 2, ref = 104},
		{id = "custom-label-6", font = 0, size = 24, align = 2, ref = 105},
		{id = "custom-value-1", font = 0, size = 24, align = 1, ref = 110},
		{id = "custom-value-2", font = 0, size = 24, align = 1, ref = 111},
		{id = "custom-value-3", font = 0, size = 24, align = 1, ref = 112},
		{id = "custom-value-4", font = 0, size = 24, align = 1, ref = 113},
		{id = "custom-value-5", font = 0, size = 24, align = 1, ref = 114},
		{id = "custom-value-6", font = 0, size = 24, align = 1, ref = 115},
	}
	skin.slider = {
		{id = "scroll-fg", src = 0, x = 1007, y = 252, w = 17, h = 24, angle = 2, range = 232, type = 7},
	}
	skin.destination = {

		{id = "button-skin", dst = {
			{x = 450, y = 350, w = 680, h = 360},
		}},
		{id = "preview-bg", dst = {
			{x = 470, y = 350, w = 640, h = 360},
		}},
		{id = "skin-name", dst = {
			{x = 790, y = 310, w = 640, h = 24},
		}},
		{id = "arrow-l", dst = {
			{x = 448, y = 514, w = 12, h = 12},
		}},
		{id = "arrow-r", dst = {
			{x = 1120, y = 514, w = 12, h = 12},
		}},

		{id = "arrow-l-active", dst = {
			{x = 448, y = 514, w = 12, h = 12}
		}, mouseRect = {x = 2, y = -164, w = 340, h = 360}},
		{id = "arrow-r-active", dst = {
			{x = 1120, y = 514, w = 12, h = 12}
		}, mouseRect = {x = -330, y = -164, w = 340, h = 360}},

		{id = "button-custom-1", dst = {
			{x = 780, y = 252, w = 440, h = 48},
		}},
		{id = "button-custom-2", dst = {
			{x = 780, y = 204, w = 440, h = 48},
		}},
		{id = "button-custom-3", dst = {
			{x = 780, y = 156, w = 440, h = 48},
		}},
		{id = "button-custom-4", dst = {
			{x = 780, y = 108, w = 440, h = 48},
		}},
		{id = "button-custom-5", dst = {
			{x = 780, y = 60, w = 440, h = 48},
		}},
		{id = "button-custom-6", dst = {
			{x = 780, y = 12, w = 440, h = 48},
		}},

		{id = "custom-label-1", dst = {
			{x = 720, y = 264, w = 400, h = 24},
		}},
		{id = "custom-label-2", dst = {
			{x = 720, y = 216, w = 400, h = 24},
		}},
		{id = "custom-label-3", dst = {
			{x = 720, y = 168, w = 400, h = 24},
		}},
		{id = "custom-label-4", dst = {
			{x = 720, y = 120, w = 400, h = 24},
		}},
		{id = "custom-label-5", dst = {
			{x = 720, y = 72, w = 400, h = 24},
		}},
		{id = "custom-label-6", dst = {
			{x = 720, y = 24, w = 400, h = 24},
		}},

		{id = "custom-value-1", dst = {
			{x = 1000, y = 264, w = 400, h = 24},
		}},
		{id = "custom-value-2", dst = {
			{x = 1000, y = 216, w = 400, h = 24},
		}},
		{id = "custom-value-3", dst = {
			{x = 1000, y = 168, w = 400, h = 24},
		}},
		{id = "custom-value-4", dst = {
			{x = 1000, y = 120, w = 400, h = 24},
		}},
		{id = "custom-value-5", dst = {
			{x = 1000, y = 72, w = 400, h = 24},
		}},
		{id = "custom-value-6", dst = {
			{x = 1000, y = 24, w = 400, h = 24},
		}},

		{id = "arrow-l", dst = {
			{x = 788, y = 270, w = 12, h = 12},
		}},
		{id = "arrow-l", dst = {
			{x = 788, y = 222, w = 12, h = 12},
		}},
		{id = "arrow-l", dst = {
			{x = 788, y = 174, w = 12, h = 12},
		}},
		{id = "arrow-l", dst = {
			{x = 788, y = 126, w = 12, h = 12},
		}},
		{id = "arrow-l", dst = {
			{x = 788, y = 78, w = 12, h = 12},
		}},
		{id = "arrow-l", dst = {
			{x = 788, y = 30, w = 12, h = 12},
		}},
		{id = "arrow-r", dst = {
			{x = 1200, y = 270, w = 12, h = 12},
		}},
		{id = "arrow-r", dst = {
			{x = 1200, y = 222, w = 12, h = 12},
		}},
		{id = "arrow-r", dst = {
			{x = 1200, y = 174, w = 12, h = 12},
		}},
		{id = "arrow-r", dst = {
			{x = 1200, y = 126, w = 12, h = 12},
		}},
		{id = "arrow-r", dst = {
			{x = 1200, y = 78, w = 12, h = 12},
		}},
		{id = "arrow-r", dst = {
			{x = 1200, y = 30, w = 12, h = 12},
		}},

		{id = "arrow-l-active", dst = {
			{x = 788, y = 270, w = 12, h = 12},
		}, mouseRect = {x = -8, y = -18, w = 220, h = 48}},
		{id = "arrow-l-active", dst = {
			{x = 788, y = 222, w = 12, h = 12},
		}, mouseRect = {x = -8, y = -18, w = 220, h = 48}},
		{id = "arrow-l-active", dst = {
			{x = 788, y = 174, w = 12, h = 12},
		}, mouseRect = {x = -8, y = -18, w = 220, h = 48}},
		{id = "arrow-l-active", dst = {
			{x = 788, y = 126, w = 12, h = 12},
		}, mouseRect = {x = -8, y = -18, w = 220, h = 48}},
		{id = "arrow-l-active", dst = {
			{x = 788, y = 78, w = 12, h = 12},
		}, mouseRect = {x = -8, y = -18, w = 220, h = 48}},
		{id = "arrow-l-active", dst = {
			{x = 788, y = 30, w = 12, h = 12},
		}, mouseRect = {x = -8, y = -18, w = 220, h = 48}},
		{id = "arrow-r-active", dst = {
			{x = 1200, y = 270, w = 12, h = 12},
		}, mouseRect = {x = -200, y = -18, w = 220, h = 48}},
		{id = "arrow-r-active", dst = {
			{x = 1200, y = 222, w = 12, h = 12},
		}, mouseRect = {x = -200, y = -18, w = 220, h = 48}},
		{id = "arrow-r-active", dst = {
			{x = 1200, y = 174, w = 12, h = 12},
		}, mouseRect = {x = -200, y = -18, w = 220, h = 48}},
		{id = "arrow-r-active", dst = {
			{x = 1200, y = 126, w = 12, h = 12},
		}, mouseRect = {x = -200, y = -18, w = 220, h = 48}},
		{id = "arrow-r-active", dst = {
			{x = 1200, y = 78, w = 12, h = 12},
		}, mouseRect = {x = -200, y = -18, w = 220, h = 48}},
		{id = "arrow-r-active", dst = {
			{x = 1200, y = 30, w = 12, h = 12},
		}, mouseRect = {x = -200, y = -18, w = 220, h = 48}},

		{id = "scroll-bg", dst = {
			{x = 1260, y = 24, w = 10, h = 264},
		}},
		{id = "scroll-fg", blend = 2, dst = {
			{x = 1256, y = 260, w = 17, h = 24},
		}},

		{id = "type-5", dst = {
			{x = 0, y = 630, w = 300, h = 30},
		}},
		{id = "type-6", dst = {
			{x = 0, y = 600, w = 300, h = 30},
		}},
		{id = "type-7", dst = {
			{x = 0, y = 570, w = 300, h = 30},
		}},
		{id = "type-15", dst = {
			{x = 0, y = 540, w = 300, h = 30},
		}},
		{id = "type-10", dst = {
			{x = 0, y = 510, w = 300, h = 30},
		}},
		{id = "type-8", dst = {
			{x = 0, y = 480, w = 300, h = 30},
		}},
		{id = "type-9", dst = {
			{x = 0, y = 450, w = 300, h = 30},
		}},
		{id = "type-11", dst = {
			{x = 0, y = 420, w = 300, h = 30},
		}},
		{id = "type-0", dst = {
			{x = 0, y = 360, w = 300, h = 30},
		}},
		{id = "type-12", dst = {
			{x = 0, y = 330, w = 300, h = 30},
		}},
		{id = "type-2", dst = {
			{x = 0, y = 300, w = 300, h = 30},
		}},
		{id = "type-1", dst = {
			{x = 0, y = 270, w = 300, h = 30},
		}},
		{id = "type-13", dst = {
			{x = 0, y = 240, w = 300, h = 30},
		}},
		{id = "type-3", dst = {
			{x = 0, y = 210, w = 300, h = 30},
		}},
		{id = "type-4", dst = {
			{x = 0, y = 180, w = 300, h = 30},
		}},
		{id = "type-14", dst = {
			{x = 0, y = 150, w = 300, h = 30},
		}},
		{id = "type-16", dst = {
			{x = 0, y = 120, w = 300, h = 30},
		}},
		{id = "type-18", dst = {
			{x = 0, y = 90, w = 300, h = 30},
		}},
		{id = "type-17", dst = {
			{x = 0, y = 60, w = 300, h = 30},
		}},
	}

	skin.skinSelect = {
		defaultType = 6,
		customOffsetStyle = 0,
		customPropertyCount = 6,
		sampleBMS = {}
	}

	return skin
end

return {
	header = header,
	main = main
}
