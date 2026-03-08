---
name: weather
description: Get current weather conditions and forecasts for any location. Use when user asks about weather, temperature, conditions, or forecast.
metadata: { "openclaw": { "emoji": "🌤️", "always": false } }
---

# Weather

Get current weather conditions and forecasts for any location using web search or weather APIs.

## 🎯 When to Use

Use this skill when user asks about:
- "What's the weather like?"
- "Will it rain today?"
- "What's the temperature in Beijing?"
- "Do I need an umbrella?"
- "Weather forecast for tomorrow"
- "Is it sunny outside?"

## 🌐 Weather Data Sources

### Method 1: Web Search (Recommended)

Use `web_search` tool to get weather information:

```kotlin
web_search(query: "weather Beijing current")
web_search(query: "weather forecast Shanghai 7 days")
web_search(query: "temperature New York now")
```

**Advantages**:
- No API key needed
- Works everywhere
- Gets latest data from multiple sources
- Often includes detailed forecasts

**Example Results**:
```
Weather in Beijing:
- Temperature: 15°C (59°F)
- Conditions: Partly Cloudy
- Humidity: 45%
- Wind: 12 km/h NE
- Updated: 10 minutes ago
```

### Method 2: Weather API (Optional)

If configured, use weather API directly:

```kotlin
web_fetch(url: "https://api.openweathermap.org/data/2.5/weather?q=Beijing&appid=YOUR_KEY&units=metric")
```

**Popular Weather APIs**:
1. **OpenWeatherMap** (Free tier: 1000 calls/day)
   - URL: `https://openweathermap.org/api`
2. **WeatherAPI** (Free tier: 1M calls/month)
   - URL: `https://www.weatherapi.com/`
3. **Tomorrow.io** (Formerly ClimaCell)
   - URL: `https://www.tomorrow.io/weather-api/`

## 📊 Weather Information to Provide

### Current Conditions

```
🌤️ Current Weather - Beijing

Temperature: 15°C (59°F)
Feels Like: 13°C (55°F)
Conditions: Partly Cloudy
Humidity: 45%
Wind: 12 km/h NE
Pressure: 1013 hPa
Visibility: 10 km
UV Index: 3 (Moderate)

Last Updated: 10 minutes ago
```

### Short Forecast (Today)

```
📅 Today - March 8, 2026

Morning:   🌤️ 12°C  Partly Cloudy
Afternoon: ☀️ 18°C  Sunny
Evening:   🌙 10°C  Clear
Night:     🌙 8°C   Clear

Precipitation: 0%
Sunrise: 06:35
Sunset: 18:20
```

### Extended Forecast (7 Days)

```
📅 7-Day Forecast - Beijing

Sat Mar 9:  ☀️ 18°C / 8°C   Sunny
Sun Mar 10: 🌤️ 16°C / 7°C   Partly Cloudy
Mon Mar 11: ☁️ 14°C / 6°C   Cloudy
Tue Mar 12: 🌧️ 12°C / 5°C   Rain (60%)
Wed Mar 13: 🌧️ 11°C / 4°C   Rain (80%)
Thu Mar 14: 🌤️ 13°C / 5°C   Partly Cloudy
Fri Mar 15: ☀️ 16°C / 7°C   Sunny
```

## 🔍 Search Strategies

### Strategy 1: Direct City Search

```kotlin
// For well-known cities
web_search(query: "weather Beijing")
web_search(query: "weather New York")
web_search(query: "weather London")
```

### Strategy 2: Specific Information

```kotlin
// Temperature only
web_search(query: "temperature Shanghai now")

// Precipitation
web_search(query: "will it rain Tokyo today")

// Forecast
web_search(query: "weather forecast Paris 5 days")

// Specific time
web_search(query: "weather Hong Kong tomorrow")
```

### Strategy 3: Location Context

```kotlin
// If user provides GPS coordinates
web_search(query: "weather 39.9042° N, 116.4074° E")

// By ZIP code (US)
web_search(query: "weather 10001 New York")

// By neighborhood
web_search(query: "weather Chaoyang District Beijing")
```

## 🎨 Weather Icons

Use these emojis for visual representation:

| Condition | Emoji | Description |
|-----------|-------|-------------|
| Clear/Sunny | ☀️ | Sunny day |
| Partly Cloudy | 🌤️ | Some clouds |
| Cloudy | ☁️ | Overcast |
| Rainy | 🌧️ | Rain |
| Stormy | ⛈️ | Thunderstorm |
| Snowy | ❄️ | Snow |
| Foggy | 🌫️ | Fog |
| Windy | 💨 | Strong wind |
| Night Clear | 🌙 | Clear night |
| Night Clouds | ☁️🌙 | Cloudy night |

## 💡 Helpful Recommendations

### Based on Temperature

```kotlin
if (temp > 30) {
    "🔥 Very hot! Stay hydrated and avoid direct sun."
} else if (temp > 25) {
    "☀️ Warm day. Light clothing recommended."
} else if (temp > 15) {
    "🌤️ Pleasant weather. Perfect for outdoor activities."
} else if (temp > 5) {
    "🧥 Cool weather. Bring a jacket."
} else if (temp > 0) {
    "🥶 Cold! Dress warmly."
} else {
    "❄️ Freezing! Bundle up!"
}
```

### Based on Conditions

```kotlin
if (conditions.contains("rain")) {
    "☂️ Don't forget your umbrella!"
} else if (conditions.contains("snow")) {
    "❄️ Snow expected. Drive carefully."
} else if (conditions.contains("storm")) {
    "⚠️ Storm warning. Stay indoors if possible."
} else if (conditions.contains("fog")) {
    "🌫️ Low visibility. Drive carefully."
} else if (windSpeed > 40) {
    "💨 Strong winds. Secure loose objects."
}
```

### Activity Suggestions

```kotlin
if (isSunny && temp > 20 && temp < 30) {
    "Perfect day for: 🏃 Running, 🚴 Cycling, 🎾 Tennis"
} else if (isRainy) {
    "Good day for: 🏋️ Gym, 📚 Reading, 🎬 Movies"
} else if (isCold) {
    "Good day for: ☕ Coffee, 🎿 Skiing, 🏠 Indoor activities"
}
```

## 📱 Response Format Examples

### Simple Query

**User**: "What's the weather?"

**Response**:
```
🌤️ Current Weather - Beijing

Temperature: 15°C (59°F)
Conditions: Partly Cloudy
Humidity: 45%
Wind: 12 km/h

Nice weather for a walk! 🚶
```

### Detailed Query

**User**: "Give me the full weather forecast for this week"

**Response**:
```
📅 7-Day Forecast - Beijing

Today (Sat):   ☀️ 18°C / 8°C   Sunny
Tomorrow (Sun): 🌤️ 16°C / 7°C   Partly Cloudy
Mon:           ☁️ 14°C / 6°C   Cloudy
Tue:           🌧️ 12°C / 5°C   Rain (60%)
Wed:           🌧️ 11°C / 4°C   Rain (80%)
Thu:           🌤️ 13°C / 5°C   Partly Cloudy
Fri:           ☀️ 16°C / 7°C   Sunny

⚠️ Heads up: Heavy rain expected Tue-Wed.
Perfect weekend for outdoor activities! 🌟
```

### Specific Location

**User**: "What's the weather in Tokyo?"

**Response**:
```
🌤️ Weather in Tokyo, Japan

Current: 22°C (72°F)
Conditions: Partly Cloudy
Humidity: 65%
Wind: 8 km/h SE

Today's Forecast:
Morning:   20°C ☀️ Sunny
Afternoon: 24°C 🌤️ Partly Cloudy
Evening:   18°C 🌙 Clear

Great weather for sightseeing! 🗼
```

## 🔧 Error Handling

### Location Not Found

```
❌ Couldn't find weather for "Atlantis"

Did you mean:
- Atlanta, Georgia
- Atlantic City, New Jersey
- Atlantic Ocean

Please specify the location more clearly.
```

### API Unavailable

```
⚠️ Weather service temporarily unavailable

Try again in a few minutes, or check:
- weather.com
- weather.gov
- accuweather.com
```

### No Internet

```
❌ No internet connection

Cannot fetch weather data. Please check your connection.
```

## 🎓 Best Practices

1. **Always include location**: Unless user's location is known
2. **Use local units**: Celsius for most of world, Fahrenheit for US
3. **Include timestamp**: "Last updated 10 minutes ago"
4. **Add context**: Recommendations based on conditions
5. **Be concise**: Don't overwhelm with too much data
6. **Use emojis**: Makes weather info more visual and friendly
7. **Warn about extremes**: Alert for dangerous weather

## ⚙️ Configuration (Optional)

If using weather API, configure in:
`/sdcard/.androidforclaw/config/openclaw.json`

```json
{
  "weather": {
    "provider": "openweathermap",
    "apiKey": "${OPENWEATHER_API_KEY}",
    "units": "metric",
    "language": "en",
    "defaultLocation": {
      "city": "Beijing",
      "country": "CN",
      "coordinates": {
        "lat": 39.9042,
        "lon": 116.4074
      }
    }
  }
}
```

## 🌍 Location Detection

### From GPS (if available)

```kotlin
// Get device location
val location = getDeviceLocation()
web_search(query: "weather ${location.latitude} ${location.longitude}")
```

### From User Input

```kotlin
// Parse city name from user query
val city = extractLocation(userQuery)
web_search(query: "weather $city")
```

### Default Location

If no location specified, use configured default or ask:
```
Which location's weather would you like to check?
```

## 🔮 Future Features

Planned improvements:
- Air quality index (AQI)
- Pollen levels
- Weather alerts and warnings
- Historical weather data
- Weather-based reminders
- Integration with calendar (weather for event dates)

---

**Remember**: Weather information helps users plan their day. Always provide clear, actionable information with friendly context.
