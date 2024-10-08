package com.example.weatherapp.Model

sealed class WeatherForecastState {

    object Loading : WeatherForecastState()
    data class Success(val forecastResponse: ForecastResponse?) : WeatherForecastState()
    data class Error(val errorMessage: String) : WeatherForecastState()
}


sealed class CurrentWeatherState {

    object Loading : CurrentWeatherState()
    data class Success(val currentWeatherResponse: WeatherResponse?) : CurrentWeatherState()
    data class Error(val errorMessage: String) : CurrentWeatherState()

}

sealed class FavouritesState {
    object Loading : FavouritesState()
    data class Success(val data: List<CurrentWeather>) : FavouritesState()
    data class Error(val message: String) : FavouritesState()
}
