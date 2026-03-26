---
name: weather
description: "Get current weather and forecasts for any location. Use when: user asks about weather, temperature, or forecasts. NOT for: historical weather data or severe weather alerts. No API key needed."
metadata: { "openclaw": { "emoji": "🌤️", "always": false } }
---

# Weather

Get current weather conditions and forecasts using web search.

## Method

Use `web_search` to query weather:

```
web_search("weather Beijing current")
web_search("weather forecast Shanghai 7 days")
web_search("will it rain Tokyo today")
```

## Response Format

Include:
- **Temperature** (°C preferred, include °F for US)
- **Conditions** (sunny, cloudy, rain, etc.)
- **Humidity & Wind** (when relevant)
- **Forecast** (if asked)
- **Recommendation** (umbrella? jacket?)

Use weather emojis: ☀️ 🌤️ ☁️ 🌧️ ⛈️ ❄️ 🌫️ 💨

## Tips

- If location unknown, ask the user
- Use local units (Celsius for most countries, Fahrenheit for US)
- Add practical advice based on conditions
- Keep it concise — weather info should be quick to read
