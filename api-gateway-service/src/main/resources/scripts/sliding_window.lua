-- Sliding Window Rate Limiter
-- Keys: KEYS[1] = Redis key for this client
-- Args: ARGV[1] = window size in ms, ARGV[2] = max requests, ARGV[3] = current timestamp

local key        = KEYS[1]
local window_ms  = tonumber(ARGV[1])
local max_req    = tonumber(ARGV[2])
local now        = tonumber(ARGV[3])
local window_start = now - window_ms

-- Remove entries older than the window
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

-- Count current requests in window
local count = redis.call('ZCARD', key)

if count < max_req then
    -- Allow: add this request
    redis.call('ZADD', key, now, now .. '-' .. math.random(100000))
    redis.call('PEXPIRE', key, window_ms)
    return {1, max_req - count - 1, now + window_ms}
else
    -- Reject: limit exceeded
    return {0, 0, now + window_ms}
end
