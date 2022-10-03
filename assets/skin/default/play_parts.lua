
local judges = { "pf", "gr", "gd", "bd", "pr", "ms" }
local timings = { "", "-e", "-l" }
local timings_early_late = { "-e", "-l" }

local function value_jc(j, t)
	if j <= 5 then
		if t == 1 then
			return 109 + j
		else
			return 410 + (j - 1)*2 + (t - 2)
		end
	else
		return 420 + (t - 1)
	end
end

return {
	judge_count_sources = function(prefix, number_image_id)
		local sources = {}
		for ij, j in ipairs(judges) do
			for it, t in ipairs(timings) do
				table.insert(sources, {
					id = prefix..j..t,
					src = number_image_id,
					x = 0,
					y = (it - 1) * 24,
					w = 264,
					h = 24,
					divx = 11,
					digit = 4,
					ref = value_jc(ij, it),
				})
			end
		end
		return sources
	end,
	judge_count_destinations = function(prefix, pos_x, pos_y, ops, offset)
		local destinations = {}
		for y, j in ipairs(judges) do
			for x, t in ipairs(timings_early_late) do
				table.insert(destinations, {
					id = prefix..j..t,
					op = ops,
					dst = {
						{x = pos_x + (x - 1) * 60, y = pos_y + (y - 1) * 18, w = 12, h = 18}
					}
				})
			end
		end
		if offset >= 0 then
			for _, dst in ipairs(destinations) do
				dst.offset = offset
			end
		end
		return destinations
	end
}
